package ro.upb.iaofu.reviewer.observation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;
import ro.upb.iaofu.reviewer.problem.Problem;

class HintLeakFilterTest {

    private final List<Problem.ForbiddenTerm> terms = Fixtures.twoSum().forbiddenTerms();
    private final String v1 = Fixtures.text("two-sum/v1.py");
    private final String v2 = Fixtures.text("two-sum/v2.py");

    @Test
    void contextExampleHintsAreCleanForV2() {
        List<String> hints = List.of(
                "Pe [3, 2, 4] primești [0, 0]. Ce index apare de două ori?",
                "Contează ordinea în care adaugi și cauți în dicționar?",
                "Verifică complementul înainte să adaugi elementul curent.");

        // v2 already uses a dict, so naming it at level 2 reveals nothing new; level 3 is not checked.
        assertThat(HintLeakFilter.leakedTerms(hints, terms, v2)).isEmpty();
    }

    @Test
    void namingTheDictForTheQuadraticVersionLeaks() {
        List<String> hints = List.of("Poți evita a doua buclă?", "Folosește un Dicționar pentru valori.", "...");

        assertThat(HintLeakFilter.leakedTerms(hints, terms, v1)).containsExactlyInAnyOrder("dicționar", "dictionar");
    }

    @Test
    void matchingIgnoresDiacriticsAndCase() {
        assertThat(HintLeakFilter.leakedTerms(List.of("Un DICTIONAR ar ajuta.", "x"), terms, v1))
                .contains("dicționar", "dictionar");
        assertThat(HintLeakFilter.leakedTerms(List.of("Caută complement-ul.", "x"), terms, v1))
                .containsExactly("complement");
        // v2 already computes target - x, so naming the complement reveals nothing new
        assertThat(HintLeakFilter.leakedTerms(List.of("Caută complementul.", "x"), terms, v2)).isEmpty();
    }

    @Test
    void longTermsCatchRomanianInflections() {
        assertThat(HintLeakFilter.leakedTerms(List.of("Caută COMPLEMENTUL.", "x"), terms, v1))
                .containsExactly("complement");
        assertThat(HintLeakFilter.leakedTerms(List.of("Pune valorile în dicționarul tău.", "x"), terms, v1))
                .contains("dicționar");
        assertThat(HintLeakFilter.leakedTerms(List.of("Ce elemente are mulțimea?", "x"), terms, v1))
                .containsExactly("mulțime");
    }

    @Test
    void onlyWholeWordsMatch() {
        assertThat(HintLeakFilter.leakedTerms(List.of("Setează indicii corect.", "Ce hashtag?"), terms, v1)).isEmpty();
        assertThat(HintLeakFilter.leakedTerms(List.of("Un set de valori văzute.", "x"), terms, v1))
                .containsExactly("set");
    }

    @Test
    void multiWordTerms() {
        assertThat(HintLeakFilter.leakedTerms(List.of("x", "Încearcă o singură parcurgere."), terms, v1))
                .containsExactly("o singură parcurgere");
    }
}
