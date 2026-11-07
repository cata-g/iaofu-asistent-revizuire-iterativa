package ro.upb.iaofu.reviewer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import ro.upb.iaofu.reviewer.problem.Problem;
import ro.upb.iaofu.reviewer.problem.ProblemRepository;
import tools.jackson.databind.json.JsonMapper;

/** Reads the shared files in ../fixtures and ../problems (relative to backend/). */
public final class Fixtures {

    public static final JsonMapper JSON = JsonMapper.builder().build();
    public static final Path FIXTURES = Path.of(System.getenv().getOrDefault("FIXTURES_DIR", "../fixtures"));
    public static final Path PROBLEMS = Path.of(System.getenv().getOrDefault("PROBLEMS_DIR", "../problems"));

    private Fixtures() {
    }

    public static String text(String relative) {
        try {
            return Files.readString(FIXTURES.resolve(relative));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T json(String relative, Class<T> type) {
        return JSON.readValue(text(relative), type);
    }

    public static ProblemRepository problems() {
        return new ProblemRepository(PROBLEMS, JSON);
    }

    public static Problem twoSum() {
        return problems().findById("two-sum").orElseThrow();
    }
}
