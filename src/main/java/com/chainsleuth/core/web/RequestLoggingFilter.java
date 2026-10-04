package com.chainsleuth.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * High-performance HTTP request/response logging filter populating MDC tracing context.
 * <p>
 * Generates an immutable {@code X-Request-Id} correlation token per HTTP interaction,
 * writes standardized entry/exit diagnostic logs, and binds context attributes to SLF4J's
 * {@link MDC} for distributed tracing ingestion.
 * </p>
 * <p>
 * <b>Virtual Thread Concurrency Dynamics:</b><br>
 * Under Java 25 and Spring Boot 4 virtual thread execution, each HTTP request runs on its own
 * lightweight virtual thread. MDC utilizes standard {@link ThreadLocal} semantics, guaranteeing
 * complete isolation without thread-leak cross-contamination. However, because virtual threads
 * do not automatically inherit parent ThreadLocal storage upon creation, concurrent tasks
 * dispatched from services into worker pools must utilize {@link MdcTaskDecorator} to copy state.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> HTTP Access Logging &amp; Correlation Filter</li>
 *   <li><b>Interacts with:</b> {@link MdcTaskDecorator}, {@link com.chainsleuth.core.exception.GlobalExceptionHandler}</li>
 * </ul>
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        long startTime = System.currentTimeMillis();

        MDC.put("requestId", requestId);
        MDC.put("httpMethod", method);
        MDC.put("uri", uri);

        response.setHeader("X-Request-Id", requestId);

        log.debug("→ Request: {} {} | Content-Type: {} | User-Agent: {}",
                method, uri,
                request.getContentType(),
                truncate(request.getHeader("User-Agent"), 100));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (status >= 400) {
                log.warn("← Response: {} {} | Status: {} | Duration: {}ms | RequestId: {}",
                        method, uri, status, durationMs, requestId);
            } else {
                log.info("← Response: {} {} | Status: {} | Duration: {}ms | RequestId: {}",
                        method, uri, status, durationMs, requestId);
            }

            MDC.remove("requestId");
            MDC.remove("httpMethod");
            MDC.remove("uri");
        }
    }

    /**
     * Suppresses logging for noisy health probes and static assets.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || "/favicon.ico".equals(path);
    }

    /**
     * Truncates header values to safe lengths for logging.
     */
    private String truncate(String s, int max) {
        if (s == null) {
            return "(none)";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
