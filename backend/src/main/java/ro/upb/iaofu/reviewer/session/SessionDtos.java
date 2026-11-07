package ro.upb.iaofu.reviewer.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.observation.ObservationView;
import ro.upb.iaofu.reviewer.version.Classification;

/** Request and response bodies of /api/sessions (docs/contracte.md section 6). */
public final class SessionDtos {

    private SessionDtos() {
    }

    public record CreateSessionRequest(@NotBlank String problemId, Mode mode, @Size(max = 50) String participantId) {
    }

    /** Deliberately without the mode (F18). */
    public record CreateSessionResponse(UUID sessionId, String problemId) {
    }

    public record SubmitRequest(@NotBlank @Size(max = 20_000) String code) {
    }

    public record SubmissionResponse(
            UUID submissionId,
            int version,
            TestReport report,
            Classification classification,
            List<ObservationView> observations,
            String assistantMessage,
            String llmStatus) {
    }

    public record VersionView(
            int version,
            UUID submissionId,
            String code,
            Instant createdAt,
            TestReport.Summary summary,
            String efficiency,
            TestReport report,
            Classification classification,
            List<ObservationView> observations,
            String assistantMessage) {
    }

    public record HistoryResponse(UUID sessionId, String problemId, boolean finished, List<VersionView> versions) {
    }
}
