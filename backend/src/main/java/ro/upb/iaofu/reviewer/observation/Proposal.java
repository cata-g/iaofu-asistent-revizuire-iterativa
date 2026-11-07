package ro.upb.iaofu.reviewer.observation;

import java.util.List;
import tools.jackson.databind.JsonNode;

/** One observation proposed by the LLM, before verification (docs/contracte.md section 5). */
public record Proposal(String type, Integer line, JsonNode evidenceInput, String risk, List<String> hints) {

    public static final List<String> TYPES = List.of("correctness", "edge_case", "efficiency");

    public boolean isEfficiency() {
        return "efficiency".equals(type);
    }
}
