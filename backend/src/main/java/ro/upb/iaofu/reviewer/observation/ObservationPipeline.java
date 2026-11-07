package ro.upb.iaofu.reviewer.observation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.upb.iaofu.reviewer.llm.LlmClient;
import ro.upb.iaofu.reviewer.llm.LlmGateway;
import ro.upb.iaofu.reviewer.llm.Prompts;
import ro.upb.iaofu.reviewer.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The proposed system: the LLM proposes, execution decides (PRD 8.1, components 4-6).
 * Every proposal is stored, confirmed or rejected, so the analysis can measure the verifier.
 */
@Service
public class ObservationPipeline {

    private static final Logger log = LoggerFactory.getLogger(ObservationPipeline.class);

    public static final String DUPLICATE = "duplicate";
    public static final String LEAK = "leak";
    static final int MAX_LEAK_RETRIES = 2;
    static final long GENERATOR_MAX_TOKENS = 6000;
    static final long REWRITE_MAX_TOKENS = 2000;

    public static final String PURPOSE_OBSERVATIONS = "observations";
    public static final String PURPOSE_RETRY = "observations_retry";
    public static final String PURPOSE_LEAK = "leak_regeneration";

    private static final Map<String, Object> REWRITE_SCHEMA = Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("risk", "hint1", "hint2"),
            "properties", Map.of("risk", Map.of("type", "string"), "hint1", Map.of("type", "string"),
                    "hint2", Map.of("type", "string")));

    private final LlmGateway llm;
    private final CounterexampleVerifier verifier;
    private final ObservationRepository observations;
    private final ObservationService observationService;
    private final JsonMapper json;

    public ObservationPipeline(LlmGateway llm, CounterexampleVerifier verifier, ObservationRepository observations,
                               ObservationService observationService, JsonMapper json) {
        this.llm = llm;
        this.verifier = verifier;
        this.observations = observations;
        this.observationService = observationService;
        this.json = json;
    }

    public record Result(List<ObservationView> shown, String llmStatus) {
    }

    /** Thrown when the generator returned invalid JSON twice (N3). */
    static class GenerationFailedException extends RuntimeException {

        GenerationFailedException(String message) {
            super(message);
        }
    }

    public Result run(UUID submissionId, ObservationPrompt.Context context) {
        List<Proposal> proposals = generate(submissionId, context);
        Problem problem = context.problem();

        List<Observation> confirmed = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Proposal proposal : proposals) {
            Observation o = new Observation(submissionId, proposal.type(), validLine(proposal.line(), context.code()),
                    proposal.evidenceInput() == null ? null : proposal.evidenceInput().toString(), proposal.risk(),
                    json.writeValueAsString(proposal.hints()));
            CounterexampleVerifier.Verdict verdict = verifier.verify(problem, context.code(), context.report(), proposal);
            o.setOutputs(text(verdict.studentOutput()), text(verdict.expectedOutput()));
            if (!verdict.confirmed()) {
                o.reject(verdict.rejectReason());
            } else if (!seen.add(proposal.type() + ":" + o.getLine())) {
                o.reject(DUPLICATE);
            } else {
                removeLeaks(submissionId, problem, context.code(), o, proposal.hints());
                if (o.isConfirmed()) {
                    confirmed.add(o);
                }
            }
            observations.save(o);
            log.info("Observation {} line {}: {}{}", o.getType(), o.getLine(), o.getStatus(),
                    o.getRejectReason() == null ? "" : " (" + o.getRejectReason() + ")");
        }
        List<Observation> shown = confirmed.stream().limit(ProposalParser.MAX_OBSERVATIONS).toList();
        observationService.show(shown);
        return new Result(observationService.views(submissionId), "ok");
    }

    /** One call plus one retry when the JSON is invalid (N3). */
    List<Proposal> generate(UUID submissionId, ObservationPrompt.Context context) {
        LlmClient.Request request = new LlmClient.Request(Prompts.OBSERVATIONS, ObservationPrompt.user(context),
                ObservationPrompt.SCHEMA, GENERATOR_MAX_TOKENS);
        String purpose = PURPOSE_OBSERVATIONS;
        for (int attempt = 1; attempt <= 2; attempt++) {
            LlmClient.Reply reply = llm.call(submissionId, purpose, request);
            try {
                return ProposalParser.parse(reply.text(), json);
            } catch (ProposalParser.InvalidResponseException e) {
                log.warn("Invalid generator response (attempt {}): {}", attempt, e.getMessage());
                purpose = PURPOSE_RETRY;
            }
        }
        throw new GenerationFailedException("Generator returned invalid JSON twice");
    }

    /**
     * Regenerates the risk and levels 1-2 while they contain forbidden terms (the risk is shown
     * together with level 1); rejects the observation after two tries.
     */
    void removeLeaks(UUID submissionId, Problem problem, String code, Observation o, List<String> hints) {
        List<String> current = new ArrayList<>(hints);
        String risk = o.getRisk();
        List<String> leaked = leaked(problem, code, risk, current);
        int retries = 0;
        while (!leaked.isEmpty() && retries < MAX_LEAK_RETRIES) {
            retries++;
            try {
                JsonNode rewritten = rewrite(submissionId, problem, risk, current, leaked);
                risk = rewritten.get("risk").asString().strip();
                current.set(0, rewritten.get("hint1").asString().strip());
                current.set(1, rewritten.get("hint2").asString().strip());
            } catch (RuntimeException e) {
                log.warn("Hint rewrite failed: {}", e.getMessage());
            }
            leaked = leaked(problem, code, risk, current);
        }
        o.setLeakRetries(retries);
        o.setRisk(risk);
        o.setHintsJson(json.writeValueAsString(current));
        if (!leaked.isEmpty()) {
            o.reject(LEAK);
        }
    }

    private static List<String> leaked(Problem problem, String code, String risk, List<String> hints) {
        return HintLeakFilter.leakedTermsIn(List.of(risk, hints.get(0), hints.get(1)), problem.forbiddenTerms(), code);
    }

    private JsonNode rewrite(UUID submissionId, Problem problem, String risk, List<String> hints, List<String> leaked) {
        List<String> forbidden = problem.forbiddenTerms().stream().map(Problem.ForbiddenTerm::term).toList();
        String user = "Problema: " + problem.title()
                + "\n\nRiscul: " + risk + "\nIndiciul 1: " + hints.get(0) + "\nIndiciul 2: " + hints.get(1)
                + "\n\nTermeni interziși: " + String.join(", ", forbidden)
                + "\nTermeni folosiți greșit: " + String.join(", ", leaked);
        LlmClient.Reply reply = llm.call(submissionId, PURPOSE_LEAK,
                new LlmClient.Request(Prompts.HINT_REWRITE, user, REWRITE_SCHEMA, REWRITE_MAX_TOKENS));
        return json.readTree(reply.text());
    }

    private String text(JsonNode node) {
        return node == null ? null : node.toString();
    }

    private static Integer validLine(Integer line, String code) {
        long lines = code.lines().count();
        return line != null && line >= 1 && line <= lines ? line : null;
    }
}
