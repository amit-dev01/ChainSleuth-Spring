package com.chainsleuth.core.security;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Token validator enforcing Supabase-specific audience guarantees and preventing anonymous access.
 * <p>
 * <b>Early Rejection Performance:</b><br>
 * Evaluated directly inside Spring Security's {@link org.springframework.security.oauth2.jwt.JwtDecoder}
 * pipeline before claim deserialization or principal extraction occurs. Discarding invalid tokens at
 * this stage avoids unnecessary object allocations and cryptographic operations for unauthorized clients.
 * </p>
 * <p>
 * <b>Defense-in-Depth for Anonymous Tokens:</b><br>
 * Although {@link SupabaseUserPrincipal}'s canonical constructor independently rejects anonymous sessions,
 * validating {@code is_anonymous == false} at the decoder level ensures malicious or unverified anonymous
 * Supabase sessions are immediately rejected with an RFC 6750 {@code invalid_token} error.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> OAuth2 JWT Validator</li>
 *   <li><b>Interacts with:</b> {@link SecurityConfig}, {@link com.chainsleuth.core.config.SecurityProperties}</li>
 * </ul>
 */
public class SupabaseAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    /**
     * Constructs the validator with the target expected audience claim.
     *
     * @param expectedAudience audience string configured via properties (typically "authenticated")
     */
    public SupabaseAudienceValidator(String expectedAudience) {
        if (expectedAudience == null || expectedAudience.isBlank()) {
            throw new IllegalArgumentException("Expected audience cannot be null or blank");
        }
        this.expectedAudience = expectedAudience.trim();
    }

    /**
     * Validates that the provided JWT specifies the expected audience and does not represent an anonymous session.
     *
     * @param jwt decoded JSON Web Token
     * @return {@link OAuth2TokenValidatorResult#success()} if compliant; failure with {@link OAuth2Error} otherwise
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        // CHECK 1: Audience claim validation
        List<String> audiences = jwt.getAudience();
        if (audiences == null || audiences.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Missing aud claim", null)
            );
        }

        if (!audiences.contains(this.expectedAudience)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            OAuth2ErrorCodes.INVALID_TOKEN,
                            "JWT aud claim does not match. Expected: " + this.expectedAudience,
                            null
                    )
            );
        }

        // CHECK 2: Anonymous session rejection
        Object isAnonClaim = jwt.getClaim("is_anonymous");
        boolean isAnonymous = false;
        if (isAnonClaim instanceof Boolean b) {
            isAnonymous = b;
        } else if (isAnonClaim instanceof String s) {
            isAnonymous = Boolean.parseBoolean(s);
        }

        if (isAnonymous) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            OAuth2ErrorCodes.INVALID_TOKEN,
                            "Anonymous users are not permitted to access ChainSleuth",
                            null
                    )
            );
        }

        return OAuth2TokenValidatorResult.success();
    }
}
