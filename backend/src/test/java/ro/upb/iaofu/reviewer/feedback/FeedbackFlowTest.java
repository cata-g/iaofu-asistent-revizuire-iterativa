package ro.upb.iaofu.reviewer.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import ro.upb.iaofu.reviewer.Fixtures;
import ro.upb.iaofu.reviewer.execution.ExecutionService;
import ro.upb.iaofu.reviewer.execution.HarnessResult;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.llm.LlmCall;
import ro.upb.iaofu.reviewer.llm.LlmCallRepository;
import ro.upb.iaofu.reviewer.llm.LlmClient;
import ro.upb.iaofu.reviewer.observation.Observation;
import ro.upb.iaofu.reviewer.observation.ObservationRepository;
import ro.upb.iaofu.reviewer.observation.ObservationView;
import ro.upb.iaofu.reviewer.problem.Problem;
import ro.upb.iaofu.reviewer.problem.ProblemRepository;
import ro.upb.iaofu.reviewer.session.Mode;
import ro.upb.iaofu.reviewer.session.Session;
import ro.upb.iaofu.reviewer.session.SessionDtos;
import ro.upb.iaofu.reviewer.session.SessionService;

/** The three variants end to end on H2, with the model and Piston mocked (no network, no cost). */
@SpringBootTest
@Transactional
class FeedbackFlowTest {

    @Autowired
    SessionService sessions;
    @Autowired
    ObservationRepository observations;
    @Autowired
    LlmCallRepository llmCalls;
    @Autowired
    ProblemRepository problems;

    @MockitoBean
    LlmClient llm;
    @MockitoBean
    ExecutionService execution;

    private Problem problem;
    private final String v2 = Fixtures.text("two-sum/v2.py");

    @BeforeEach
    void setUp() {
        problem = problems.findById("two-sum").orElseThrow();
        when(llm.available()).thenReturn(true);
        when(llm.model()).thenReturn("claude-haiku-5-5");
        when(execution.evaluate(any(), anyString())).thenReturn(Fixtures.json("two-sum/report-v2.json", TestReport.class));
        when(execution.isValidInput(any(), any())).thenReturn(true);
        // Reference: [1,2] on [3,2,4] and [0,1] on [1,2]; v2: [0,0] and [0,1]
        when(execution.runSingle(any(), eq(problem.referenceCode()), any())).thenAnswer(inv ->
                single(inv.getArgument(2).toString().contains("[3,2,4]") ? "[1,2]" : "[0,1]"));
        when(execution.runSingle(any(), eq(v2), any())).thenAnswer(inv ->
                single(inv.getArgument(2).toString().contains("[3,2,4]") ? "[0,0]" : "[0,1]"));
    }

    private static HarnessResult single(String actual) {
        return new HarnessResult("ok", null, null, null, Fixtures.JSON.readTree(actual), 0.1, null);
    }

    private static LlmClient.Reply reply(String text) {
        return new LlmClient.Reply(text, 2000, 1000, "end_turn");
    }

    private SessionDtos.SubmissionResponse submit(Mode mode) {
        Session s = sessions.create("two-sum", mode, "T");
        return sessions.submit(s.getId(), v2);
    }

    @Test
    void proposedShowsOnlyConfirmedObservations() {
        when(llm.complete(any())).thenReturn(reply(Fixtures.text("llm/observations-valid.json")));

        SessionDtos.SubmissionResponse r = submit(Mode.PROPOSED);

        assertThat(r.llmStatus()).isEqualTo("ok");
        assertThat(r.observations()).hasSize(1);
        ObservationView shown = r.observations().getFirst();
        assertThat(shown.line()).isEqualTo(4);
        assertThat(shown.evidence().actual().toString()).isEqualTo("[0,0]");
        assertThat(shown.hints()).hasSize(1);

        List<Observation> stored = observations.findAll();
        assertThat(stored).extracting(Observation::getStatus).containsExactlyInAnyOrder("confirmed", "rejected");
        assertThat(stored).filteredOn(o -> !o.isConfirmed()).extracting(Observation::getRejectReason)
                .containsExactly("same_output");
    }

    @Test
    void everyCallIsLoggedWithItsCost() {
        when(llm.complete(any())).thenReturn(reply(Fixtures.text("llm/observations-valid.json")));

        submit(Mode.PROPOSED);

        List<LlmCall> calls = llmCalls.findAll();
        assertThat(calls).hasSize(1);
        // 2000 * 0.10 / 1e6 + 1000 * 0.50 / 1e6
        assertThat(calls.getFirst().getCostUsd()).isEqualByComparingTo(new BigDecimal("0.0007"));
    }

    @Test
    void invalidJsonIsRetriedOnceThenOnlyTestsAreShown() {
        when(llm.complete(any())).thenReturn(reply(Fixtures.text("llm/observations-invalid.json")));

        SessionDtos.SubmissionResponse r = submit(Mode.PROPOSED);

        assertThat(r.llmStatus()).isEqualTo("failed");
        assertThat(r.observations()).isEmpty();
        assertThat(r.report().summary().passed()).isEqualTo(11);
        verify(llm, times(2)).complete(any());
        assertThat(llmCalls.findAll()).extracting(LlmCall::getPurpose)
                .containsExactlyInAnyOrder("observations", "observations_retry");
    }

