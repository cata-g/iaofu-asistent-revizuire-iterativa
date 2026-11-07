package ro.upb.iaofu.reviewer.feedback;

import java.util.List;
import java.util.UUID;
import ro.upb.iaofu.reviewer.observation.ObservationPrompt;
import ro.upb.iaofu.reviewer.observation.ObservationView;
import ro.upb.iaofu.reviewer.session.Mode;

/** What each variant adds on top of the test results (F17). All variants share the same response shape. */
public interface FeedbackStrategy {

    String LLM_OK = "ok";
    String LLM_NOT_USED = "not_used";
    String LLM_FAILED = "failed";
    String LLM_BUDGET_EXCEEDED = "budget_exceeded";

    Mode mode();

    Result produce(UUID submissionId, ObservationPrompt.Context context);

    record Result(List<ObservationView> observations, String assistantMessage, String llmStatus) {

        public static Result notUsed() {
            return new Result(List.of(), null, LLM_NOT_USED);
        }

        public static Result unavailable(String status) {
            return new Result(List.of(), null, status);
        }
    }
}
