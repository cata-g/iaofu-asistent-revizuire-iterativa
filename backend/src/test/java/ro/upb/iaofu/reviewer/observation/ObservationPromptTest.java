package ro.upb.iaofu.reviewer.observation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.problem.Problem;
import ro.upb.iaofu.reviewer.version.Classification;
import ro.upb.iaofu.reviewer.version.DiffService;

class ObservationPromptTest {

    private final Problem problem = Fixtures.twoSum();
    private final String v1 = Fixtures.text("two-sum/v1.py");
    private final String v2 = Fixtures.text("two-sum/v2.py");

    private String prompt() {
        return ObservationPrompt.user(new ObservationPrompt.Context(problem, 2, v2, DiffService.unifiedDiff(1, v1, 2, v2),
                Fixtures.json("two-sum/report-v2.json", TestReport.class),
                Fixtures.json("two-sum/classification-v1-v2.json", Classification.class)));
    }

    @Test
    void containsWhatTheGeneratorNeeds() {
        String p = prompt();

        assertThat(p).contains(problem.statement());
        assertThat(p).contains("  4 |         seen[x] = i");
        assertThat(p).contains("```diff").contains("+    seen = {}");
        assertThat(p).contains("edge-half-target-once (edge): input {\"nums\":[3,2,4],\"target\":6}; așteptat [1,2]; obținut [0,0]");
        assertThat(p).contains("Panta log-log ≈ 1.07");
        assertThat(p).contains("Regresie nouă: edge-same-value-twice, edge-half-target-once, edge-zeros");
    }

    @Test
    void neverContainsHiddenData() {
        String p = prompt();

        assertThat(p).doesNotContain(problem.referenceCode().strip());
        assertThat(p).doesNotContain("basic-1").doesNotContain("[2,7,11,15],\"target\":9}; așteptat");
        assertThat(p).doesNotContain("forbidden").doesNotContain("complement");
    }

    @Test
    void firstVersionSaysSo() {
        String p = ObservationPrompt.user(new ObservationPrompt.Context(problem, 1, v1, null,
                Fixtures.json("two-sum/report-v1.json", TestReport.class), null));

        assertThat(p).contains("Este prima versiune.").contains("prea lent").doesNotContain("## Față de versiunea");
    }
}
