package ro.upb.iaofu.reviewer.observation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/observations")
public class ObservationController {

    private final ObservationService observations;

    public ObservationController(ObservationService observations) {
        this.observations = observations;
    }

    public record FeedbackRequest(@NotBlank String verdict) {
    }

    @PostMapping("/{id}/next-hint")
    public ObservationService.HintResponse nextHint(@PathVariable UUID id) {
        return observations.nextHint(id);
    }

    @PostMapping("/{id}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void feedback(@PathVariable UUID id, @Valid @RequestBody FeedbackRequest request) {
        observations.feedback(id, request.verdict());
    }
}
