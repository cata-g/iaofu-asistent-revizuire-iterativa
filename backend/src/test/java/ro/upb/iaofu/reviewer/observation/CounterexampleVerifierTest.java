package ro.upb.iaofu.reviewer.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;
import ro.upb.iaofu.reviewer.execution.ExecutionService;
import ro.upb.iaofu.reviewer.execution.HarnessResult;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.problem.Problem;
import tools.jackson.databind.JsonNode;

class CounterexampleVerifierTest {

    private final ExecutionService execution = mock(ExecutionService.class);
    private final CounterexampleVerifier verifier = new CounterexampleVerifier(execution);
    private final Problem problem = Fixtures.twoSum();
    private final String v2 = Fixtures.text("two-sum/v2.py");
    private final TestReport reportV1 = Fixtures.json("two-sum/report-v1.json", TestReport.class);
    private final TestReport reportV2 = Fixtures.json("two-sum/report-v2.json", TestReport.class);

    private static JsonNode node(String json) {
        return Fixtures.JSON.readTree(json);
    }

    private static HarnessResult single(String actual, String error) {
        return new HarnessResult("ok", null, null, null, actual == null ? null : node(actual), 0.1, null);
    }

    private static Proposal edge(String input) {
        return new Proposal("edge_case", 4, input == null ? null : node(input), "r", List.of("a", "b", "c"));
    }

    private void outputs(String student, String reference) {
        when(execution.isValidInput(eq(problem), any())).thenReturn(true);
        when(execution.runSingle(eq(problem), eq(problem.referenceCode()), any())).thenReturn(single(reference, null));
        when(execution.runSingle(eq(problem), eq(v2), any())).thenReturn(single(student, null));
    }

    @Test
    void differentOutputsConfirm() {
        outputs("[0,0]", "[1,2]");

        CounterexampleVerifier.Verdict v = verifier.verify(problem, v2, reportV2, edge("{\"nums\":[3,2,4],\"target\":6}"));

        assertThat(v.confirmed()).isTrue();
        assertThat(v.studentOutput().toString()).isEqualTo("[0,0]");
        assertThat(v.expectedOutput().toString()).isEqualTo("[1,2]");
    }

    @Test
    void sameOutputRejects() {
        outputs("[0,1]", "[0,1]");

        CounterexampleVerifier.Verdict v = verifier.verify(problem, v2, reportV2, edge("{\"nums\":[1,2],\"target\":3}"));

        assertThat(v.confirmed()).isFalse();
        assertThat(v.rejectReason()).isEqualTo(CounterexampleVerifier.SAME_OUTPUT);
    }

    @Test
    void studentCrashConfirms() {
        when(execution.isValidInput(eq(problem), any())).thenReturn(true);
        when(execution.runSingle(eq(problem), eq(problem.referenceCode()), any())).thenReturn(single("[0,1]", null));
        when(execution.runSingle(eq(problem), eq(v2), any()))
                .thenReturn(new HarnessResult("ok", "line 3: IndexError: list index out of range", null, null, null, 0.1, null));

        CounterexampleVerifier.Verdict v = verifier.verify(problem, v2, reportV2, edge("{\"nums\":[1,2],\"target\":3}"));

        assertThat(v.confirmed()).isTrue();
        assertThat(v.studentOutput().asString()).contains("IndexError");
    }

    @Test
    void wrongParametersAreRejectedWithoutRunning() {
        CounterexampleVerifier.Verdict v = verifier.verify(problem, v2, reportV2, edge("{\"arr\":[3,2,4],\"target\":6}"));

        assertThat(v.rejectReason()).isEqualTo(CounterexampleVerifier.INVALID_INPUT);
        verify(execution, never()).runSingle(any(), any(), any());
        assertThat(verifier.verify(problem, v2, reportV2, edge(null)).rejectReason())
                .isEqualTo(CounterexampleVerifier.INVALID_INPUT);
        assertThat(verifier.verify(problem, v2, reportV2, edge("\"nums=[1]\"")).rejectReason())
                .isEqualTo(CounterexampleVerifier.INVALID_INPUT);
    }

    @Test
    void inputBreakingTheConstraintsIsRejected() {
        when(execution.isValidInput(eq(problem), any())).thenReturn(false);

        // Two solutions: violates "exactly one solution"
        CounterexampleVerifier.Verdict v = verifier.verify(problem, v2, reportV2, edge("{\"nums\":[1,2,1,2],\"target\":3}"));

        assertThat(v.rejectReason()).isEqualTo(CounterexampleVerifier.INVALID_INPUT);
    }

    @Test
    void efficiencyNeedsAMeasuredTimeout() {
        Proposal efficiency = new Proposal("efficiency", 3, null, "r", List.of("a", "b", "c"));

        CounterexampleVerifier.Verdict slow = verifier.verify(problem, "", reportV1, efficiency);
        CounterexampleVerifier.Verdict fast = verifier.verify(problem, v2, reportV2, efficiency);

        assertThat(slow.confirmed()).isTrue();
        assertThat(slow.studentOutput().asString()).isEqualTo("La n = 10 000 depășește limita de 1000 ms.");
        assertThat(fast.rejectReason()).isEqualTo(CounterexampleVerifier.NOT_SLOW);
    }
}
