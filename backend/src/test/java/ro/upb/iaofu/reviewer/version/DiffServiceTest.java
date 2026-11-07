package ro.upb.iaofu.reviewer.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;

class DiffServiceTest {

    @Test
    void unifiedDiffBetweenVersions() {
        String diff = DiffService.unifiedDiff(1, Fixtures.text("two-sum/v1.py"), 2, Fixtures.text("two-sum/v2.py"));

        assertThat(diff).startsWith("--- v1/solution.py\n+++ v2/solution.py\n@@");
        assertThat(diff).contains("-    for i in range(len(nums)):").contains("+    seen = {}");
    }

    @Test
    void identicalCodeHasNoDiff() {
        assertThat(DiffService.unifiedDiff(1, "a\nb\n", 2, "a\r\nb\r\n")).isEmpty();
    }
}
