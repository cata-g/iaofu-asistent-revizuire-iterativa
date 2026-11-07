package ro.upb.iaofu.reviewer.common;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ro.upb.iaofu.reviewer.execution.ExecutionException;

/** Maps exceptions to the { "error": "..." } shape from docs/contracte.md section 6. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Map<String, String>> conflict(ConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<Map<String, String>> badRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, e instanceof MethodArgumentNotValidException
                ? "Invalid request body" : e.getMessage());
    }

    @ExceptionHandler(ExecutionException.class)
    ResponseEntity<Map<String, String>> execution(ExecutionException e) {
        log.error("Code execution failed", e);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Code execution is unavailable: " + e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message == null ? status.getReasonPhrase() : message));
    }
}
