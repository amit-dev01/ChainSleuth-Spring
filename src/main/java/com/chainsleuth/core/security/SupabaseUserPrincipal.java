package com.chainsleuth.core.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

/**
 * Immutable, type-safe security principal representing an authenticated ChainSleuth operator.
 * <p>
 * Implemented as a Java 25 record, ensuring thread-safety and zero-allocation mutability across
 * virtual thread execution contexts. Captures multi-tenant boundaries ({@code organizationId})
 * and security constraints directly from Supabase JWT claims.
 * </p>
 * <p>
 * <b>Multi-Tenancy &amp; Anonymous Users:</b>
 * <ul>
 *   <li>{@code organizationId} is nullable to support enterprise {@link AppRole#ADMIN} users whose oversight spans all tenants.</li>
 *   <li>The canonical constructor enforces rejection of anonymous tokens as a defense-in-depth measure.</li>
 * </ul>
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> Domain Security Principal</li>
 *   <li><b>Interacts with:</b> {@link SupabasePrincipalExtractor}, {@link ChainSleuthJwtConverter}, {@link CurrentUser}, {@link SecurityUtils}</li>
 * </ul>
 *
 * @param userId         unique user identifier extracted from the JWT {@code sub} claim
 * @param email          verified user email address from the {@code email} claim
 * @param appRole        application-level role extracted from {@code app_metadata.app_role}
 * @param organizationId law enforcement or financial tenant identifier (nullable for global admins)
 * @param sessionId      Supabase session tracking identifier
 * @param isAnonymous    flag indicating anonymous status (must strictly be false)
 */
public record SupabaseUserPrincipal(
        UUID userId,
        String email,
        AppRole appRole,
        String organizationId,
        String sessionId,
        boolean isAnonymous
) {

    /**
     * Canonical constructor enforcing structural invariants and security policies.
     *
     * @throws IllegalArgumentException if {@code userId} is null or {@code email} is blank
     * @throws SecurityException        if {@code isAnonymous} evaluates to true
     */
    public SupabaseUserPrincipal {
        if (userId == null) {
            throw new IllegalArgumentException("User identifier (sub) must not be null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email claim must not be null or blank");
        }
        if (appRole == null) {
            appRole = AppRole.INVESTIGATOR;
        }
        if (isAnonymous) {
            throw new SecurityException("Anonymous users are strictly prohibited from accessing ChainSleuth");
        }
    }

    /**
     * Checks whether this principal possesses a specific application role.
     *
     * @param role target role to verify
     * @return {@code true} if assigned role matches; {@code false} otherwise
     */
    public boolean hasRole(AppRole role) {
        return this.appRole == role;
    }

    /**
     * Evaluates if this principal holds any of the specified application roles.
     *
     * @param roles varargs array of target roles
     * @return {@code true} if principal possesses at least one matching role
     */
    public boolean hasAnyRole(AppRole... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        return Arrays.asList(roles).contains(this.appRole);
    }

    /**
     * Compares this principal's organization with a target organization identifier.
     *
     * @param otherOrgId organization ID to evaluate
     * @return {@code true} if non-null and identical; {@code false} if either is null or mismatched
     */
    public boolean isSameOrganization(String otherOrgId) {
        if (this.organizationId == null || otherOrgId == null) {
            return false;
        }
        return this.organizationId.equals(otherOrgId.trim());
    }

    /**
     * Converts the principal's active role into a collection of Spring Security authorities.
     *
     * @return singleton list of {@link GrantedAuthority}
     */
    public Collection<GrantedAuthority> toGrantedAuthorities() {
        return List.of(this.appRole.toGrantedAuthority());
    }
}
