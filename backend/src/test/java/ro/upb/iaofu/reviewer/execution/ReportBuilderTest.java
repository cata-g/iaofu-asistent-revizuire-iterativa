package ro.upb.iaofu.reviewer.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;
import ro.upb.iaofu.reviewer.problem.Problem;

class ReportBuilderTest {

    private final Problem problem = Fixtures.twoSum();
    private final HarnessResult reference = Fixtures.json("two-sum/harness-raw-reference.json", HarnessResult.class);

    private TestReport report(String version) {
        return ReportBuilder.build(problem, Fixtures.json("two-sum/harness-raw-" + version + ".json",
                HarnessResult.class), reference);
    }

    @Test
    void quadraticVersionIsCorrectButTooSlow() {
        TestReport r = report("v1");

        assertThat(r.status()).isEqualTo("ok");
        assertThat(r.efficiency()).isEqualTo(TestReport.EFFICIENCY_TOO_SLOW);
        assertThat(r.slope()).isGreaterThan(1.7);
        assertThat(r.failedTests()).extracting(TestReport.TestResult::id)
                .containsExactly("large-10000", "large-100000");
        assertThat(r.summary()).isEqualTo(new TestReport.Summary(12, 14));
    }

    @Test
    void dictVersionFailsTheSameElementCases() {
        TestReport r = report("v2");

        assertThat(r.efficiency()).isEqualTo(TestReport.EFFICIENCY_OK);
        assertThat(r.slope()).isLessThan(1.4);
        assertThat(r.failedTests()).extracting(TestReport.TestResult::id)
                .containsExactly("edge-same-value-twice", "edge-half-target-once", "edge-zeros");
        TestReport.TestResult failed = r.failedTests().get(1);
        assertThat(failed.input().toString()).isEqualTo("{\"nums\":[3,2,4],\"target\":6}");
        assertThat(failed.expected().toString()).isEqualTo("[1,2]");
        assertThat(failed.actual().toString()).isEqualTo("[0,0]");
    }

    @Test
    void matchesTheReportFixture() {
        TestReport fixture = Fixtures.json("two-sum/report-v2.json", TestReport.class);
        TestReport r = report("v2");

        assertThat(r.summary()).isEqualTo(fixture.summary());
        assertThat(r.timings()).isEqualTo(fixture.timings());
        assertThat(r.slope()).isEqualTo(fixture.slope());
    }

    @Test
    void passedTestsDoNotRevealInputOrExpected() {
        String json = Fixtures.JSON.writeValueAsString(report("v2").tests().get(0));

        assertThat(json).isEqualTo("{\"id\":\"basic-1\",\"group\":\"basic\",\"passed\":true,\"timeMs\":"
                + report("v2").tests().get(0).timeMs() + "}");
    }

    @Test
    void wrongAnswerOnLargeInputIsDetectedByDigest() {
        HarnessResult raw = Fixtures.json("two-sum/harness-raw-v2.json", HarnessResult.class);
        List<HarnessResult.RawTiming> tampered = raw.timings().stream()
                .map(t -> t.n() == 1000 ? new HarnessResult.RawTiming(t.n(), "ok", t.timeMs(), "different", null) : t)
                .toList();
        TestReport r = ReportBuilder.build(problem, new HarnessResult("ok", null, raw.tests(), tampered, null, null,
                null), reference);

        assertThat(r.timings()).extracting(TestReport.Timing::status).containsExactly("ok", "wrong", "ok", "ok");
        assertThat(r.failedTests()).extracting(TestReport.TestResult::id).contains("large-1000");
        assertThat(r.efficiency()).isEqualTo(TestReport.EFFICIENCY_OK);
    }

    @Test
    void compileErrorHasNoTests() {
        TestReport r = ReportBuilder.build(problem,
                new HarnessResult("compile_error", "line 2: SyntaxError: invalid syntax", null, null, null, null, null),
                reference);

        assertThat(r.status()).isEqualTo("compile_error");
        assertThat(r.error()).contains("SyntaxError");
        assertThat(r.tests()).isEmpty();
        assertThat(r.summary()).isEqualTo(new TestReport.Summary(0, 14));
        assertThat(r.efficiency()).isEqualTo(TestReport.EFFICIENCY_UNKNOWN);
    }
}
