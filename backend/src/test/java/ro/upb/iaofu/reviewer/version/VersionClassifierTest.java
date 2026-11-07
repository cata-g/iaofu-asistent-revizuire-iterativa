package ro.upb.iaofu.reviewer.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;
import ro.upb.iaofu.reviewer.execution.TestReport;

class VersionClassifierTest {

    private final TestReport v1 = Fixtures.json("two-sum/report-v1.json", TestReport.class);
    private final TestReport v2 = Fixtures.json("two-sum/report-v2.json", TestReport.class);

    private static TestReport compileError() {
        return new TestReport("compile_error", "line 1: SyntaxError", new TestReport.Summary(0, 14), List.of(),
                List.of(), null, TestReport.EFFICIENCY_UNKNOWN);
    }

    @Test
    void twoSumV1ToV2MatchesTheFixture() {
        Classification expected = Fixtures.json("two-sum/classification-v1-v2.json", Classification.class);

        assertThat(VersionClassifier.classify(1, v1, v2)).isEqualTo(expected);
    }

    @Test
    void reverseDirectionSwapsResolvedAndRegression() {
        Classification c = VersionClassifier.classify(2, v2, v1);

        assertThat(c.resolved()).extracting(Classification.ClassifiedTest::testId)
                .containsExactly("edge-same-value-twice", "edge-half-target-once", "edge-zeros");
        assertThat(c.regression()).extracting(Classification.ClassifiedTest::testId)
                .containsExactly("large-10000", "large-100000");
        assertThat(c.efficiency()).isEqualTo(new Classification.EfficiencyChange("ok", "too_slow"));
    }

    @Test
    void sameReportHasOnlyRemainingFailures() {
        Classification c = VersionClassifier.classify(1, v2, v2);

        assertThat(c.resolved()).isEmpty();
        assertThat(c.regression()).isEmpty();
        assertThat(c.remaining()).hasSize(3);
    }

    @Test
    void afterACompileErrorEveryFailureRemains() {
        Classification c = VersionClassifier.classify(1, compileError(), v2);

        assertThat(c.resolved()).isEmpty();
        assertThat(c.regression()).isEmpty();
        assertThat(c.remaining()).hasSize(3);
    }

    @Test
    void aCompileErrorTurnsPassingTestsIntoRegressions() {
        Classification c = VersionClassifier.classify(1, v2, compileError());

        assertThat(c.regression()).hasSize(11);
        assertThat(c.remaining()).hasSize(3);
        assertThat(c.efficiency().after()).isEqualTo("unknown");
    }
}
