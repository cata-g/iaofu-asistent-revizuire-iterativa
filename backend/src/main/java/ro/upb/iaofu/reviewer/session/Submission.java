package ro.upb.iaofu.reviewer.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One version of a session (F8); JSON columns hold the contract shapes as text. */
@Entity
@Table(name = "submission")
public class Submission {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(name = "report_json", nullable = false, columnDefinition = "TEXT")
    private String reportJson;

    @Column(name = "classification_json", columnDefinition = "TEXT")
    private String classificationJson;

    @Column(nullable = false)
    private int passed;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private String efficiency;

    private Double slope;

    @Column(name = "assistant_message", columnDefinition = "TEXT")
    private String assistantMessage;

    @Column(name = "llm_status", nullable = false)
    private String llmStatus;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Submission() {
    }

    public Submission(UUID sessionId, int version, String code) {
        this.id = UUID.randomUUID();
        this.sessionId = sessionId;
        this.version = version;
        this.code = code;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getVersion() {
        return version;
    }

    public String getCode() {
        return code;
    }

    public String getReportJson() {
        return reportJson;
    }

    public String getClassificationJson() {
        return classificationJson;
    }

    public int getPassed() {
        return passed;
    }

    public int getTotal() {
        return total;
    }

    public String getEfficiency() {
        return efficiency;
    }

    public Double getSlope() {
        return slope;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public String getLlmStatus() {
        return llmStatus;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setReport(String reportJson, int passed, int total, String efficiency, Double slope) {
        this.reportJson = reportJson;
        this.passed = passed;
        this.total = total;
        this.efficiency = efficiency;
        this.slope = slope;
    }

    public void setClassificationJson(String classificationJson) {
        this.classificationJson = classificationJson;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public void setLlmStatus(String llmStatus) {
        this.llmStatus = llmStatus;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
