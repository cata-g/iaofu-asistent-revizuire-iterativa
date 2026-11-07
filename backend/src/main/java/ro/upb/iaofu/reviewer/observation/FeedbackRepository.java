package ro.upb.iaofu.reviewer.observation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findByObservationIdInOrderByCreatedAtAsc(Collection<UUID> observationIds);
}
