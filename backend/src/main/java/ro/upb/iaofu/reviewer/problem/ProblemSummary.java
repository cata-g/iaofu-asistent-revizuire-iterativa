package ro.upb.iaofu.reviewer.problem;

/** Item of GET /api/problems. */
public record ProblemSummary(String id, String title, String category, String difficulty) {

    static ProblemSummary of(Problem p) {
        return new ProblemSummary(p.id(), p.title(), p.category(), p.difficulty());
    }
}
