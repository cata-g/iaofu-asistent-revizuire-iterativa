package ro.upb.iaofu.reviewer.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import ro.upb.iaofu.reviewer.Fixtures;
import ro.upb.iaofu.reviewer.problem.Problem;

class ExecutionServiceTest {

    private final PistonClient piston = mock(PistonClient.class);
    private final ExecutionService service = new ExecutionService(piston, new HarnessBuilder(Fixtures.JSON),
            Fixtures.JSON);
    private final Problem problem = Fixtures.twoSum();

    private static PistonClient.Run stdout(String out) {
        return new PistonClient.Run(out, "", 0, null, null, null);
    }

    private static String marker(String fixture) {
        return ExecutionService.RESULT_MARKER + Fixtures.text(fixture).replace("\n", "").replace("\r", "");
    }

    @Test
    void ignoresPrintsFromTheSolution() {
        HarnessResult r = service.parse(stdout("debug print\n__HARNESS_RESULT__{\"status\":\"ok\",\"valid\":true}\n"));

        assertThat(r.ok()).isTrue();
        assertThat(r.valid()).isTrue();
    }

    @Test
    void killedProcessBecomesCrashedResult() {
        HarnessResult r = service.parse(new PistonClient.Run("", "", null, "SIGKILL", "TO",
                "Time limit exceeded (wall clock)"));

        assertThat(r.status()).isEqualTo(HarnessResult.CRASHED);
        assertThat(r.error()).contains("Time limit");
    }

    @Test
    void evaluateRunsTheReferenceOnlyOnce() {
        when(piston.execute(anyList(), anyString(), anyLong())).thenAnswer(inv -> {
            boolean isReference = inv.<java.util.List<PistonClient.File>>getArgument(0).get(1).content()
                    .equals(problem.referenceCode());
            return stdout(marker(isReference ? "two-sum/harness-raw-reference.json" : "two-sum/harness-raw-v2.json"));
        });

        TestReport first = service.evaluate(problem, Fixtures.text("two-sum/v2.py"));
        service.evaluate(problem, Fixtures.text("two-sum/v2.py"));

        assertThat(first.failedTests()).hasSize(3);
        // two student runs + one reference run
        verify(piston, times(3)).execute(anyList(), anyString(), anyLong());
    }

    @Test
    void referenceThatTimesOutIsAConfigurationError() {
        when(piston.execute(anyList(), anyString(), anyLong()))
                .thenReturn(stdout(marker("two-sum/harness-raw-v1.json")));

        assertThatThrownBy(() -> service.reference(problem)).isInstanceOf(ExecutionException.class)
                .hasMessageContaining("does not finish");
    }

    @Test
    void sendsHarnessSolutionAndInputs() {
        var files = new HarnessBuilder(Fixtures.JSON).files(problem, "code");

        assertThat(files).extracting(PistonClient.File::name).containsExactly("harness.py", "solution.py", "inputs.py");
        assertThat(files.get(0).content()).contains("__HARNESS_RESULT__");
    }
}
