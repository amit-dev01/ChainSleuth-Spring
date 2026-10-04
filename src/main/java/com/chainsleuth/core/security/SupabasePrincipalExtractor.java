package com.chainsleuth.core.security;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Component responsible for translating decoded Spring Security {@link Jwt} tokens
 * into validated, strongly-typed {@link SupabaseUserPrincipal} instances.
 * <p>
 * Decodes Supabase-specific metadata hierarchies including {@code app_metadata} nested
 * claims, tenant partitioning boundaries, and session tracking identifiers with strict
 * exception sanitization.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> Security Token Mapping &amp; Extraction</li>
 *   <li><b>Interacts with:</b> {@link SupabaseUserPrincipal}, {@link AppRole}, {@link ChainSleuthJwtConverter}</li>
 * </ul>
 */
@Component
public class SupabasePrincipalExtractor {

    private static final Logger log = LoggerFactory.getLogger(SupabasePrincipalExtractor.class);

    /**
     * Default no-argument constructor for Spring component scanning.
     */
    public SupabasePrincipalExtractor() {
    }

    /**
     * Extracts and validates user claims from a decoded JWT.
     *
     * @param jwt decoded JSON Web Token
     * @return validated {@link SupabaseUserPrincipal}
     * @throws JwtException if required claims are missing or malformed
     */
    public SupabaseUserPrincipal extract(Jwt jwt) {
        try {
            // STEP A: Extract sub claim as UUID
            String subject = jwt.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new JwtException("Missing required 'sub' claim in JWT payload");
            }
            UUID userId;
            try {
                userId = UUID.fromString(subject);
            } catch (IllegalArgumentException e) {
                throw new JwtException("Invalid sub claim: not a valid UUID: " + subject, e);
            }

            // STEP B: Extract email claim
            Object emailObj = jwt.getClaim("email");
            if (emailObj == null) {
                throw new JwtException("Missing required email claim in JWT payload");
            }
            String email = emailObj.toString().trim();
            if (email.isBlank()) {
                throw new JwtException("Required email claim is blank in JWT payload");
            }

            // STEP C: Extract app_metadata map
            Object appMetaObj = jwt.getClaim("app_metadata");
            Map<String, Object> appMetadata;
            if (appMetaObj instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) rawMap;
                appMetadata = casted;
            } else {
                log.warn("app_metadata missing from JWT — defaulting to INVESTIGATOR role");
                appMetadata = Map.of();
            }

            // STEP D: Extract app_role from app_metadata
            Object roleObj = appMetadata.get("app_role");
            String rawRole = roleObj != null ? roleObj.toString() : null;
            AppRole appRole = AppRole.fromString(rawRole);

            // STEP E: Extract organization_id from app_metadata
            Object orgObj = appMetadata.get("organization_id");
            String organizationId = null;
            if (orgObj != null) {
                String candidate = orgObj.toString().trim();
                if (!candidate.isEmpty()) {
                    organizationId = candidate;
                }
            }

            // STEP F: Extract session_id
            Object sessionObj = jwt.getClaim("session_id");
            String sessionId = "UNKNOWN";
            if (sessionObj != null) {
                String candidate = sessionObj.toString().trim();
                if (!candidate.isEmpty()) {
                    sessionId = candidate;
                }
            }

            // STEP G: Extract is_anonymous claim
            Object isAnonObj = jwt.getClaim("is_anonymous");
            boolean isAnonymous = false;
            if (isAnonObj instanceof Boolean b) {
                isAnonymous = b;
            } else if (isAnonObj instanceof String s) {
                isAnonymous = Boolean.parseBoolean(s);
            }

            // STEP H: Build and return SupabaseUserPrincipal
            SupabaseUserPrincipal principal = new SupabaseUserPrincipal(
                    userId,
                    email,
                    appRole,
                    organizationId,
                    sessionId,
                    isAnonymous
            );

            log.debug("Extracted principal: userId={}, role={}, orgId={}",
                    principal.userId(), principal.appRole(), principal.organizationId());

            return principal;

        } catch (IllegalArgumentException | ClassCastException e) {
            log.error("Failed to extract principal from JWT: {}", e.getMessage());
            throw new JwtException("Failed to extract principal from JWT claims: " + e.getMessage(), e);
        }
    }
}
