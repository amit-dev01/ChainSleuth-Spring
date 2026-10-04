package com.chainsleuth.core.exception;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Centralized exception handling interceptor governing all REST controller interactions across ChainSleuth.
 * <p>
 * <b>Layered Exception Architecture:</b>
 * <ul>
 *   <li><b>Layer 1 (Domain Exceptions):</b> {@link #handleChainSleuthException} catches all concrete domain
 *   exceptions (not-found, traversal limits, tampering, RPC timeouts) and attaches RFC 9457 extensions.</li>
 *   <li><b>Layer 2 (Framework Validation &amp; Parsing):</b> Overridden {@link ResponseEntityExceptionHandler}
 *   methods convert Spring MVC binding, JSON deserialization, and HTTP routing errors to RFC 9457 {@link ChainSleuthProblemDetail}.</li>
 *   <li><b>Layer 3 (Service Layer Constraints):</b> {@link #handleConstraintViolation} formats method-level validation failures.</li>
 *   <li><b>Layer 4 (Security Denials):</b> {@link #handleAccessDenied} captures service-level {@code @PreAuthorize} rejections.</li>
 *   <li><b>Layer 5 (Catch-All Safety Net):</b> {@link #handleUnexpectedException} masks unhandled internal errors behind generic messages.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Security &amp; Information Disclosure Prevention:</b><br>
 * Internal stack traces, raw database error strings, SQL snippets, and underlying Java class names are
 * strictly omitted from client responses. Complete diagnostic details are written securely to server logs
 * correlated by {@code traceId}.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Global REST Controller Advice</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthException}, {@link ChainSleuthProblemDetail}</li>
 * </ul>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // SECURITY RULES (MANDATORY INVARIANTS):
    // 1. NEVER expose stack traces in error responses
    // 2. NEVER expose exception class names
    // 3. NEVER expose database error messages (may contain SQL/schema info)
    // 4. NEVER expose internal IP addresses, hostnames, or service names
    // 5. NEVER log PII (email, phone, name) in error logs — use userId/traceId
    // =========================================================================

    /**
     * Default constructor.
     */
    public GlobalExceptionHandler() {
    }

    /**
     * Intercepts Spring MVC's internal framework exception handling to ensure all error bodies
     * are uniformly formatted as RFC 9457 {@link ChainSleuthProblemDetail} instances.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail enriched;
        if (body instanceof ProblemDetail pd) {
            enriched = ChainSleuthProblemDetail.from(pd);
        } else {
            enriched = ChainSleuthProblemDetail.forCode(
                    ErrorCode.INTERNAL_ERROR,
                    "An unexpected error occurred during request processing."
            );
        }

        if (enriched.getInstance() == null && request instanceof ServletWebRequest swr) {
            enriched.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }

        return super.handleExceptionInternal(ex, enriched, headers, status, request);
    }

    /**
     * Handles all custom domain and infrastructure exceptions extending {@link ChainSleuthException}.
     *
     * @param ex      domain exception instance
     * @param request current web request
     * @return structured RFC 9457 problem detail response
     */
    @ExceptionHandler(ChainSleuthException.class)
    public ResponseEntity<ProblemDetail> handleChainSleuthException(
            ChainSleuthException ex,
            WebRequest request
    ) {
        if (ex.getStatusCode().is5xxServerError()) {
            log.error("Domain exception [{}]: {}", ex.errorCode().shortCode(), ex.getMessage(), ex);
        } else {
            log.warn("Domain exception [{}]: {}", ex.errorCode().shortCode(), ex.getMessage());
        }

        ProblemDetail base = ex.getBody();
        ChainSleuthProblemDetail enriched = ChainSleuthProblemDetail.from(base);

        if (request instanceof ServletWebRequest swr) {
            enriched.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        if (ex instanceof RateLimitExceededException rle) {
            responseHeaders.set("Retry-After", String.valueOf(rle.retryAfterSeconds()));
        }

        return ResponseEntity
                .status(ex.getStatusCode())
                .headers(responseHeaders)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(enriched);
    }

    /**
     * Handles controller request body validation failures (@Valid / @Validated DTOs).
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String uri = request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "unknown";
        log.warn("Validation failure on {}: {} field errors", uri, ex.getBindingResult().getErrorCount());

        ChainSleuthProblemDetail detail = ChainSleuthProblemDetail.forCode(
                ErrorCode.CONSTRAINT_VIOLATION,
                "Request validation failed. See 'fieldErrors' for details."
        );

        if (request instanceof ServletWebRequest swr) {
            detail.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }

        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        "rejected", fe.getRejectedValue() != null ? sanitizeForLog(fe.getRejectedValue().toString()) : "null"
                ))
                .toList();

        detail.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }

    /**
     * Handles unreadable or malformed JSON payloads.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Malformed JSON request body: {}", sanitizeForLog(ex.getMessage()));

        ChainSleuthProblemDetail detail = ChainSleuthProblemDetail.forCode(
                ErrorCode.MALFORMED_JSON,
                "The request body could not be parsed as valid JSON. Check the Content-Type header and body format."
        );

        if (request instanceof ServletWebRequest swr) {
            detail.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }

    /**
     * Handles Jakarta Bean Validation violations originating from method-level validation at the service tier.
     *
     * @param ex      constraint violation exception
     * @param request current web request
     * @return RFC 9457 response with violation paths
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request
    ) {
        log.warn("Constraint violation: {} violations", ex.getConstraintViolations().size());

        ChainSleuthProblemDetail detail = ChainSleuthProblemDetail.forCode(
                ErrorCode.CONSTRAINT_VIOLATION,
                "Input validation failed at the service layer."
        );

        if (request instanceof ServletWebRequest swr) {
            detail.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }

        List<Map<String, String>> violations = ex.getConstraintViolations()
                .stream()
                .map(cv -> Map.of(
                        "path", cv.getPropertyPath().toString(),
                        "message", cv.getMessage(),
                        "value", cv.getInvalidValue() != null ? sanitizeForLog(cv.getInvalidValue().toString()) : "null"
                ))
                .toList();

        detail.setProperty("violations", violations);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }

    /**
     * Handles Spring Security authorization rejections raised from service-level @PreAuthorize evaluations.
     *
     * @param ex      access denied exception
     * @param request current web request
     * @return RFC 9457 403 Forbidden response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request
    ) {
        String uri = request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "unknown";
        log.info("Access denied on {}: {}", uri, ex.getMessage());

        ChainSleuthProblemDetail detail = ChainSleuthProblemDetail.forCode(
                ErrorCode.DOMAIN_FORBIDDEN,
                "You do not have permission to perform this action."
        );

        if (request instanceof ServletWebRequest swr) {
            detail.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }

    /**
     * Global catch-all handler for unexpected internal exceptions.
     *
     * @param ex      unexpected exception
     * @param request current web request
     * @return sanitized RFC 9457 500 response referencing traceId
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception ex,
            WebRequest request
    ) {
        String uri = request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "unknown";
        log.error("Unexpected exception at {}: {}", uri, ex.getMessage(), ex);

        ChainSleuthProblemDetail detail = ChainSleuthProblemDetail.forCode(
                ErrorCode.INTERNAL_ERROR,
                "An unexpected internal error occurred. Please contact support with the traceId from this response."
        );

        if (request instanceof ServletWebRequest swr) {
            detail.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }

    /**
     * Sanitizes inputs to neutralize Log Injection (CWE-117) vulnerabilities.
     *
     * @param input raw input string
     * @return sanitized string capped at 500 characters
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return "(null)";
        }
        String cleaned = input.replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
    }
}
