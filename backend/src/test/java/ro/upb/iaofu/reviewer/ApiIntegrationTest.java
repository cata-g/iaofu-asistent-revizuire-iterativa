package ro.upb.iaofu.reviewer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.upb.iaofu.reviewer.execution.ExecutionService;
import ro.upb.iaofu.reviewer.execution.TestReport;
import ro.upb.iaofu.reviewer.session.SubmissionRepository;
import tools.jackson.databind.JsonNode;

/** End-to-end through HTTP and the H2 database, with Piston replaced by a mock. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SubmissionRepository submissions;

    @MockitoBean
    ExecutionService execution;

    private JsonNode body(String content) {
        return Fixtures.JSON.readTree(content);
    }

    @Test
    void problemListAndDetailHideHiddenData() throws Exception {
        mvc.perform(get("/api/problems").param("category", "hashing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("two-sum"));
        mvc.perform(get("/api/problems").param("difficulty", "medium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String detail = mvc.perform(get("/api/problems/two-sum")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).doesNotContain("referenceCode", "\"tests\"", "forbiddenTerms");

        mvc.perform(get("/api/problems/nope")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void sessionFlowStoresVersions() throws Exception {
        when(execution.evaluate(any(), anyString()))
                .thenReturn(Fixtures.json("two-sum/report-v1.json", TestReport.class))
                .thenReturn(Fixtures.json("two-sum/report-v2.json", TestReport.class));

        String created = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemId\":\"two-sum\",\"mode\":\"baseline_a\",\"participantId\":\"P1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body(created).has("mode")).isFalse();
        String sessionId = body(created).get("sessionId").asString();

        mvc.perform(post("/api/sessions/{id}/submissions", sessionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"v1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.report.efficiency").value("too_slow"));
        mvc.perform(post("/api/sessions/{id}/submissions", sessionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"v2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.report.summary.passed").value(11));

        assertThat(submissions.findBySessionIdOrderByVersionAsc(java.util.UUID.fromString(sessionId)))
                .extracting(s -> s.getCode()).containsExactly("v1", "v2");

        mvc.perform(post("/api/sessions/{id}/finish", sessionId)).andExpect(status().isNoContent());
        mvc.perform(post("/api/sessions/{id}/submissions", sessionId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"v3\"}")).andExpect(status().isConflict());
    }

    @Test
    void invalidRequestsAreBadRequests() throws Exception {
        mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemId\":\"two-sum\",\"mode\":\"baseline_c\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
