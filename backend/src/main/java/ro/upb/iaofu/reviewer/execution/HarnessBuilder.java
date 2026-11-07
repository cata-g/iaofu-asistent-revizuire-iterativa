package ro.upb.iaofu.reviewer.execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;
import ro.upb.iaofu.reviewer.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Builds the files and the stdin job for harness.py. */
@Component
public class HarnessBuilder {

    static final int TEST_TIMEOUT_MS = 2000;
    static final int LARGE_REPEATS = 3;

    private final JsonMapper json;
    private final String harnessSource;

    public HarnessBuilder(JsonMapper json) {
        this.json = json;
        try (InputStream in = HarnessBuilder.class.getResourceAsStream("/harness/harness.py")) {
            if (in == null) {
                throw new IllegalStateException("harness/harness.py missing from classpath");
            }
            this.harnessSource = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<PistonClient.File> files(Problem problem, String solutionCode) {
        return List.of(
                new PistonClient.File("harness.py", harnessSource),
                new PistonClient.File("solution.py", solutionCode),
                new PistonClient.File("inputs.py", problem.inputsCode()));
    }

    public ObjectNode evaluateJob(Problem problem) {
        ObjectNode job = base(problem, "evaluate");
        ArrayNode tests = job.putArray("tests");
        for (Problem.TestCase t : problem.tests()) {
            ObjectNode test = tests.addObject().put("id", t.id()).put("group", t.group());
            test.set("input", t.input());
            test.set("expected", t.expected());
        }
        ObjectNode large = job.putObject("large");
        ArrayNode sizes = large.putArray("sizes");
        problem.large().sizes().forEach(sizes::add);
        large.put("seed", problem.large().seed())
                .put("timeLimitMs", problem.timeLimitMs())
                .put("repeats", LARGE_REPEATS);
        return job;
    }

    public ObjectNode singleJob(Problem problem, JsonNode input) {
        ObjectNode job = base(problem, "single");
        job.set("input", input);
        return job;
    }

    public ObjectNode validateJob(JsonNode input) {
        ObjectNode job = json.createObjectNode().put("mode", "validate");
        job.set("input", input);
        return job;
    }

    /** Upper bound for the whole Piston run; the harness enforces the per-call limits. */
    public long evaluateTimeoutMs(Problem problem) {
        long tests = (long) problem.tests().size() * TEST_TIMEOUT_MS;
        long large = (long) problem.large().sizes().size() * LARGE_REPEATS * problem.timeLimitMs();
        return Math.min(25_000, tests + large + 3_000);
    }

    private ObjectNode base(Problem problem, String mode) {
        ObjectNode job = json.createObjectNode()
                .put("mode", mode)
                .put("function", problem.signature().function())
                .put("comparator", problem.comparator())
                .put("testTimeoutMs", TEST_TIMEOUT_MS);
        ArrayNode params = job.putArray("params");
        problem.signature().paramNames().forEach(params::add);
        return job;
    }
}
