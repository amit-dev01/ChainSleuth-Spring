package com.chainsleuth.core.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Abstract foundational exception class for all domain and infrastructure exceptions in ChainSleuth.
 * <p>
 * <b>Spring Framework 7 ErrorResponse Integration:</b><br>
 * Extends {@link ErrorResponseException}, directly implementing Spring's {@link org.springframework.web.ErrorResponse}
 * interface. This guarantees that any domain exception thrown from services or controllers can be
 * natively processed by Spring MVC's {@link org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler}
 * even in the absence of a specialized {@code @ExceptionHandler} method.
 * </p>
 * <p>
 * <b>Deferred Trace &amp; Timestamp Lifecycle:</b><br>
 * MDC trace identifiers and ISO-8601 timestamps are intentionally <i>not</i> populated in this constructor.
 * Because exceptions may be constructed before an asynchronous boundary or inside virtual threads prior to context
 * attachment, dynamic metadata is injected downstream by {@link GlobalExceptionHandler} and {@link ChainSleuthProblemDetail}.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Root Domain Exception</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthProblemDetail}, {@link GlobalExceptionHandler}</li>
 * </ul>
 */
public abstract class ChainSleuthException extends ErrorResponseException {

    private final ErrorCode errorCode;
    private final String detail;

    /**
     * Constructs a domain exception without an underlying cause.
     *
     * @param errorCode strongly-typed error code defining status and type URI
     * @param detail    human-readable, non-sensitive context describing the specific failure
     */
    protected ChainSleuthException(ErrorCode errorCode, String detail) {
        super(errorCode.httpStatus(), buildProblemDetail(errorCode, detail), null);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * Constructs a domain exception preserving the underlying root cause for server-side logging.
     *
     * @param errorCode strongly-typed error code defining status and type URI
     * @param detail    human-readable, non-sensitive context describing the specific failure
     * @param cause     underlying throwable cause
     */
    protected ChainSleuthException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.httpStatus(), buildProblemDetail(errorCode, detail), null);
        this.errorCode = errorCode;
        this.detail = detail;
        initCause(cause);
    }

    /**
     * Constructs an initial RFC 9457 {@link ProblemDetail} payload.
     *
     * @param errorCode error code registry entry
     * @param detail    descriptive explanation
     * @return initialized {@link ProblemDetail}
     */
    private static ProblemDetail buildProblemDetail(ErrorCode errorCode, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorCode.httpStatus(), detail);
        pd.setType(errorCode.typeUri());
        pd.setTitle(errorCode.defaultTitle());
        pd.setProperty("errorCode", errorCode.shortCode());
        return pd;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String detail() {
        return detail;
    }

    @Override
    public String getMessage() {
        return "[" + errorCode.shortCode() + "] " + detail;
    }
}
