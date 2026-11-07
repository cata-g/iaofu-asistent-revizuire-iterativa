package ro.upb.iaofu.reviewer.problem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Loads every problems/&lt;id&gt;/ directory once at startup. */
@Repository
public class ProblemRepository {

    private static final Logger log = LoggerFactory.getLogger(ProblemRepository.class);

    private final Map<String, Problem> problems = new LinkedHashMap<>();

    public ProblemRepository(@Value("${reviewer.problems.dir}") Path dir, JsonMapper json) {
        if (!Files.isDirectory(dir)) {
            log.warn("Problems directory {} does not exist; no problems loaded", dir.toAbsolutePath());
            return;
        }
        try (Stream<Path> children = Files.list(dir)) {
            children.filter(p -> Files.exists(p.resolve("problem.json")))
                    .sorted(Comparator.naturalOrder())
                    .map(p -> load(p, json))
                    .forEach(p -> problems.put(p.id(), p));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.info("Loaded {} problem(s) from {}", problems.size(), dir.toAbsolutePath());
    }

    private static Problem load(Path dir, JsonMapper json) {
        try {
            Problem p = json.readValue(dir.resolve("problem.json").toFile(), Problem.class);
            return p.withSources(Files.readString(dir.resolve("reference.py")),
                    Files.readString(dir.resolve("inputs.py")));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load problem " + dir, e);
        }
    }

    public Collection<Problem> findAll() {
        return problems.values();
    }

    public Optional<Problem> findById(String id) {
        return Optional.ofNullable(problems.get(id));
    }
}
