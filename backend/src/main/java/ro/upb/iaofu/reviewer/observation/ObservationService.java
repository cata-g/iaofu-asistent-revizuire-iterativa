package ro.upb.iaofu.reviewer.observation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upb.iaofu.reviewer.common.ConflictException;
import ro.upb.iaofu.reviewer.common.NotFoundException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Shows observations to the student, unlocks hints and records feedback. */
@Service
public class ObservationService {

    public static final int LEVELS = 3;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObservationRepository observations;
    private final HintRevealRepository reveals;
    private final FeedbackRepository feedback;
    private final JsonMapper json;

    public ObservationService(ObservationRepository observations, HintRevealRepository reveals,
                              FeedbackRepository feedback, JsonMapper json) {
        this.observations = observations;
        this.reveals = reveals;
        this.feedback = feedback;
        this.json = json;
    }

    public record HintResponse(int level, String text, int levelsAvailable) {
    }

    /** Marks confirmed observations as shown and records level 1 as revealed for each. */
    @Transactional
    public void show(List<Observation> shown) {
        for (Observation o : shown) {
            o.setShown(true);
            observations.save(o);
            reveals.save(new HintReveal(o.getId(), 1));
        }
    }

    @Transactional
    public HintResponse nextHint(UUID observationId) {
        Observation o = shownObservation(observationId);
        int current = reveals.findTopByObservationIdOrderByLevelDesc(observationId)
                .map(HintReveal::getLevel).orElse(0);
        if (current >= LEVELS) {
            throw new ConflictException("All hint levels are already unlocked");
        }
        int next = current + 1;
        reveals.save(new HintReveal(observationId, next));
        return new HintResponse(next, hints(o).get(next - 1), LEVELS);
    }

    @Transactional
    public void feedback(UUID observationId, String verdict) {
        if (!Feedback.USEFUL.equals(verdict) && !Feedback.FALSE.equals(verdict)) {
            throw new IllegalArgumentException("verdict must be 'useful' or 'false'");
        }
        shownObservation(observationId);
        feedback.save(new Feedback(observationId, verdict));
    }

    /** Views of the observations shown for the given submissions, keyed by submission id. */
    @Transactional(readOnly = true)
    public Map<UUID, List<ObservationView>> viewsBySubmission(Collection<UUID> submissionIds) {
        List<Observation> shown = observations.findBySubmissionIdInAndShownTrueOrderByCreatedAtAsc(submissionIds);
        List<UUID> ids = shown.stream().map(Observation::getId).toList();
        Map<UUID, Integer> levels = reveals.findByObservationIdIn(ids).stream()
                .collect(Collectors.toMap(HintReveal::getObservationId, HintReveal::getLevel, Math::max));
        Map<UUID, String> verdicts = feedback.findByObservationIdInOrderByCreatedAtAsc(ids).stream()
                .collect(Collectors.toMap(Feedback::getObservationId, Feedback::getVerdict, (a, b) -> b));
        return shown.stream().collect(Collectors.groupingBy(Observation::getSubmissionId,
                Collectors.mapping(o -> view(o, levels.getOrDefault(o.getId(), 1), verdicts.get(o.getId())),
                        Collectors.toList())));
    }

    public List<ObservationView> views(UUID submissionId) {
        return viewsBySubmission(List.of(submissionId)).getOrDefault(submissionId, List.of());
    }

    ObservationView view(Observation o, int unlockedLevels, String verdict) {
        List<String> hints = hints(o);
        List<ObservationView.Hint> visible = IntStream.rangeClosed(1, Math.min(unlockedLevels, LEVELS))
                .mapToObj(level -> new ObservationView.Hint(level, hints.get(level - 1)))
                .toList();
        return new ObservationView(o.getId(), o.getType(), o.getLine(), evidence(o), o.getRisk(), visible, LEVELS,
                verdict);
    }

    private ObservationView.Evidence evidence(Observation o) {
        if ("efficiency".equals(o.getType())) {
            String description = o.getStudentOutputJson() == null ? null : json.readTree(o.getStudentOutputJson()).asString();
            return new ObservationView.Evidence(null, null, null, description);
        }
        return new ObservationView.Evidence(readOrNull(o.getEvidenceInputJson()), readOrNull(o.getStudentOutputJson()),
                readOrNull(o.getExpectedOutputJson()), null);
    }

    private JsonNode readOrNull(String text) {
        return text == null ? null : json.readTree(text);
    }

    private List<String> hints(Observation o) {
        return json.readValue(o.getHintsJson(), STRING_LIST);
    }

    private Observation shownObservation(UUID id) {
        return observations.findById(id).filter(Observation::isShown)
                .orElseThrow(() -> new NotFoundException("Observation " + id + " not found"));
    }
}
