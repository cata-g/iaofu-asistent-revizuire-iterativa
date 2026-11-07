package ro.upb.iaofu.reviewer.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;

class ProposalParserTest {

    private List<Proposal> parse(String text) {
        return ProposalParser.parse(text, Fixtures.JSON);
    }

    @Test
    void parsesTheValidFixture() {
        List<Proposal> proposals = parse(Fixtures.text("llm/observations-valid.json"));

        assertThat(proposals).hasSize(2);
        Proposal first = proposals.getFirst();
        assertThat(first.type()).isEqualTo("edge_case");
        assertThat(first.line()).isEqualTo(4);
        assertThat(first.evidenceInput().toString()).isEqualTo("{\"nums\":[3,2,4],\"target\":6}");
        assertThat(first.hints()).hasSize(3);
    }

    @Test
    void rejectsTheInvalidFixture() {
        assertThatThrownBy(() -> parse(Fixtures.text("llm/observations-invalid.json")))
                .isInstanceOf(ProposalParser.InvalidResponseException.class);
    }

    @Test
    void readsTheEvidenceAsAJsonStringFromStructuredOutput() {
        List<Proposal> proposals = parse("""
                {"observations":[{"type":"correctness","line":2,"evidenceInputJson":"{\\"nums\\":[1,2],\\"target\\":3}",
                "risk":"r","hints":["a","b","c"]}]}""");

        assertThat(proposals.getFirst().evidenceInput().get("target").asInt()).isEqualTo(3);
    }

    @Test
    void efficiencyHasNoEvidenceInput() {
        List<Proposal> proposals = parse("""
                {"observations":[{"type":"efficiency","line":3,"evidenceInputJson":"","risk":"r","hints":["a","b","c"]}]}""");

        assertThat(proposals.getFirst().evidenceInput()).isNull();
        assertThat(proposals.getFirst().isEfficiency()).isTrue();
    }

    @Test
    void unparseableEvidenceIsKeptForTheVerifierToReject() {
        List<Proposal> proposals = parse("""
                {"observations":[{"type":"edge_case","line":1,"evidenceInputJson":"nums=[3,2,4]","risk":"r",
                "hints":["a","b","c"]}]}""");

        assertThat(proposals.getFirst().evidenceInput().isObject()).isFalse();
    }

    @Test
    void acceptsCodeFencesAndKeepsAtMostThree() {
        String item = "{\"type\":\"efficiency\",\"line\":1,\"evidenceInputJson\":\"\",\"risk\":\"r\",\"hints\":[\"a\",\"b\",\"c\"]}";
        String text = "```json\n{\"observations\":[" + String.join(",", item, item, item, item) + "]}\n```";

        assertThat(parse(text)).hasSize(3);
    }

    @Test
    void emptyListIsValid() {
        assertThat(parse("{\"observations\":[]}")).isEmpty();
    }

    @Test
    void notJsonIsInvalid() {
        assertThatThrownBy(() -> parse("Iată observațiile mele: ...")).isInstanceOf(ProposalParser.InvalidResponseException.class);
        assertThatThrownBy(() -> parse("{\"obs\":[]}")).isInstanceOf(ProposalParser.InvalidResponseException.class);
    }
}
