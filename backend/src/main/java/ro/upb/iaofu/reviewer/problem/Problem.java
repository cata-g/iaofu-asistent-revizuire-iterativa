package ro.upb.iaofu.reviewer.problem;

import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * A problem as described in docs/contracte.md section 1, plus the contents of
 * reference.py and inputs.py. Never serialize this to the frontend; use
 * {@link ProblemDetail} or {@link ProblemSummary}.
 */
public record Problem(
        String id,
        String title,
        String category,
        String difficulty,
        String statement,
        List<Example> examples,
        Signature signature,
        List<String> constraints,
        int timeLimitMs,
        String comparator,
        List<ForbiddenTerm> forbiddenTerms,
        List<TestCase> tests,
        LargeSpec large,
        String referenceCode,
        String inputsCode) {

    public record Example(JsonNode input, JsonNode output, String explanation) {
    }

    public record Signature(String function, List<Param> params, String returns) {

        public List<String> paramNames() {
            return params.stream().map(Param::name).toList();
        }
    }

    public record Param(String name, String type) {
    }

    public record ForbiddenTerm(String term, String allowedIfCode) {
    }

    public record TestCase(String id, String group, JsonNode input, JsonNode expected) {
    }

    public record LargeSpec(List<Integer> sizes, int seed) {
    }

    Problem withSources(String referenceCode, String inputsCode) {
        return new Problem(id, title, category, difficulty, statement, examples, signature, constraints,
                timeLimitMs, comparator, forbiddenTerms, tests, large, referenceCode, inputsCode);
    }

    public String starterCode() {
        String params = String.join(", ", signature.params().stream()
                .map(p -> p.name() + ": " + p.type()).toList());
        return "def " + signature.function() + "(" + params + ") -> " + signature.returns() + ":\n    pass\n";
    }

    public TestCase test(String testId) {
        return tests.stream().filter(t -> t.id().equals(testId)).findFirst().orElse(null);
    }
}
