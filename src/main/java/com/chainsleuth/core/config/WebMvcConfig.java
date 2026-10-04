package com.chainsleuth.core.config;

import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration establishing content negotiation rules and hardened error attributes.
 * <p>
 * <b>Four-Layer Exception Defense-in-Depth:</b>
 * <ol>
 *   <li><b>Layer 1 (Domain Layer):</b> {@link com.chainsleuth.core.exception.GlobalExceptionHandler} explicitly intercepts
 *   all domain exceptions extending {@link com.chainsleuth.core.exception.ChainSleuthException}.</li>
 *   <li><b>Layer 2 (Framework Layer):</b> GlobalExceptionHandler extends {@link org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler}
 *   to handle Spring MVC dispatch and conversion exceptions.</li>
 *   <li><b>Layer 3 (Auto-Configuration Layer):</b> {@code spring.mvc.problemdetails.enabled=true} enables automatic RFC 9457
 *   generation for framework edge-cases.</li>
 *   <li><b>Layer 4 (Fallback Safety Net):</b> The custom {@link ErrorAttributes} bean defined below strips stack traces and internal
 *   exception details from Spring Boot's default {@code /error} controller.</li>
 * </ol>
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Web MVC Content &amp; Error Configuration</li>
 *   <li><b>Interacts with:</b> {@link com.chainsleuth.core.exception.GlobalExceptionHandler}</li>
 * </ul>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Registers application/problem+json as an explicitly recognized content negotiation media type.
     *
     * @param configurer content negotiation DSL configurer
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("problem", MediaType.APPLICATION_PROBLEM_JSON);
    }

    /**
     * Hardened fallback error attribute provider for container-level errors routed to /error.
     * Eliminates stack traces, exception class names, and attaches active trace identifiers.
     *
     * @return custom {@link ErrorAttributes} bean
     */
    @Bean
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> attrs = super.getErrorAttributes(webRequest, options);

                // Strip sensitive data to prevent information leakage
                attrs.remove("trace");
                attrs.remove("exception");
                attrs.remove("errors");

                String traceId = MDC.get("traceId");
                if (traceId != null && !traceId.isBlank()) {
                    attrs.put("traceId", traceId);
                } else {
                    attrs.put("traceId", "UNAVAILABLE");
                }

                attrs.put("timestamp", Instant.now().toString());
                return attrs;
            }
        };
    }
}
