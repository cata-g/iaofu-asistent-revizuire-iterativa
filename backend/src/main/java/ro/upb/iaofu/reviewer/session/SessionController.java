package ro.upb.iaofu.reviewer.session;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping
    public SessionDtos.CreateSessionResponse create(@Valid @RequestBody SessionDtos.CreateSessionRequest request) {
        Session session = sessions.create(request.problemId(), request.mode(), request.participantId());
        return new SessionDtos.CreateSessionResponse(session.getId(), session.getProblemId());
    }

    @PostMapping("/{id}/submissions")
    public SessionDtos.SubmissionResponse submit(@PathVariable UUID id,
                                                 @Valid @RequestBody SessionDtos.SubmitRequest request) {
        return sessions.submit(id, request.code());
    }

    @PostMapping("/{id}/finish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finish(@PathVariable UUID id) {
        sessions.finish(id);
    }
}
