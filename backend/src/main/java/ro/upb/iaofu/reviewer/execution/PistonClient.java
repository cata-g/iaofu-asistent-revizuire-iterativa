package ro.upb.iaofu.reviewer.execution;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Thin client for Piston's POST /api/v2/execute. */
@Component
public class PistonClient {

    static final String LANGUAGE = "python";
    static final String VERSION = "3.12.0";
    private static final long MEMORY_LIMIT_BYTES = 512L * 1024 * 1024;

    private final RestClient http;

    public PistonClient(@Value("${reviewer.piston.url}") String baseUrl) {
        // Piston rejects the HTTP/2 upgrade headers the JDK client sends by default.
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public record File(String name, String content) {
    }

    record ExecuteRequest(String language, String version, List<File> files, String stdin,
                          long run_timeout, long run_memory_limit) {
    }

    public record Run(String stdout, String stderr, Integer code, String signal, String status, String message) {
    }

    record ExecuteResponse(Run run, String message) {
    }

    /** Runs the first file as the entry point; stdin is passed as-is. */
    public Run execute(List<File> files, String stdin, long timeoutMs) {
        try {
            ExecuteResponse response = http.post().uri("/api/v2/execute")
                    .body(new ExecuteRequest(LANGUAGE, VERSION, files, stdin, timeoutMs, MEMORY_LIMIT_BYTES))
                    .retrieve()
                    .body(ExecuteResponse.class);
            if (response == null || response.run() == null) {
                throw new ExecutionException("Piston returned no run result"
                        + (response != null && response.message() != null ? ": " + response.message() : ""));
            }
            return response.run();
        } catch (RestClientException e) {
            throw new ExecutionException("Piston request failed", e);
        }
    }
}