    @Test
    void leakingHintsAreRewritten() {
        String leaking = """
                {"observations":[{"type":"edge_case","line":4,"evidenceInputJson":"{\\"nums\\":[3,2,4],\\"target\\":6}",
                "risk":"r","hints":["Folosește un hash?","Caută complementul.","Nivel 3."]}]}""";
        when(llm.complete(any())).thenReturn(reply(leaking))
                .thenReturn(reply("{\"risk\":\"Același index apare de două ori.\",\"hint1\":\"Ce index apare de două ori?\",\"hint2\":\"Contează ordinea operațiilor?\"}"));

        SessionDtos.SubmissionResponse r = submit(Mode.PROPOSED);

        assertThat(r.observations()).hasSize(1);
        assertThat(r.observations().getFirst().hints().getFirst().text()).isEqualTo("Ce index apare de două ori?");
        assertThat(observations.findAll().getFirst().getLeakRetries()).isEqualTo(1);
    }

    @Test
    void aLeakingRiskIsRewrittenToo() {
        String leaking = """
                {"observations":[{"type":"edge_case","line":4,"evidenceInputJson":"{\\"nums\\":[3,2,4],\\"target\\":6}",
                "risk":"Ar trebui un hashmap.","hints":["a","b","c"]}]}""";
        when(llm.complete(any())).thenReturn(reply(leaking))
                .thenReturn(reply("{\"risk\":\"Același index apare de două ori.\",\"hint1\":\"a\",\"hint2\":\"b\"}"));

        SessionDtos.SubmissionResponse r = submit(Mode.PROPOSED);

        assertThat(r.observations().getFirst().risk()).isEqualTo("Același index apare de două ori.");
    }

    @Test
    void hintsThatKeepLeakingAreDropped() {
        String leaking = """
                {"observations":[{"type":"edge_case","line":4,"evidenceInputJson":"{\\"nums\\":[3,2,4],\\"target\\":6}",
                "risk":"r","hints":["Folosește un hash?","x","y"]}]}""";
        when(llm.complete(any())).thenReturn(reply(leaking))
                .thenReturn(reply("{\"risk\":\"r\",\"hint1\":\"Tot un hash.\",\"hint2\":\"x\"}"));

        SessionDtos.SubmissionResponse r = submit(Mode.PROPOSED);

        assertThat(r.observations()).isEmpty();
        assertThat(observations.findAll().getFirst().getRejectReason()).isEqualTo("leak");
        verify(llm, times(3)).complete(any());
    }

    @Test
    void baselineBReturnsFreeText() {
        when(llm.complete(any())).thenReturn(reply(Fixtures.text("llm/baseline-b-response.md")));

        SessionDtos.SubmissionResponse r = submit(Mode.BASELINE_B);

        assertThat(r.assistantMessage()).contains("Soluția corectă");
        assertThat(r.observations()).isEmpty();
        assertThat(r.classification()).isNull();
        assertThat(llmCalls.findAll()).extracting(LlmCall::getPurpose).containsExactly("baseline_b");
    }

    @Test
    void baselineANeverCallsTheModel() {
        SessionDtos.SubmissionResponse r = submit(Mode.BASELINE_A);

        assertThat(r.llmStatus()).isEqualTo("not_used");
        verify(llm, never()).complete(any());
    }

    @Test
    void allTestsPassingSkipsTheModel() {
        TestReport v2 = Fixtures.json("two-sum/report-v2.json", TestReport.class);
        when(execution.evaluate(any(), anyString())).thenReturn(new TestReport("ok", null,
                new TestReport.Summary(14, 14), List.of(), v2.timings(), v2.slope(), "ok"));

        assertThat(submit(Mode.PROPOSED).llmStatus()).isEqualTo("not_used");
        assertThat(submit(Mode.BASELINE_B).llmStatus()).isEqualTo("not_used");
        verify(llm, never()).complete(any());
    }

    @Test
    void budgetStopsTheCalls() {
        llmCalls.save(new LlmCall(null, "observations", "claude-haiku-5-5", 0, 0, new BigDecimal("5.00"), 0, true,
                null));

        SessionDtos.SubmissionResponse r = submit(Mode.PROPOSED);

        assertThat(r.llmStatus()).isEqualTo("budget_exceeded");
        verify(llm, never()).complete(any());
    }

    @Test
    void apiErrorsDegradeToTestResults() {
        when(llm.complete(any())).thenThrow(new RuntimeException("overloaded"));

        SessionDtos.SubmissionResponse r = submit(Mode.BASELINE_B);

        assertThat(r.llmStatus()).isEqualTo("failed");
        assertThat(llmCalls.findAll()).extracting(LlmCall::isSuccess).containsExactly(false);
    }
}
