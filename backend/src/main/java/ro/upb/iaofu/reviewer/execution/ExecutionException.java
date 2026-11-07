package ro.upb.iaofu.reviewer.execution;

/** Piston is unreachable or the harness produced no result. */
public class ExecutionException extends RuntimeException {

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
