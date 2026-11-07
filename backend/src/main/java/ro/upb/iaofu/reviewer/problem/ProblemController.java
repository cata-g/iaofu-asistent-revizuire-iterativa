package ro.upb.iaofu.reviewer.problem;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.upb.iaofu.reviewer.common.NotFoundException;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemRepository problems;

    public ProblemController(ProblemRepository problems) {
        this.problems = problems;
    }

    @GetMapping
    public List<ProblemSummary> list(@RequestParam(required = false) String category,
                                     @RequestParam(required = false) String difficulty) {
        return problems.findAll().stream()
                .filter(p -> category == null || category.isBlank() || p.category().equals(category))
                .filter(p -> difficulty == null || difficulty.isBlank() || p.difficulty().equals(difficulty))
                .map(ProblemSummary::of)
                .toList();
    }

    @GetMapping("/{id}")
    public ProblemDetail get(@PathVariable String id) {
        return problems.findById(id).map(ProblemDetail::of)
                .orElseThrow(() -> new NotFoundException("Problem " + id + " not found"));
    }
}
