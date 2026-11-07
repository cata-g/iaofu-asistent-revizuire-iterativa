package ro.upb.iaofu.reviewer.observation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** An LLM hypothesis and its verification outcome (docs/contracte.md section 4). */
@Entity
@Table(name = "observation")
public class Observation {

    public static final String CONFIRMED = "confirmed";
    public static final String REJECTED = "rejected";

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(nullable = false)
    private String type;

    private Integer line;

    @Column(name = "evidence_input_json", columnDefinition = "TEXT")
    private String evidenceInputJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String risk;

    @Column(name = "hints_json", nullable = false, columnDefinition = "TEXT")
    private String hintsJson;

    @Column(nullable = false)
    private String status;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "student_output_json", columnDefinition = "TEXT")
    private String studentOutputJson;

    @Column(name = "expected_output_json", columnDefinition = "TEXT")
    private String expectedOutputJson;

    @Column(name = "leak_retries", nullable = false)
    private int leakRetries;

    @Column(nullable = false)
    private boolean shown;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Observation() {
    }

    public Observation(UUID submissionId, String type, Integer line, String evidenceInputJson, String risk,
                       String hintsJson) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.type = type;
        this.line = line;
        this.evidenceInputJson = evidenceInputJson;
        this.risk = risk;
        this.hintsJson = hintsJson;
        this.status = CONFIRMED;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getType() {
        return type;
    }

    public Integer getLine() {
        return line;
    }

    public String getEvidenceInputJson() {
        return evidenceInputJson;
    }

    public String getRisk() {
        return risk;
    }

    public String getHintsJson() {
        return hintsJson;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public String getStudentOutputJson() {
        return studentOutputJson;
    }

    public String getExpectedOutputJson() {
        return expectedOutputJson;
    }

    public int getLeakRetries() {
        return leakRetries;
    }

    public boolean isShown() {
        return shown;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isConfirmed() {
        return CONFIRMED.equals(status);
    }

    public void reject(String reason) {
        this.status = REJECTED;
        this.rejectReason = reason;
    }

    public void setOutputs(String studentOutputJson, String expectedOutputJson) {
        this.studentOutputJson = studentOutputJson;
        this.expectedOutputJson = expectedOutputJson;
    }

    public void setHintsJson(String hintsJson) {
        this.hintsJson = hintsJson;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public void setLeakRetries(int leakRetries) {
        this.leakRetries = leakRetries;
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }
}
