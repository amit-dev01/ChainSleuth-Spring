package com.chainsleuth.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable configuration properties for application security, CORS, and JWT claims.
 * <p>
 * Defines trusted frontend origins and JWT token validation requirements such as the Supabase
 * audience claim.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1 — Foundational Infrastructure &amp; Core Configuration</li>
 *   <li><b>Platform Component:</b> Security &amp; OIDC Policy Configuration</li>
 *   <li><b>Owner:</b> Principal Security Architect</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "chainsleuth.security")
@Validated
public record SecurityProperties(
    @Valid @NotNull CorsConfig cors,
    @Valid @NotNull JwtConfig jwt
) {
    /**
     * Cross-Origin Resource Sharing (CORS) security configuration.
     *
     * @param allowedOrigins list of permitted origin URLs
     */
    public record CorsConfig(
        @NotEmpty List<String> allowedOrigins
    ) {}

    /**
     * JSON Web Token (JWT) claim verification parameters.
     *
     * @param audience expected audience claim (e.g., 'authenticated' for Supabase)
     */
    public record JwtConfig(
        @NotBlank String audience
    ) {}
}
