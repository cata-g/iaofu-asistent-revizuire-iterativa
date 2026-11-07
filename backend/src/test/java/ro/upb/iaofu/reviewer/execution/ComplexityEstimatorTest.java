package ro.upb.iaofu.reviewer.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class ComplexityEstimatorTest {

    private static TestReport.Timing ok(int n, double ms) {
        return new TestReport.Timing(n, "ok", ms);
    }

    @Test
    void linearHasSlopeNearOne() {
        Double slope = ComplexityEstimator.slope(List.of(ok(100, 0.01), ok(1000, 0.1), ok(10000, 1.0),
                ok(100000, 10.0)));
        assertThat(slope).isCloseTo(1.0, within(0.01));
    }

    @Test
    void quadraticHasSlopeNearTwo() {
        Double slope = ComplexityEstimator.slope(List.of(ok(100, 0.2), ok(1000, 21.5)));
        assertThat(slope).isCloseTo(2.03, within(0.01));
    }

    @Test
    void ignoresTimeoutsAndSkipped() {
        Double slope = ComplexityEstimator.slope(List.of(ok(100, 0.2), ok(1000, 20.0),
                new TestReport.Timing(10000, "timeout", null), new TestReport.Timing(100000, "skipped", null)));
        assertThat(slope).isCloseTo(2.0, within(0.01));
    }

    @Test
    void needsTwoPoints() {
        assertThat(ComplexityEstimator.slope(List.of(ok(100, 0.2),
                new TestReport.Timing(1000, "timeout", null)))).isNull();
        assertThat(ComplexityEstimator.slope(List.of())).isNull();
    }
}
