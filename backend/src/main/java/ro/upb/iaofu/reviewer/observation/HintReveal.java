package ro.upb.iaofu.reviewer.observation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A hint level unlocked by the student, with the moment it was unlocked (F19). */
@Entity
@Table(name = "hint_reveal")
public class HintReveal {

    @Id
    private UUID id;

    @Column(name = "observation_id", nullable = false)
    private UUID observationId;

    @Column(nullable = false)
    private int level;

    @Column(name = "revealed_at", nullable = false)
    private Instant revealedAt;

    protected HintReveal() {
    }

    public HintReveal(UUID observationId, int level) {
        this.id = UUID.randomUUID();
        this.observationId = observationId;
        this.level = level;
        this.revealedAt = Instant.now();
    }

    public UUID getObservationId() {
        return observationId;
    }

    public int getLevel() {
        return level;
    }

    public Instant getRevealedAt() {
        return revealedAt;
    }
}
