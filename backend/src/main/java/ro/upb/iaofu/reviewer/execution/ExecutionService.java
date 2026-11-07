package ro.upb.iaofu.reviewer.execution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.upb.iaofu.reviewer.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Runs code through harness.py in Piston. */
@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);
    static final String RESULT_MARKER = "__HARNESS_RESULT__";
    private static final long SINGLE_TIMEOUT_MS = 8_000;

    private final PistonClient piston;
    private final HarnessBuilder harness;
    private final JsonMapper json;
    private final Map<String, HarnessResult> referenceCache = new ConcurrentHashMap<>();

    public ExecutionService(PistonClient piston, HarnessBuilder harness, JsonMapper json) {
        this.piston = piston;
        this.harness = harness;
        this.json = json;
    }

    /** Runs every test and the large inputs (F4-F7). */
    public TestReport evaluate(Problem problem, String code) {
        HarnessResult raw = run(problem, code, harness.evaluateJob(problem).toString(),
                harness.evaluateTimeoutMs(problem));
        return ReportBuilder.build(problem, raw, reference(problem));
    }

    /** Runs code on one input, e.g. a counterexample proposed by the LLM. */
    public HarnessResult runSingle(Problem problem, String code, JsonNode input) {
        return run(problem, code, harness.singleJob(problem, input).toString(), SINGLE_TIMEOUT_MS);
    }

    /** Checks an input against the problem's constraints (inputs.is_valid). */
    public boolean isValidInput(Problem problem, JsonNode input) {
        HarnessResult result = run(problem, "", harness.validateJob(input).toString(), SINGLE_TIMEOUT_MS);
        return Boolean.TRUE.equals(result.valid());
    }

    /** The reference solution's raw evaluation, computed once per problem. */
    public HarnessResult reference(Problem problem) {
        return referenceCache.computeIfAbsent(problem.id(), id -> {
            HarnessResult ref = run(problem, problem.referenceCode(), harness.evaluateJob(problem).toString(),
                    harness.evaluateTimeoutMs(problem));
            boolean finished = ref.ok() && ref.timings().stream().allMatch(t -> "ok".equals(t.status()));
            if (!finished) {
                throw new ExecutionException("Reference solution of " + id + " does not finish: " + ref.error());
            }
            log.info("Cached reference run for {}", id);
            return ref;
        });
    }

    HarnessResult run(Problem problem, String code, String job, long timeoutMs) {
        PistonClient.Run run = piston.execute(harness.files(problem, code), job, timeoutMs);
        return parse(run);
    }

    HarnessResult parse(PistonClient.Run run) {
        String stdout = run.stdout() == null ? "" : run.stdout();
        for (String line : stdout.split("\n")) {
            if (line.startsWith(RESULT_MARKER)) {
                try {
                    return json.readValue(line.substring(RESULT_MARKER.length()), HarnessResult.class);
                } catch (JacksonException e) {
                    throw new ExecutionException("Unreadable harness result", e);
                }
            }
        }
        // The process was killed before printing (time or memory limit, crash).
        String reason = run.message() != null ? run.message() : "process ended without a result";
        log.warn("Harness produced no result: status={} message={} stderr={}", run.status(), run.message(),
                abbreviate(run.stderr()));
        return HarnessResult.crashed(reason);
    }

    private static String abbreviate(String s) {
        return s == null || s.length() < 300 ? s : s.substring(0, 300) + "...";
    }
}
