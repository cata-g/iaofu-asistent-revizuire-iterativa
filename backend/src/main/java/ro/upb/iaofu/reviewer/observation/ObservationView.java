package ro.upb.iaofu.reviewer.observation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** What the frontend receives for a confirmed observation; only unlocked hint levels are included (F15). */
public record ObservationView(
        UUID id,
        String type,
        Integer line,
        Evidence evidence,
        String risk,
        List<Hint> hints,
        int levelsAvailable,
        String verdict) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Evidence(JsonNode input, JsonNode actual, JsonNode expected, String description) {
    }

    public record Hint(int level, String text) {
    }
}
