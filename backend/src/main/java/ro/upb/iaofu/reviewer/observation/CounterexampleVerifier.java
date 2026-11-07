package ro.upb.iaofu.reviewer.observation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import ro.upb.iaofu.reviewer.execution.ExecutionService;
import ro.upb.iaofu.reviewer.execution.HarnessResult;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Confirms an observation only if running the student's code and the reference on its
 * evidence input gives different results (F12). Efficiency claims are confirmed by the
 * measured timings instead.
 */
@Component
public class CounterexampleVerifier {

    public static final String INVALID_INPUT = "invalid_input";
    public static final String SAME_OUTPUT = "same_output";
    public static final String REFERENCE_ERROR = "reference_error";
    public static final String NOT_SLOW = "not_slow";

    private final ExecutionService execution;

    public CounterexampleVerifier(ExecutionService execution) {
        this.execution = execution;
    }

    /** studentOutput/expectedOutput are what the student sees as evidence; rejectReason is null when confirmed. */
    public record Verdict(boolean confirmed, String rejectReason, JsonNode studentOutput, JsonNode expectedOutput) {

        static Verdict rejected(String reason) {
            return new Verdict(false, reason, null, null);
        }
    }

    public Verdict verify(Problem problem, String code, TestReport report, Proposal proposal) {
        if (proposal.isEfficiency()) {
            return verifyEfficiency(problem, report);
        }
        JsonNode input = proposal.evidenceInput();
        if (!hasExactlyTheParams(problem, input) || !execution.isValidInput(problem, input)) {
            return Verdict.rejected(INVALID_INPUT);
        }
        HarnessResult reference = execution.runSingle(problem, problem.referenceCode(), input);
        if (!reference.ok() || reference.error() != null) {
            return Verdict.rejected(REFERENCE_ERROR);
        }
        HarnessResult student = execution.runSingle(problem, code, input);
        JsonNode studentOutput = studentOutput(student);
        boolean differs = !student.ok() || student.error() != null
                || !Objects.equals(student.actual(), reference.actual());
        return differs ? new Verdict(true, null, studentOutput, reference.actual())
                : new Verdict(false, SAME_OUTPUT, studentOutput, reference.actual());
    }

    private static Verdict verifyEfficiency(Problem problem, TestReport report) {
        return report.timings().stream().filter(t -> "timeout".equals(t.status())).findFirst()
                .map(t -> new Verdict(true, null, JsonNodeFactory.instance.stringNode(String.format(Locale.ROOT,
                        "La n = %,d depășește limita de %d ms.", t.n(), problem.timeLimitMs()).replace(',', ' ')),
                        null))
                .orElse(Verdict.rejected(NOT_SLOW));
    }

    private static JsonNode studentOutput(HarnessResult student) {
        if (!student.ok()) {
            return JsonNodeFactory.instance.stringNode("eroare: " + student.error());
        }
        if (student.error() != null) {
            return JsonNodeFactory.instance.stringNode("timeout".equals(student.error())
                    ? "nu termină (timeout)" : "eroare: " + student.error());
        }
        return student.actual();
    }

    private static boolean hasExactlyTheParams(Problem problem, JsonNode input) {
        if (input == null || !input.isObject()) {
            return false;
        }
        Set<String> names = new HashSet<>(input.propertyNames());
        return names.equals(new HashSet<>(problem.signature().paramNames()));
    }
}
