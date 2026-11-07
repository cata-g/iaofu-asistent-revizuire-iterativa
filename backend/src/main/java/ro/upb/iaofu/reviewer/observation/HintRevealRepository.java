package ro.upb.iaofu.reviewer.observation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HintRevealRepository extends JpaRepository<HintReveal, UUID> {

    List<HintReveal> findByObservationIdIn(Collection<UUID> observationIds);

    Optional<HintReveal> findTopByObservationIdOrderByLevelDesc(UUID observationId);
}
