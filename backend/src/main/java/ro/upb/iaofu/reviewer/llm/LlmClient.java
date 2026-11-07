package ro.upb.iaofu.reviewer.llm;

import java.util.Map;

/** One text-in, text-out call to the model. Implementations do not log or check the budget; {@link LlmGateway} does. */
public interface LlmClient {

    /**
     * @param jsonSchema when not null, the response is constrained to this JSON schema (structured output)
     */
    record Request(String system, String user, Map<String, Object> jsonSchema, long maxTokens) {
    }

    record Reply(String text, long inputTokens, long outputTokens, String stopReason) {
    }

    Reply complete(Request request);

    String model();

    /** False when no API key is configured. */
    boolean available();
}
