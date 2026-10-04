package com.chainsleuth.core.exception;

import java.net.URI;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of all domain, infrastructure, and validation error codes in ChainSleuth.
 * <p>
 * Implements a standardized Uniform Resource Name (URN) scheme adhering to RFC 9457:
 * <pre>{@code
 *   urn:chainsleuth:error:<category>:<specific-code>
 * }</pre>
 * </p>
 * <p>
 * <b>Why URNs instead of URLs:</b><br>
 * URNs are persistent, location-independent identifiers. If API documentation or developer
 * portal URLs migrate (e.g., from {@code https://docs.chainsleuth.app} to {@code https://developer.chainsleuth.io}),
 * clients hardcoded to URL types break. URNs guarantee immutable error classification semantics
 * across all client libraries and automated SOC/SIEM alerting rules.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Central Error Code Registry</li>
 *   <li><b>Interacts with:</b> {@link ChainSleuthException}, {@link ChainSleuthProblemDetail}, {@link GlobalExceptionHandler}</li>
 * </ul>
 */
public enum ErrorCode {

    // =========================================================================
    // VALIDATION CATEGORY (400 Bad Request)
    // =========================================================================
    INVALID_REQUEST("validation:invalid-request", HttpStatus.BAD_REQUEST, "Invalid Request"),
    CONSTRAINT_VIOLATION("validation:constraint-violation", HttpStatus.BAD_REQUEST, "Constraint Violation"),
    MALFORMED_JSON("validation:malformed-json", HttpStatus.BAD_REQUEST, "Malformed JSON Body"),
    INVALID_ADDRESS("validation:invalid-address", HttpStatus.BAD_REQUEST, "Invalid Blockchain Address"),
    INVALID_CHAIN("validation:invalid-chain", HttpStatus.BAD_REQUEST, "Unsupported Blockchain Network"),
    MISSING_FIELD("validation:missing-field", HttpStatus.BAD_REQUEST, "Required Field Missing"),

    // =========================================================================
    // DOMAIN CATEGORY (404 Not Found & 403 Forbidden)
    // =========================================================================
    CASE_NOT_FOUND("domain:case-not-found", HttpStatus.NOT_FOUND, "Case Not Found"),
    EVIDENCE_NOT_FOUND("domain:evidence-not-found", HttpStatus.NOT_FOUND, "Evidence Not Found"),
    DOCUMENT_NOT_FOUND("domain:document-not-found", HttpStatus.NOT_FOUND, "Legal Document Not Found"),
    WALLET_NOT_FOUND("domain:wallet-not-found", HttpStatus.NOT_FOUND, "Wallet Not Found"),
    DOMAIN_FORBIDDEN("domain:forbidden", HttpStatus.FORBIDDEN, "Forbidden"),

    // =========================================================================
    // CHAIN & RPC CATEGORY (400, 429, 502, 504)
    // =========================================================================
    RPC_FAILURE("chain:rpc-failure", HttpStatus.BAD_GATEWAY, "Blockchain RPC Failure"),
    RPC_TIMEOUT("chain:rpc-timeout", HttpStatus.GATEWAY_TIMEOUT, "Blockchain RPC Timeout"),
    RPC_RATE_LIMIT("chain:rpc-rate-limit", HttpStatus.TOO_MANY_REQUESTS, "Blockchain RPC Rate Limit Exceeded"),
    UNSUPPORTED_CHAIN("chain:unsupported-chain", HttpStatus.BAD_REQUEST, "Unsupported Blockchain Network"),
    INVALID_TX_HASH("chain:invalid-tx-hash", HttpStatus.BAD_REQUEST, "Invalid Transaction Hash"),

    // =========================================================================
    // TRAVERSAL CATEGORY (409, 422, 504)
    // =========================================================================
    DEPTH_EXCEEDED("traversal:depth-exceeded", HttpStatus.UNPROCESSABLE_ENTITY, "Maximum Traversal Depth Exceeded"),
    TRAVERSAL_IN_PROGRESS("traversal:traversal-in-progress", HttpStatus.CONFLICT, "Traversal Already In Progress"),
    TRAVERSAL_TIMEOUT("traversal:traversal-timeout", HttpStatus.GATEWAY_TIMEOUT, "Traversal Timed Out"),
    CYCLE_DETECTED("traversal:cycle-detected", HttpStatus.UNPROCESSABLE_ENTITY, "Cycle Detected in Transaction Graph"),

    // =========================================================================
    // EVIDENCE CATEGORY (404, 409)
    // =========================================================================
    TAMPER_DETECTED("evidence:tamper-detected", HttpStatus.CONFLICT, "Evidence Tampering Detected"),
    CERT_NOT_FOUND("evidence:cert-not-found", HttpStatus.NOT_FOUND, "Evidence Certificate Not Found"),
    HASH_MISMATCH("evidence:hash-mismatch", HttpStatus.CONFLICT, "Evidence Hash Mismatch"),

    // =========================================================================
    // AI CATEGORY (422, 500, 503)
    // =========================================================================
    MODEL_UNAVAILABLE("ai:model-unavailable", HttpStatus.SERVICE_UNAVAILABLE, "AI Model Unavailable"),
    OUTPUT_PARSE_FAILURE("ai:output-parse-failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI Output Parsing Failed"),
    TOOL_CALL_FAILURE("ai:tool-call-failure", HttpStatus.INTERNAL_SERVER_ERROR, "AI Tool Execution Failed"),
    CONTEXT_TOO_LARGE("ai:context-too-large", HttpStatus.UNPROCESSABLE_ENTITY, "Input Too Large for AI Context"),

    // =========================================================================
    // SYSTEM CATEGORY (500, 502, 503)
    // =========================================================================
    INTERNAL_ERROR("system:internal-error", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    SERVICE_UNAVAILABLE("system:service-unavailable", HttpStatus.SERVICE_UNAVAILABLE, "Service Temporarily Unavailable"),
    DOWNSTREAM_FAILURE("system:downstream-failure", HttpStatus.BAD_GATEWAY, "Downstream Service Failure");

    private final URI typeUri;
    private final HttpStatus httpStatus;
    private final String shortCode;
    private final String defaultTitle;

    /**
     * Constructs an error code with its fully qualified URN, associated status, and human-readable title.
     *
     * @param uriPath      category and specific code segment (e.g. "validation:invalid-request")
     * @param httpStatus   standard HTTP status code mapping
     * @param defaultTitle default RFC 9457 title
     */
    ErrorCode(String uriPath, HttpStatus httpStatus, String defaultTitle) {
        this.typeUri = URI.create("urn:chainsleuth:error:" + uriPath);
        this.httpStatus = httpStatus;
        this.shortCode = uriPath.substring(uriPath.lastIndexOf(':') + 1);
        this.defaultTitle = defaultTitle;
    }

    /**
     * Resolves an {@link ErrorCode} constant matching the specified URN.
     *
     * @param uri target RFC 9457 error type URI
     * @return {@link Optional} containing matching code, or empty if unrecognized
     */
    public static Optional<ErrorCode> fromUri(URI uri) {
        if (uri == null) {
            return Optional.empty();
        }
        for (ErrorCode code : values()) {
            if (code.typeUri.equals(uri)) {
                return Optional.of(code);
            }
        }
        return Optional.empty();
    }

    public URI typeUri() {
        return typeUri;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String shortCode() {
        return shortCode;
    }

    public String defaultTitle() {
        return defaultTitle;
    }
}
