package ro.upb.iaofu.reviewer.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import ro.upb.iaofu.reviewer.problem.Problem;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.StringNode;

/** Turns a raw harness result into a {@link TestReport}, comparing large inputs with the reference. */
public final class ReportBuilder {

    private ReportBuilder() {
    }

    public static TestReport build(Problem problem, HarnessResult raw, HarnessResult reference) {
        if (!raw.ok()) {
            return new TestReport(raw.status(), raw.error(), new TestReport.Summary(0, totalTests(problem)),
                    List.of(), List.of(), null, TestReport.EFFICIENCY_UNKNOWN);
        }
        Map<Integer, String> expectedDigest = reference.timings().stream()
                .collect(Collectors.toMap(HarnessResult.RawTiming::n, HarnessResult.RawTiming::digest));

        List<TestReport.TestResult> tests = new ArrayList<>();
        for (HarnessResult.RawTest t : raw.tests()) {
            if (t.passed()) {
                tests.add(TestReport.TestResult.passed(t.id(), t.group(), t.timeMs()));
            } else {
                Problem.TestCase source = problem.test(t.id());
                tests.add(new TestReport.TestResult(t.id(), t.group(), false, source.input(), source.expected(),
                        t.actual(), t.error(), t.timeMs()));
            }
        }

        List<TestReport.Timing> timings = new ArrayList<>();
        for (HarnessResult.RawTiming t : raw.timings()) {
            String status = t.status();
            if ("ok".equals(status) && !Objects.equals(t.digest(), expectedDigest.get(t.n()))) {
                status = "wrong";
            }
            timings.add(new TestReport.Timing(t.n(), status, t.timeMs()));
            tests.add(largeTest(t, status));
        }

        int passed = (int) tests.stream().filter(TestReport.TestResult::passed).count();
        boolean timedOut = timings.stream().anyMatch(t -> "timeout".equals(t.status()));
        return new TestReport(HarnessResult.OK, null, new TestReport.Summary(passed, tests.size()), tests, timings,
                ComplexityEstimator.slope(timings),
                timedOut ? TestReport.EFFICIENCY_TOO_SLOW : TestReport.EFFICIENCY_OK);
    }

    private static TestReport.TestResult largeTest(HarnessResult.RawTiming t, String status) {
        String id = "large-" + t.n();
        if ("ok".equals(status)) {
            return TestReport.TestResult.passed(id, "large", t.timeMs());
        }
        StringNode input = JsonNodeFactory.instance.stringNode("n = " + t.n() + " (generat)");
        String error = switch (status) {
            case "timeout" -> "timeout";
            case "skipped" -> "nerulat: n mai mic a depășit timpul";
            case "wrong" -> null;
            default -> t.error();
        };
        StringNode actual = "wrong".equals(status)
                ? JsonNodeFactory.instance.stringNode("răspuns diferit de cel corect") : null;
        return new TestReport.TestResult(id, "large", false, input, null, actual, error, t.timeMs());
    }

    private static int totalTests(Problem problem) {
        return problem.tests().size() + problem.large().sizes().size();
    }
}
