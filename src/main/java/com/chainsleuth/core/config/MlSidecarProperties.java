package com.chainsleuth.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable configuration properties for the Python Machine Learning sidecar.
 * <p>
 * Manages gRPC connectivity, HTTP fallbacks, and circuit breaker resilience parameters
 * for transaction classification, entity clustering, and anomaly scoring.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1 — Foundational Infrastructure &amp; Core Configuration</li>
 *   <li><b>Platform Component:</b> ML Sidecar Connectivity Properties</li>
 *   <li><b>Owner:</b> Lead ML Infrastructure Engineer &amp; Platform Architect</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "chainsleuth.ml-sidecar")
@Validated
public record MlSidecarProperties(
    @NotBlank String grpcHost,
    @NotNull Integer grpcPort,
    @NotBlank String httpUrl,
    int connectTimeoutSeconds,
    int requestTimeoutSeconds,
    @Valid @NotNull CircuitBreakerConfig circuitBreaker
) {
    /**
     * Circuit breaker thresholds and recovery timeout values.
     *
     * @param failureThreshold       number of consecutive failures before opening circuit
     * @param recoveryTimeoutSeconds delay before transitioning from OPEN to HALF_OPEN
     */
    public record CircuitBreakerConfig(
        int failureThreshold,
        int recoveryTimeoutSeconds
    ) {}
}
