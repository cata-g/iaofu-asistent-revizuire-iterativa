package ro.upb.iaofu.reviewer.export;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSetMetaData;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.upb.iaofu.reviewer.common.NotFoundException;

/**
 * CSV export of the log for the analysis (F19, docs/contracte.md section 6). Includes rejected
 * observations and the session mode, so it is a research endpoint, not one for the student.
 * The tables are small (a study has a few hundred rows), so the CSV is built in memory.
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    /** URL name to a fixed query; nothing from the request reaches the SQL. */
    private static final Map<String, String> QUERIES = Map.of(
            "sessions", "select * from session order by created_at",
            "submissions", "select * from submission order by created_at",
            "observations", "select * from observation order by created_at",
            "hint_reveals", "select * from hint_reveal order by revealed_at",
            "feedback", "select * from feedback order by created_at",
            "llm_calls", "select * from llm_call order by created_at");

    private final JdbcTemplate jdbc;

    public ExportController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/{table}.csv")
    public ResponseEntity<String> export(@PathVariable String table) {
        String sql = QUERIES.get(table);
        if (sql == null) {
            throw new NotFoundException("Unknown export '" + table + "'; expected one of " + QUERIES.keySet());
        }
        String csv = jdbc.query(sql, (ResultSetExtractor<String>) rs -> {
            StringBuilder out = new StringBuilder();
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            for (int i = 1; i <= columns; i++) {
                out.append(i > 1 ? "," : "").append(escape(meta.getColumnLabel(i).toLowerCase()));
            }
            out.append('\n');
            while (rs.next()) {
                for (int i = 1; i <= columns; i++) {
                    Object value = rs.getObject(i);
                    out.append(i > 1 ? "," : "").append(escape(value == null ? "" : value.toString()));
                }
                out.append('\n');
            }
            return out.toString();
        });
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + table + ".csv\"")
                .body(csv);
    }

    static String escape(String value) {
        boolean quote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        return quote ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
