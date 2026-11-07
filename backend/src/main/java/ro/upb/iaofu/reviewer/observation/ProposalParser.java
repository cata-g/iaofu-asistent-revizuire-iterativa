package ro.upb.iaofu.reviewer.observation;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses and validates the generator's JSON (docs/contracte.md section 5). Any shape error
 * makes the whole response invalid, which triggers the single retry from N3.
 */
public final class ProposalParser {

    public static final int MAX_OBSERVATIONS = 3;

    private ProposalParser() {
    }

    public static class InvalidResponseException extends RuntimeException {

        InvalidResponseException(String message) {
            super(message);
        }
    }

    public static List<Proposal> parse(String text, JsonMapper json) {
        JsonNode root;
        try {
            root = json.readTree(stripFences(text));
        } catch (JacksonException e) {
            throw new InvalidResponseException("Not JSON: " + e.getOriginalMessage());
        }
        JsonNode items = root.get("observations");
        if (items == null || !items.isArray()) {
            throw new InvalidResponseException("Missing 'observations' array");
        }
        List<Proposal> proposals = new ArrayList<>();
        for (JsonNode item : items) {
            proposals.add(parseItem(item, json));
        }
        return proposals.size() > MAX_OBSERVATIONS ? proposals.subList(0, MAX_OBSERVATIONS) : proposals;
    }

    private static Proposal parseItem(JsonNode item, JsonMapper json) {
        String type = text(item, "type");
        if (!Proposal.TYPES.contains(type)) {
            throw new InvalidResponseException("Unknown type '" + type + "'");
        }
        JsonNode line = item.get("line");
        if (line != null && !line.isNull() && !line.isIntegralNumber()) {
            throw new InvalidResponseException("'line' must be an integer");
        }
        JsonNode hints = item.get("hints");
        if (hints == null || !hints.isArray() || hints.size() != 3) {
            throw new InvalidResponseException("'hints' must have exactly 3 levels");
        }
        List<String> hintTexts = new ArrayList<>();
        for (JsonNode h : hints) {
            if (!h.isString() || h.asString().isBlank()) {
                throw new InvalidResponseException("Every hint must be a non-empty string");
            }
            hintTexts.add(h.asString().strip());
        }
        return new Proposal(type, line == null || line.isNull() ? null : line.asInt(),
                evidence(item, type, json), text(item, "risk"), hintTexts);
    }

    /**
     * The model sends the input as a JSON string (evidenceInputJson) because structured output needs a
     * fixed schema; fixtures and hand-written responses may use an object (evidenceInput).
     * An input that is not a JSON object is kept as-is and rejected later by the verifier.
     */
    private static JsonNode evidence(JsonNode item, String type, JsonMapper json) {
        if ("efficiency".equals(type)) {
            return null;
        }
        JsonNode object = item.get("evidenceInput");
        if (object != null && object.isObject()) {
            return object;
        }
        JsonNode raw = item.get("evidenceInputJson");
        if (raw != null && raw.isString() && !raw.asString().isBlank()) {
            try {
                return json.readTree(raw.asString());
            } catch (JacksonException e) {
                return raw;
            }
        }
        if (object != null && !object.isNull()) {
            throw new InvalidResponseException("'evidenceInput' must be an object");
        }
        return null;
    }

    private static String text(JsonNode item, String field) {
        JsonNode node = item.get(field);
        if (node == null || !node.isString() || node.asString().isBlank()) {
            throw new InvalidResponseException("Missing '" + field + "'");
        }
        return node.asString().strip();
    }

    private static String stripFences(String text) {
        String t = text.strip();
        if (t.startsWith("```")) {
            t = t.substring(t.indexOf('\n') + 1);
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
        }
        return t;
    }
}
