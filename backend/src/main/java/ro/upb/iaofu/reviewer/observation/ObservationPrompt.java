package ro.upb.iaofu.reviewer.observation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.problem.Problem;
import ro.upb.iaofu.reviewer.version.Classification;

/**
 * Builds the user message for the generator (F11): statement, code with line numbers, diff,
 * failed tests, timings and classification. Never includes the reference solution or the
 * tests that pass.
 */
public final class ObservationPrompt {

    private ObservationPrompt() {
    }

    public static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("observations"),
            "properties", Map.of("observations", Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "additionalProperties", false,
                            "required", List.of("type", "line", "evidenceInputJson", "risk", "hints"),
                            "properties", Map.of(
                                    "type", Map.of("type", "string", "enum", Proposal.TYPES),
                                    "line", Map.of("type", "integer"),
                                    "evidenceInputJson", Map.of("type", "string"),
                                    "risk", Map.of("type", "string"),
                                    "hints", Map.of("type", "array", "items", Map.of("type", "string")))))));

    public record Context(Problem problem, int version, String code, String diff, TestReport report,
                          Classification classification) {
    }

    public static String user(Context c) {
        StringBuilder s = new StringBuilder();
        Problem p = c.problem();
        s.append("## Enunț\n\n").append(p.statement()).append("\n\n");
        s.append("Semnătură: `").append(p.starterCode().lines().findFirst().orElse("")).append("`\n\n");
        s.append("Constrângeri:\n");
        p.constraints().forEach(x -> s.append("- ").append(x).append('\n'));
        s.append("- Limita de timp pe inputul mare: ").append(p.timeLimitMs()).append(" ms\n\n");

        s.append("## Codul studentului (versiunea ").append(c.version()).append(")\n\n```python\n")
                .append(numbered(c.code())).append("```\n\n");

        s.append("## Diferența față de versiunea anterioară\n\n");
        if (c.diff() == null) {
            s.append("Este prima versiune.\n\n");
        } else if (c.diff().isEmpty()) {
            s.append("Codul nu s-a schimbat.\n\n");
        } else {
            s.append("```diff\n").append(c.diff()).append("\n```\n\n");
        }

        s.append("## Rezultatele testelor\n\n").append(results(c.report())).append('\n');
        if (c.classification() != null) {
            s.append("## Față de versiunea ").append(c.classification().previousVersion()).append("\n\n")
                    .append("- Rezolvat: ").append(ids(c.classification().resolved())).append('\n')
                    .append("- Regresie nouă: ").append(ids(c.classification().regression())).append('\n')
                    .append("- Rămas: ").append(ids(c.classification().remaining())).append('\n');
        }
        return s.toString();
    }

    static String numbered(String code) {
        List<String> lines = code.replace("\r\n", "\n").lines().toList();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            s.append(String.format(Locale.ROOT, "%3d | %s%n", i + 1, lines.get(i)));
        }
        return s.toString();
    }

    private static String results(TestReport r) {
        if (!"ok".equals(r.status())) {
            return "Codul nu rulează: " + r.error() + "\n";
        }
        StringBuilder s = new StringBuilder();
        s.append(r.summary().passed()).append(" din ").append(r.summary().total()).append(" teste trec.\n");
        List<TestReport.TestResult> failed = r.failedTests();
        if (!failed.isEmpty()) {
            s.append("\nTeste picate:\n");
            for (TestReport.TestResult t : failed) {
                s.append("- ").append(t.id()).append(" (").append(t.group()).append("): input ")
                        .append(t.input());
                if (t.expected() != null) {
                    s.append("; așteptat ").append(t.expected());
                }
                if (t.error() != null) {
                    s.append("; eroare: ").append(t.error());
                } else {
                    s.append("; obținut ").append(t.actual());
                }
                s.append('\n');
            }
        }
        s.append("\nTimpi pe inputuri mari: ");
        s.append(r.timings().stream().map(t -> "n = " + t.n() + ": "
                + ("ok".equals(t.status()) || "wrong".equals(t.status())
                ? String.format(Locale.ROOT, "%.3f ms", t.timeMs()) : t.status())).collect(Collectors.joining("; ")));
        if (r.slope() != null) {
            s.append(String.format(Locale.ROOT, ". Panta log-log ≈ %.2f (timpul crește cam ca n^%.1f)", r.slope(),
                    r.slope()));
        }
        s.append(". Eficiență: ").append("too_slow".equals(r.efficiency()) ? "prea lent" : "în limita de timp")
                .append(".\n");
        return s.toString();
    }

    private static String ids(List<Classification.ClassifiedTest> tests) {
        return tests.isEmpty() ? "nimic" : tests.stream().map(Classification.ClassifiedTest::testId)
                .collect(Collectors.joining(", "));
    }
}
