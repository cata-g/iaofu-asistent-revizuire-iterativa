package ro.upb.iaofu.reviewer.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** System prompts kept as files in resources/prompts, so they can be tuned without touching code. */
public final class Prompts {

    public static final String OBSERVATIONS = load("observations-system.md");
    public static final String HINT_REWRITE = load("hint-rewrite-system.md");
    public static final String BASELINE_B = load("baseline-b-system.md");

    private Prompts() {
    }

    private static String load(String name) {
        try (InputStream in = Prompts.class.getResourceAsStream("/prompts/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Missing prompt " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
