package ro.upb.iaofu.reviewer.observation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A useful / false mark on an observation (F14); every mark is kept, the latest one counts. */
@Entity
@Table(name = "feedback")
public class Feedback {

    public static final String USEFUL = "useful";
    public static final String FALSE = "false";

    @Id
    private UUID id;

    @Column(name = "observation_id", nullable = false)
    private UUID observationId;

    @Column(nullable = false)
    private String verdict;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Feedback() {
    }

    public Feedback(UUID observationId, String verdict) {
        this.id = UUID.randomUUID();
        this.observationId = observationId;
        this.verdict = verdict;
        this.createdAt = Instant.now();
    }

    public UUID getObservationId() {
        return observationId;
    }

    public String getVerdict() {
        return verdict;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
