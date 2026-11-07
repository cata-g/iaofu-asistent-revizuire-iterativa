package ro.upb.iaofu.reviewer.version;

import java.util.List;

/** Resolved / regression / remaining between two versions (docs/contracte.md section 3). */
public record Classification(
        int previousVersion,
        List<ClassifiedTest> resolved,
        List<ClassifiedTest> regression,
        List<ClassifiedTest> remaining,
        EfficiencyChange efficiency) {

    public record ClassifiedTest(String testId, String group) {
    }

    public record EfficiencyChange(String before, String after) {
    }
}
