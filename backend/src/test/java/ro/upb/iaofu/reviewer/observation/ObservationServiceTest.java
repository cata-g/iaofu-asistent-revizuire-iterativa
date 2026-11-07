package ro.upb.iaofu.reviewer.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ro.upb.iaofu.reviewer.common.ConflictException;
import ro.upb.iaofu.reviewer.common.NotFoundException;
import ro.upb.iaofu.reviewer.session.Mode;
import ro.upb.iaofu.reviewer.session.Session;
import ro.upb.iaofu.reviewer.session.SessionRepository;
import ro.upb.iaofu.reviewer.session.Submission;
import ro.upb.iaofu.reviewer.session.SubmissionRepository;

/** Runs against H2 so the hint log is checked through the real schema. */
@SpringBootTest
@Transactional
class ObservationServiceTest {

    @Autowired
    ObservationService service;
    @Autowired
    ObservationRepository observations;
    @Autowired
    HintRevealRepository reveals;
    @Autowired
    SessionRepository sessions;
    @Autowired
    SubmissionRepository submissions;

    private Submission submission;

    @BeforeEach
    void setUp() {
        Session session = sessions.save(new Session("two-sum", Mode.PROPOSED, null));
        submission = new Submission(session.getId(), 1, "code");
        submission.setReport("{}", 0, 0, "ok", null);
        submission.setLlmStatus("ok");
        submissions.save(submission);
    }

    private Observation observation(boolean shown) {
        Observation o = new Observation(submission.getId(), "edge_case", 4, "{\"nums\":[3,2,4],\"target\":6}",
                "risc", "[\"nivel 1\",\"nivel 2\",\"nivel 3\"]");
        o.setOutputs("[0,0]", "[1,2]");
        observations.save(o);
        if (shown) {
            service.show(List.of(o));
        }
        return o;
    }

    @Test
    void onlyLevelOneIsVisibleAtFirst() {
        Observation o = observation(true);

        ObservationView view = service.views(submission.getId()).getFirst();

        assertThat(view.hints()).extracting(ObservationView.Hint::text).containsExactly("nivel 1");
        assertThat(view.levelsAvailable()).isEqualTo(3);
        assertThat(view.evidence().input().toString()).isEqualTo("{\"nums\":[3,2,4],\"target\":6}");
        assertThat(view.evidence().actual().toString()).isEqualTo("[0,0]");
        assertThat(reveals.findByObservationIdIn(List.of(o.getId()))).extracting(HintReveal::getLevel)
                .containsExactly(1);
    }

    @Test
    void levelsUnlockInOrderAndAreLogged() {
        Observation o = observation(true);

        assertThat(service.nextHint(o.getId()).text()).isEqualTo("nivel 2");
        assertThat(service.nextHint(o.getId()).level()).isEqualTo(3);
        assertThatThrownBy(() -> service.nextHint(o.getId())).isInstanceOf(ConflictException.class);

        assertThat(service.views(submission.getId()).getFirst().hints()).hasSize(3);
        assertThat(reveals.findByObservationIdIn(List.of(o.getId()))).extracting(HintReveal::getLevel)
                .containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void hiddenOrRejectedObservationsAreNotReachable() {
        Observation hidden = observation(false);

        assertThatThrownBy(() -> service.nextHint(hidden.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.nextHint(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
        assertThat(service.views(submission.getId())).isEmpty();
    }

    @Test
    void latestFeedbackWins() {
        Observation o = observation(true);

        service.feedback(o.getId(), "useful");
        service.feedback(o.getId(), "false");

        assertThat(service.views(submission.getId()).getFirst().verdict()).isEqualTo("false");
        assertThatThrownBy(() -> service.feedback(o.getId(), "maybe")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void efficiencyEvidenceIsADescription() {
        Observation o = new Observation(submission.getId(), "efficiency", 3, null, "risc",
                "[\"a\",\"b\",\"c\"]");
        o.setOutputs("\"La n = 10 000 depășește 1000 ms.\"", null);
        observations.save(o);
        service.show(List.of(o));

        ObservationView.Evidence evidence = service.views(submission.getId()).getFirst().evidence();

        assertThat(evidence.description()).isEqualTo("La n = 10 000 depășește 1000 ms.");
        assertThat(evidence.input()).isNull();
    }
}
