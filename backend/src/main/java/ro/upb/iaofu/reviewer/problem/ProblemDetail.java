package ro.upb.iaofu.reviewer.problem;

import java.util.List;

/** GET /api/problems/{id}: only the public fields (F2, F3). */
public record ProblemDetail(
        String id,
        String title,
        String category,
        String difficulty,
        String statement,
        List<Problem.Example> examples,
        List<String> constraints,
        int timeLimitMs,
        Problem.Signature signature,
        String starterCode) {

    static ProblemDetail of(Problem p) {
        return new ProblemDetail(p.id(), p.title(), p.category(), p.difficulty(), p.statement(), p.examples(),
                p.constraints(), p.timeLimitMs(), p.signature(), p.starterCode());
    }
}
