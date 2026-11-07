package ro.upb.iaofu.reviewer.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findBySessionIdOrderByVersionAsc(UUID sessionId);

    Optional<Submission> findTopBySessionIdOrderByVersionDesc(UUID sessionId);
}
