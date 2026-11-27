package ro.upb.iaofu.reviewer.session;

import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.upb.iaofu.reviewer.common.ConflictException;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessions;
    /** Sessions with a submission running; a second one (e.g. a double click) would get the same version. */
    private final Set<UUID> running = ConcurrentHashMap.newKeySet();

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
        if (!running.add(id)) {
            throw new ConflictException("A submission for session " + id + " is already running");
        }
        try {
            return sessions.submit(id, request.code());
        } finally {
            running.remove(id);
        }
    }

    @GetMapping("/{id}/history")
    public SessionDtos.HistoryResponse history(@PathVariable UUID id) {
        return sessions.history(id);
    }

    @PostMapping("/{id}/finish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finish(@PathVariable UUID id) {
        sessions.finish(id);
    }
}
