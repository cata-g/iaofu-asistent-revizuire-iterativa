package ro.upb.iaofu.reviewer.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import java.time.Duration;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Claude through the official Java SDK. The API key comes only from the environment. */
@Component
public class AnthropicLlmClient implements LlmClient {

    private final String model;
    private final AnthropicClient client;

    public AnthropicLlmClient(@Value("${reviewer.llm.api-key:}") String apiKey,
                              @Value("${reviewer.llm.model}") String model) {
        this.model = model;
        this.client = apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .build();
    }

    @Override
    public Reply complete(Request request) {
        if (client == null) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured");
        }
        // Low effort keeps the cost per submission small; the same settings are used by every variant.
        OutputConfig.Builder output = OutputConfig.builder().effort(OutputConfig.Effort.LOW);
        if (request.jsonSchema() != null) {
            JsonOutputFormat.Schema.Builder schema = JsonOutputFormat.Schema.builder();
            request.jsonSchema().forEach((k, v) -> schema.putAdditionalProperty(k, JsonValue.from(v)));
            output.format(JsonOutputFormat.builder().schema(schema.build()).build());
        }
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(request.maxTokens())
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(output.build())
                .system(request.system())
                .addUserMessage(request.user())
                .build();
        Message message = client.messages().create(params);
        String text = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(t -> t.text())
                .collect(Collectors.joining());
        return new Reply(text, message.usage().inputTokens(), message.usage().outputTokens(),
                message.stopReason().map(Object::toString).orElse(null));
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public boolean available() {
        return client != null;
    }
}
