package ro.upb.iaofu.reviewer.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session")
public class Session {

    @Id
    private UUID id;

    @Column(name = "problem_id", nullable = false)
    private String problemId;

    @Column(nullable = false)
    private Mode mode;

    @Column(name = "participant_id")
    private String participantId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected Session() {
    }

    public Session(String problemId, Mode mode, String participantId) {
        this.id = UUID.randomUUID();
        this.problemId = problemId;
        this.mode = mode;
        this.participantId = participantId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getProblemId() {
        return problemId;
    }

    public Mode getMode() {
        return mode;
    }

    public String getParticipantId() {
        return participantId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public void finish() {
        if (finishedAt == null) {
            finishedAt = Instant.now();
        }
    }
}
