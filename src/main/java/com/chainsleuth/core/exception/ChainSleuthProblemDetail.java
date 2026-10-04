package com.chainsleuth.core.exception;

import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

/**
 * Enterprise extension of Spring Framework 7's {@link ProblemDetail}, providing mandatory
 * RFC 9457 custom properties for distributed tracing, timing, and programmatic error codes.
 * <p>
 * <b>RFC 9457 Custom Properties:</b>
 * <ul>
 *   <li>{@code traceId} — Extracted from SLF4J {@link MDC} (populated automatically by Micrometer Tracing)
 *   enabling immediate cross-referencing against ELK/Splunk logs.</li>
 *   <li>{@code timestamp} — ISO-8601 UTC timestamp capturing the precise server-side response emission moment.</li>
 *   <li>{@code errorCode} — Short token code (e.g. {@code invalid-request}) enabling frontend and client SDKs
 *   to execute clean switch statements without parsing full URN strings.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Subclassing Rationale:</b><br>
 * Subclassing {@link ProblemDetail} ensures that all error responses emitted by the platform
 * (whether originating from domain exceptions, Spring MVC validation failures, or unhandled 500s)
 * consistently include the required forensic extension fields.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> RFC 9457 ProblemDetail Extension Model</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthException}, {@link GlobalExceptionHandler}</li>
 * </ul>
 */
public class ChainSleuthProblemDetail extends ProblemDetail {

    /**
     * Copy constructor promoting a standard {@link ProblemDetail} to an enriched {@link ChainSleuthProblemDetail}.
     *
     * @param base source problem detail instance
     */
    private ChainSleuthProblemDetail(ProblemDetail base) {
        super(base.getStatus());
        setType(base.getType());
        setTitle(base.getTitle());
        setDetail(base.getDetail());
        setInstance(base.getInstance());
        if (base.getProperties() != null) {
            base.getProperties().forEach(this::setProperty);
        }
    }

    /**
     * Promotes an existing {@link ProblemDetail} and attaches mandatory trace and timestamp extensions.
     *
     * @param base source problem detail
     * @return enriched {@link ChainSleuthProblemDetail}
     */
    public static ChainSleuthProblemDetail from(ProblemDetail base) {
        ChainSleuthProblemDetail detail = new ChainSleuthProblemDetail(base);
        detail.setProperty("timestamp", Instant.now().toString());
        detail.setProperty("traceId", extractTraceId());
        if (detail.getProperties() == null || !detail.getProperties().containsKey("errorCode")) {
            detail.setProperty("errorCode", "error");
        }
        return detail;
    }

    /**
     * Factory method building a complete problem detail directly from an {@link ErrorCode}.
     *
     * @param code   target error code
     * @param detail descriptive detail message
     * @return enriched {@link ChainSleuthProblemDetail}
     */
    public static ChainSleuthProblemDetail forCode(ErrorCode code, String detail) {
        ProblemDetail base = ProblemDetail.forStatusAndDetail(code.httpStatus(), detail);
        base.setType(code.typeUri());
        base.setTitle(code.defaultTitle());
        base.setProperty("errorCode", code.shortCode());
        return from(base);
    }

    /**
     * Extracts active trace identifier from SLF4J MDC storage.
     *
     * @return active traceId or "UNAVAILABLE" fallback
     */
    private static String extractTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : "UNAVAILABLE";
    }
}
