package ro.upb.iaofu.reviewer.version;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ro.upb.iaofu.reviewer.execution.TestReport;

/** Deterministic comparison of two versions by test id (F9); no LLM involved. */
public final class VersionClassifier {

    private VersionClassifier() {
    }

    public static Classification classify(int previousVersion, TestReport previous, TestReport current) {
        Map<String, Boolean> before = outcomes(previous);
        List<Classification.ClassifiedTest> resolved = new ArrayList<>();
        List<Classification.ClassifiedTest> regression = new ArrayList<>();
        List<Classification.ClassifiedTest> remaining = new ArrayList<>();

        for (TestReport.TestResult test : current.tests()) {
            Boolean passedBefore = before.get(test.id());
            Classification.ClassifiedTest item = new Classification.ClassifiedTest(test.id(), test.group());
            if (passedBefore == null) {
                // Previous version did not run (compile error): every failure now is still open.
                if (!test.passed()) {
                    remaining.add(item);
                }
            } else if (!passedBefore && test.passed()) {
                resolved.add(item);
            } else if (passedBefore && !test.passed()) {
                regression.add(item);
            } else if (!passedBefore) {
                remaining.add(item);
            }
        }
        // Tests that ran before but not now (current version does not compile) are regressions.
        if (current.tests().isEmpty()) {
            previous.tests().stream().filter(TestReport.TestResult::passed)
                    .forEach(t -> regression.add(new Classification.ClassifiedTest(t.id(), t.group())));
            previous.tests().stream().filter(t -> !t.passed())
                    .forEach(t -> remaining.add(new Classification.ClassifiedTest(t.id(), t.group())));
        }
        return new Classification(previousVersion, resolved, regression, remaining,
                new Classification.EfficiencyChange(previous.efficiency(), current.efficiency()));
    }

    private static Map<String, Boolean> outcomes(TestReport report) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        report.tests().forEach(t -> map.put(t.id(), t.passed()));
        return map;
    }
}
