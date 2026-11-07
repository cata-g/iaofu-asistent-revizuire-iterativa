package ro.upb.iaofu.reviewer.problem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;

class ProblemRepositoryTest {

    @Test
    void loadsTwoSumWithSources() {
        Problem p = Fixtures.twoSum();

        assertThat(p.title()).isNotBlank();
        assertThat(p.signature().paramNames()).containsExactly("nums", "target");
        assertThat(p.tests()).isNotEmpty();
        assertThat(p.referenceCode()).contains("def two_sum");
        assertThat(p.inputsCode()).contains("def generate").contains("def is_valid");
    }

    @Test
    void starterCodeFollowsTheSignature() {
        assertThat(Fixtures.twoSum().starterCode())
                .isEqualTo("def two_sum(nums: list[int], target: int) -> list[int]:\n    pass\n");
    }

    @Test
    void publicDetailNeverContainsHiddenFields() {
        String detail = Fixtures.JSON.writeValueAsString(ProblemDetail.of(Fixtures.twoSum()));

        assertThat(detail).contains("starterCode").contains("statement");
        assertThat(detail).doesNotContain("\"tests\"", "\"expected\"", "referenceCode", "inputsCode",
                "forbiddenTerms", "comparator", "\"large\"", "seen = {}");
    }

    @Test
    void summaryHasOnlyListFields() {
        String summary = Fixtures.JSON.writeValueAsString(ProblemSummary.of(Fixtures.twoSum()));

        assertThat(Fixtures.JSON.readTree(summary).propertyNames())
                .containsExactlyInAnyOrder("id", "title", "category", "difficulty");
    }
}
