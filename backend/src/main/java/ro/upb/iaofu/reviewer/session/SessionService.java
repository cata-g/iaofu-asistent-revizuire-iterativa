package ro.upb.iaofu.reviewer.session;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upb.iaofu.reviewer.common.ConflictException;
import ro.upb.iaofu.reviewer.common.NotFoundException;
import ro.upb.iaofu.reviewer.execution.ExecutionService;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.observation.ObservationService;
import ro.upb.iaofu.reviewer.observation.ObservationView;
import ro.upb.iaofu.reviewer.problem.Problem;
import ro.upb.iaofu.reviewer.problem.ProblemRepository;
import ro.upb.iaofu.reviewer.version.Classification;
import ro.upb.iaofu.reviewer.version.VersionClassifier;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SessionService {

    static final String LLM_NOT_USED = "not_used";

    private final SessionRepository sessions;
    private final SubmissionRepository submissions;
    private final ProblemRepository problems;
    private final ExecutionService execution;
    private final ObservationService observations;
    private final JsonMapper json;

    public SessionService(SessionRepository sessions, SubmissionRepository submissions, ProblemRepository problems,
                          ExecutionService execution, ObservationService observations, JsonMapper json) {
        this.sessions = sessions;
        this.submissions = submissions;
        this.problems = problems;
        this.execution = execution;
        this.observations = observations;
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
        Optional<Submission> previous = submissions.findTopBySessionIdOrderByVersionDesc(sessionId);
        int version = previous.map(s -> s.getVersion() + 1).orElse(1);

        TestReport report = execution.evaluate(problem, code);
        // Stored for every mode so the analysis can compare variants; shown only in the proposed one.
        Classification classification = previous
                .map(p -> VersionClassifier.classify(p.getVersion(), report(p), report))
                .orElse(null);

        Submission submission = new Submission(sessionId, version, code);
        submission.setReport(json.writeValueAsString(report), report.summary().passed(), report.summary().total(),
                report.efficiency(), report.slope());
        submission.setClassificationJson(classification == null ? null : json.writeValueAsString(classification));
        submission.setLlmStatus(LLM_NOT_USED);
        submission.setDurationMs(System.currentTimeMillis() - start);
        submissions.save(submission);

        return new SessionDtos.SubmissionResponse(submission.getId(), version, report,
                visibleClassification(session, classification), List.of(), null, LLM_NOT_USED);
    }

    @Transactional
    public void finish(UUID sessionId) {
        session(sessionId).finish();
    }

    @Transactional(readOnly = true)
    public SessionDtos.HistoryResponse history(UUID sessionId) {
        Session session = session(sessionId);
        List<Submission> versions = submissions.findBySessionIdOrderByVersionAsc(sessionId);
        Map<UUID, List<ObservationView>> views = observations.viewsBySubmission(
                versions.stream().map(Submission::getId).toList());
        List<SessionDtos.VersionView> items = versions.stream().map(s -> {
            TestReport report = report(s);
            Classification classification = s.getClassificationJson() == null ? null
                    : json.readValue(s.getClassificationJson(), Classification.class);
            return new SessionDtos.VersionView(s.getVersion(), s.getId(), s.getCode(), s.getCreatedAt(),
                    report.summary(), report.efficiency(), report, visibleClassification(session, classification),
                    views.getOrDefault(s.getId(), List.of()), s.getAssistantMessage());
        }).toList();
        return new SessionDtos.HistoryResponse(sessionId, session.getProblemId(), session.isFinished(), items);
    }

    public Session session(UUID sessionId) {
        return sessions.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session " + sessionId + " not found"));
    }

    private static Classification visibleClassification(Session session, Classification classification) {
        return session.getMode() == Mode.PROPOSED ? classification : null;
    }

    private TestReport report(Submission submission) {
        return json.readValue(submission.getReportJson(), TestReport.class);
    }

    private Problem problem(String problemId) {
        return problems.findById(problemId)
                .orElseThrow(() -> new NotFoundException("Problem " + problemId + " not found"));
    }
}
