package ro.upb.iaofu.reviewer.llm;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LlmCallRepository extends JpaRepository<LlmCall, UUID> {

    @Query("select coalesce(sum(c.costUsd), 0) from LlmCall c")
    BigDecimal totalCost();
}
