/**
 * ChainSleuth Exception Infrastructure — Phase 1C
 *
 * <p>This package implements a layered, RFC 9457-compliant error handling
 * system for the ChainSleuth blockchain forensics platform.
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * java.lang.Exception
 * └── org.springframework.web.ErrorResponseException  (Spring Framework 7)
 *     └── ChainSleuthException                        (abstract base)
 *         ├── EntityNotFoundException                 (404 domain errors)
 *         ├── ChainRpcException                       (502/429/504 RPC errors)
 *         ├── TraversalException                      (422/409/504 traversal errors)
 *         ├── EvidenceTamperedException               (409 integrity errors)
 *         ├── AiServiceException                      (503/500/422 AI errors)
 *         └── RateLimitExceededException              (429 rate limit errors)
 * </pre>
 *
 * <h2>Error Response Flow</h2>
 * <pre>
 * Controller/Service throws ChainSleuthException
 *   → Spring MVC catches it (it's an ErrorResponse)
 *   → GlobalExceptionHandler.handleChainSleuthException()
 *   → Promotes ProblemDetail to ChainSleuthProblemDetail (adds traceId, timestamp)
 *   → Returns ResponseEntity with Content-Type: application/problem+json
 * </pre>
 *
 * <h2>All Error Responses have these fields (RFC 9457 + ChainSleuth extensions)</h2>
 * <ul>
 *   <li>type      — urn:chainsleuth:error:{category}:{code}</li>
 *   <li>title     — short human-readable title</li>
 *   <li>status    — HTTP status code (integer)</li>
 *   <li>detail    — human-readable explanation specific to this occurrence</li>
 *   <li>instance  — the request URI where the error occurred</li>
 *   <li>traceId   — Micrometer trace ID for log correlation (ChainSleuth extension)</li>
 *   <li>timestamp — ISO 8601 UTC of error occurrence (ChainSleuth extension)</li>
 *   <li>errorCode — short code for client-side switch statements (ChainSleuth extension)</li>
 * </ul>
 *
 * <h2>Security Rules (NEVER violate)</h2>
 * <ol>
 *   <li>NEVER expose stack traces in error responses</li>
 *   <li>NEVER expose exception class names</li>
 *   <li>NEVER expose database error messages (may contain SQL/schema info)</li>
 *   <li>NEVER expose internal IP addresses, hostnames, or service names</li>
 *   <li>NEVER log PII (email, phone, name) in error logs — use userId/traceId</li>
 * </ol>
 */
package com.chainsleuth.core.exception;
