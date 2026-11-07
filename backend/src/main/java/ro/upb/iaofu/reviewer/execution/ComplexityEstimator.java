package ro.upb.iaofu.reviewer.execution;

import java.util.List;

/** Slope of log10(time) against log10(n): ~1 for O(n), ~2 for O(n^2) (F6). */
public final class ComplexityEstimator {

    private ComplexityEstimator() {
    }

    /** Least-squares slope over the timings that finished; null with fewer than two points. */
    public static Double slope(List<TestReport.Timing> timings) {
        List<double[]> points = timings.stream()
                .filter(t -> ("ok".equals(t.status()) || "wrong".equals(t.status()))
                        && t.timeMs() != null && t.timeMs() > 0)
                .map(t -> new double[]{Math.log10(t.n()), Math.log10(t.timeMs())})
                .toList();
        if (points.size() < 2) {
            return null;
        }
        double mx = points.stream().mapToDouble(p -> p[0]).average().orElseThrow();
        double my = points.stream().mapToDouble(p -> p[1]).average().orElseThrow();
        double num = 0;
        double den = 0;
        for (double[] p : points) {
            num += (p[0] - mx) * (p[1] - my);
            den += (p[0] - mx) * (p[0] - mx);
        }
        return Math.round(num / den * 100) / 100.0;
    }
}
