package ro.upb.iaofu.reviewer.llm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Every model call with its tokens and cost (N4); the budget is the sum of cost_usd. */
@Entity
@Table(name = "llm_call")
public class LlmCall {

    @Id
    private UUID id;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "cost_usd", nullable = false)
    private BigDecimal costUsd;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LlmCall() {
    }

    public LlmCall(UUID submissionId, String purpose, String model, long inputTokens, long outputTokens,
                   BigDecimal costUsd, long latencyMs, boolean success, String error) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.purpose = purpose;
        this.model = model;
        this.inputTokens = (int) inputTokens;
        this.outputTokens = (int) outputTokens;
        this.costUsd = costUsd;
        this.latencyMs = latencyMs;
        this.success = success;
        this.error = error;
        this.createdAt = Instant.now();
    }

    public String getPurpose() {
        return purpose;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }
}
