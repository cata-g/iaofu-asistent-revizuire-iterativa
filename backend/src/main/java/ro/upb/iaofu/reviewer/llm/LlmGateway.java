package ro.upb.iaofu.reviewer.llm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Checks the budget, calls the model and logs every call with its cost (N4). */
@Service
public class LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(LlmGateway.class);
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private final LlmClient client;
    private final LlmCallRepository calls;
    private final BigDecimal budgetUsd;
    private final BigDecimal inputUsdPerMtok;
    private final BigDecimal outputUsdPerMtok;

    public LlmGateway(LlmClient client, LlmCallRepository calls,
                      @Value("${reviewer.llm.budget-usd}") BigDecimal budgetUsd,
                      @Value("${reviewer.llm.input-usd-per-mtok}") BigDecimal inputUsdPerMtok,
                      @Value("${reviewer.llm.output-usd-per-mtok}") BigDecimal outputUsdPerMtok) {
        this.client = client;
        this.calls = calls;
        this.budgetUsd = budgetUsd;
        this.inputUsdPerMtok = inputUsdPerMtok;
        this.outputUsdPerMtok = outputUsdPerMtok;
    }

    /** The budget is spent or no API key is configured. */
    public static class UnavailableException extends RuntimeException {

        private final boolean budget;

        UnavailableException(String message, boolean budget) {
            super(message);
            this.budget = budget;
        }

        public boolean isBudget() {
            return budget;
        }
    }

    /** The API call itself failed (network, rate limit, server error); already logged. */
    public static class CallFailedException extends RuntimeException {

        CallFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Not transactional on purpose: a failed call is logged and reported with an exception, and an
     * exception leaving a transactional proxy would mark the whole submission for rollback.
     */
    public LlmClient.Reply call(UUID submissionId, String purpose, LlmClient.Request request) {
        if (!client.available()) {
            throw new UnavailableException("No API key configured", false);
        }
        BigDecimal spent = calls.totalCost();
        if (spent.compareTo(budgetUsd) >= 0) {
            throw new UnavailableException("LLM budget of $" + budgetUsd + " is spent ($" + spent + ")", true);
        }
        long start = System.currentTimeMillis();
        try {
            LlmClient.Reply reply = client.complete(request);
            long latency = System.currentTimeMillis() - start;
            BigDecimal cost = cost(reply.inputTokens(), reply.outputTokens());
            calls.save(new LlmCall(submissionId, purpose, client.model(), reply.inputTokens(), reply.outputTokens(),
                    cost, latency, true, null));
            log.info("LLM {}: {} in / {} out tokens, ${} ({} ms)", purpose, reply.inputTokens(), reply.outputTokens(),
                    cost, latency);
            return reply;
        } catch (RuntimeException e) {
            calls.save(new LlmCall(submissionId, purpose, client.model(), 0, 0, BigDecimal.ZERO,
                    System.currentTimeMillis() - start, false, e.getClass().getSimpleName() + ": " + e.getMessage()));
            throw new CallFailedException("LLM call failed: " + e.getMessage(), e);
        }
    }

    public BigDecimal cost(long inputTokens, long outputTokens) {
        return inputUsdPerMtok.multiply(BigDecimal.valueOf(inputTokens))
                .add(outputUsdPerMtok.multiply(BigDecimal.valueOf(outputTokens)))
                .divide(MILLION, 8, RoundingMode.HALF_UP);
    }
}
