package ro.upb.iaofu.reviewer.observation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    List<Observation> findBySubmissionIdInAndShownTrueOrderByCreatedAtAsc(Collection<UUID> submissionIds);
}
