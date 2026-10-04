package com.chainsleuth.core.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable configuration properties for cryptographic evidence generation and court verification.
 * <p>
 * Enforces entropy constraints on HMAC signing keys to guarantee chain-of-custody integrity
 * under Federal Rules of Evidence (FRE) Rule 902(14) for self-authenticating digital records.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1 — Foundational Infrastructure &amp; Core Configuration</li>
 *   <li><b>Platform Component:</b> Evidence Certification Security Parameters</li>
 *   <li><b>Owner:</b> Lead Cryptography &amp; Legal Compliance Engineer</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "chainsleuth.evidence")
@Validated
public record EvidenceProperties(
    @NotBlank String hmacSecretKey,
    @NotBlank String verificationBaseUrl
) {
    /**
     * Compact constructor providing immediate fail-fast validation upon instantiation.
     */
    public EvidenceProperties {
        if (hmacSecretKey != null && hmacSecretKey.trim().length() < 32) {
            throw new IllegalArgumentException(
                "HMAC secret key must be at least 32 characters (256-bit entropy requirement)");
        }
    }

    /**
     * Validates that the configured HMAC secret key satisfies enterprise entropy standards
     * for SHA-256 evidence certification.
     *
     * @return {@code true} if key satisfies the minimum 32-character requirement
     */
    @AssertTrue(message = "HMAC secret key must be at least 32 characters (256-bit entropy minimum for legal evidence sealing)")
    public boolean isHmacSecretKeySufficientlyLong() {
        return hmacSecretKey != null && hmacSecretKey.trim().length() >= 32;
    }
}
