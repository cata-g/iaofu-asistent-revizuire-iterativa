package ro.upb.iaofu.reviewer.session;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upb.iaofu.reviewer.common.ConflictException;
import ro.upb.iaofu.reviewer.common.NotFoundException;
import ro.upb.iaofu.reviewer.execution.ExecutionService;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.problem.Problem;
import ro.upb.iaofu.reviewer.problem.ProblemRepository;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SessionService {

    static final String LLM_NOT_USED = "not_used";

    private final SessionRepository sessions;
    private final SubmissionRepository submissions;
    private final ProblemRepository problems;
    private final ExecutionService execution;
    private final JsonMapper json;

    public SessionService(SessionRepository sessions, SubmissionRepository submissions, ProblemRepository problems,
                          ExecutionService execution, JsonMapper json) {
        this.sessions = sessions;
        this.submissions = submissions;
        this.problems = problems;
        this.execution = execution;
        this.json = json;
    }

    @Transactional
    public Session create(String problemId, Mode mode, String participantId) {
        problem(problemId);
        return sessions.save(new Session(problemId, mode == null ? Mode.PROPOSED : mode, participantId));
    }

    @Transactional
    public SessionDtos.SubmissionResponse submit(UUID sessionId, String code) {
        long start = System.currentTimeMillis();
        Session session = session(sessionId);
        if (session.isFinished()) {
            throw new ConflictException("Session " + sessionId + " is finished");
        }
        Problem problem = problem(session.getProblemId());
        int version = submissions.findTopBySessionIdOrderByVersionDesc(sessionId)
                .map(s -> s.getVersion() + 1).orElse(1);

        TestReport report = execution.evaluate(problem, code);

        Submission submission = new Submission(sessionId, version, code);
        submission.setReport(json.writeValueAsString(report), report.summary().passed(), report.summary().total(),
                report.efficiency(), report.slope());
        submission.setLlmStatus(LLM_NOT_USED);
        submission.setDurationMs(System.currentTimeMillis() - start);
        submissions.save(submission);

        return new SessionDtos.SubmissionResponse(submission.getId(), version, report, null, List.of(), null,
                LLM_NOT_USED);
    }

    @Transactional
    public void finish(UUID sessionId) {
        session(sessionId).finish();
    }

    public Session session(UUID sessionId) {
        return sessions.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session " + sessionId + " not found"));
    }

    private Problem problem(String problemId) {
        return problems.findById(problemId)
                .orElseThrow(() -> new NotFoundException("Problem " + problemId + " not found"));
    }
}
