package ro.upb.iaofu.reviewer.execution;

import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Raw output of harness.py (docs/contracte.md section 2.1). Which fields are set
 * depends on the mode: evaluate sets tests and timings, single sets actual,
 * validate sets valid.
 */
public record HarnessResult(
        String status,
        String error,
        List<RawTest> tests,
        List<RawTiming> timings,
        JsonNode actual,
        Double timeMs,
        Boolean valid) {

    public static final String OK = "ok";
    public static final String COMPILE_ERROR = "compile_error";
    /** Set by the backend when the process died without printing a result (killed by Piston). */
    public static final String CRASHED = "error";

    public record RawTest(String id, String group, boolean passed, JsonNode actual, String error, Double timeMs) {
    }

    public record RawTiming(int n, String status, Double timeMs, String digest, String error) {
    }

    static HarnessResult crashed(String error) {
        return new HarnessResult(CRASHED, error, List.of(), List.of(), null, null, null);
    }

    public boolean ok() {
        return OK.equals(status);
    }
}
