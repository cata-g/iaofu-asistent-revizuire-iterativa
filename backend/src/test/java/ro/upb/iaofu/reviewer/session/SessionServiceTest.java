package ro.upb.iaofu.reviewer.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ro.upb.iaofu.reviewer.Fixtures;
import ro.upb.iaofu.reviewer.common.ConflictException;
import ro.upb.iaofu.reviewer.common.NotFoundException;
import ro.upb.iaofu.reviewer.execution.ExecutionService;
import ro.upb.iaofu.reviewer.execution.TestReport;

class SessionServiceTest {

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final SubmissionRepository submissions = mock(SubmissionRepository.class);
    private final ExecutionService execution = mock(ExecutionService.class);
    private final SessionService service = new SessionService(sessions, submissions, Fixtures.problems(), execution,
            Fixtures.JSON);
    private final TestReport report = Fixtures.json("two-sum/report-v2.json", TestReport.class);

    private Session existing(Mode mode) {
        Session s = new Session("two-sum", mode, "P1");
        when(sessions.findById(s.getId())).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void createDefaultsToProposed() {
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Session s = service.create("two-sum", null, null);

        assertThat(s.getMode()).isEqualTo(Mode.PROPOSED);
    }

    @Test
    void createRejectsUnknownProblem() {
        assertThatThrownBy(() -> service.create("missing", Mode.BASELINE_A, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void submissionsGetIncreasingVersions() {
        Session s = existing(Mode.BASELINE_A);
        Submission previous = new Submission(s.getId(), 1, "v1");
        when(submissions.findTopBySessionIdOrderByVersionDesc(s.getId())).thenReturn(Optional.of(previous));
        when(execution.evaluate(any(), anyString())).thenReturn(report);

        SessionDtos.SubmissionResponse response = service.submit(s.getId(), "v2");

        assertThat(response.version()).isEqualTo(2);
        assertThat(response.report()).isEqualTo(report);
        ArgumentCaptor<Submission> saved = ArgumentCaptor.forClass(Submission.class);
        verify(submissions).save(saved.capture());
        assertThat(saved.getValue().getPassed()).isEqualTo(11);
        assertThat(saved.getValue().getEfficiency()).isEqualTo("ok");
        assertThat(saved.getValue().getReportJson()).contains("edge-half-target-once");
    }

    @Test
    void finishedSessionRejectsSubmissions() {
        Session s = existing(Mode.PROPOSED);
        s.finish();

        assertThatThrownBy(() -> service.submit(s.getId(), "code")).isInstanceOf(ConflictException.class);
        verify(execution, never()).evaluate(any(), anyString());
    }

    @Test
    void unknownSessionIsNotFound() {
        assertThatThrownBy(() -> service.submit(UUID.randomUUID(), "code")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void modeParsesWireValues() {
        assertThat(Mode.fromWire("baseline_b")).isEqualTo(Mode.BASELINE_B);
        assertThatThrownBy(() -> Mode.fromWire("baseline_c")).isInstanceOf(IllegalArgumentException.class);
    }
}
