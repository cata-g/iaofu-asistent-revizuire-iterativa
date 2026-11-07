package ro.upb.iaofu.reviewer.feedback;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ro.upb.iaofu.reviewer.llm.LlmClient;
import ro.upb.iaofu.reviewer.llm.LlmGateway;
import ro.upb.iaofu.reviewer.llm.Prompts;
import ro.upb.iaofu.reviewer.observation.ObservationPipeline;
import ro.upb.iaofu.reviewer.observation.ObservationPrompt;
import ro.upb.iaofu.reviewer.session.Mode;

/** The three variants. The LLM is not called when every test passes, in any variant. */
@Component
public class FeedbackStrategies {

    private static final Logger log = LoggerFactory.getLogger(FeedbackStrategies.class);
    static final String PURPOSE_BASELINE_B = "baseline_b";
    static final long BASELINE_B_MAX_TOKENS = 4000;

    private final Map<Mode, FeedbackStrategy> byMode = new EnumMap<>(Mode.class);

    public FeedbackStrategies(ObservationPipeline pipeline, LlmGateway llm) {
        List.of(new BaselineA(), new BaselineB(llm), new Proposed(pipeline)).forEach(s -> byMode.put(s.mode(), s));
    }

    public FeedbackStrategy forMode(Mode mode) {
        return byMode.get(mode);
    }

    /** Test results only, like LeetCode. */
    static class BaselineA implements FeedbackStrategy {

        @Override
        public Mode mode() {
            return Mode.BASELINE_A;
        }

        @Override
        public Result produce(UUID submissionId, ObservationPrompt.Context context) {
            return Result.notUsed();
        }
    }

    /** Same model, asked "what is wrong here?" with only the statement and the code. */
    static class BaselineB implements FeedbackStrategy {

        private final LlmGateway llm;

        BaselineB(LlmGateway llm) {
            this.llm = llm;
        }

        @Override
        public Mode mode() {
            return Mode.BASELINE_B;
        }

        @Override
        public Result produce(UUID submissionId, ObservationPrompt.Context context) {
            if (context.report().allPassed()) {
                return Result.notUsed();
            }
            String user = context.problem().statement() + "\n\n```python\n" + context.code() + "\n```\n\nCe e greșit aici?";
            return guarded(() -> new Result(List.of(), llm.call(submissionId, PURPOSE_BASELINE_B,
                    new LlmClient.Request(Prompts.BASELINE_B, user, null, BASELINE_B_MAX_TOKENS)).text(), LLM_OK));
        }
    }

    /** Verified observations with graded hints. */
    static class Proposed implements FeedbackStrategy {

        private final ObservationPipeline pipeline;

        Proposed(ObservationPipeline pipeline) {
            this.pipeline = pipeline;
        }

        @Override
        public Mode mode() {
            return Mode.PROPOSED;
        }

        @Override
        public Result produce(UUID submissionId, ObservationPrompt.Context context) {
            if (context.report().allPassed() || !"ok".equals(context.report().status())) {
                // Nothing to verify against when the code does not run; the error is shown as is.
                return Result.notUsed();
            }
            return guarded(() -> {
                ObservationPipeline.Result r = pipeline.run(submissionId, context);
                return new Result(r.shown(), null, r.llmStatus());
            });
        }
    }

    /** Any LLM problem degrades to test results only (N3). */
    static FeedbackStrategy.Result guarded(Supplier<FeedbackStrategy.Result> body) {
        try {
            return body.get();
        } catch (LlmGateway.UnavailableException e) {
            log.warn("LLM unavailable: {}", e.getMessage());
            return FeedbackStrategy.Result.unavailable(e.isBudget()
                    ? FeedbackStrategy.LLM_BUDGET_EXCEEDED : FeedbackStrategy.LLM_FAILED);
        } catch (RuntimeException e) {
            log.warn("LLM feedback failed: {}", e.getMessage());
            return FeedbackStrategy.Result.unavailable(FeedbackStrategy.LLM_FAILED);
        }
    }
}
