package ro.upb.iaofu.reviewer.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** What an execution returns (docs/contracte.md section 2.2). Safe to send to the frontend. */
public record TestReport(
        String status,
        String error,
        Summary summary,
        List<TestResult> tests,
        List<Timing> timings,
        Double slope,
        String efficiency) {

    public static final String EFFICIENCY_OK = "ok";
    public static final String EFFICIENCY_TOO_SLOW = "too_slow";
    public static final String EFFICIENCY_UNKNOWN = "unknown";

    public record Summary(int passed, int total) {
    }

    /** Failed tests carry input, expected, actual and error; passed tests only id, group, passed, timeMs. */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public record TestResult(String id, String group, boolean passed, JsonNode input, JsonNode expected,
                             JsonNode actual, String error, Double timeMs) {

        static TestResult passed(String id, String group, Double timeMs) {
            return new TestResult(id, group, true, null, null, null, null, timeMs);
        }
    }

    public record Timing(int n, String status, Double timeMs) {
    }

    public boolean allPassed() {
        return "ok".equals(status) && summary.passed() == summary.total();
    }

    public List<TestResult> failedTests() {
        return tests.stream().filter(t -> !t.passed()).toList();
    }
}
