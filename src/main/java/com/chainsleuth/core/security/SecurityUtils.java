package com.chainsleuth.core.security;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Thread-safe utility methods for interrogating the active security context across the application.
 * <p>
 * <b>Usage Context:</b>
 * <ul>
 *   <li><b>{@link CurrentUser}:</b> Preferred for declarative controller method parameter injection.</li>
 *   <li><b>{@link SecurityUtils}:</b> Intended for business service implementations, domain event listeners,
 *   asynchronous task executions, and auditing routines where MVC parameter binding is unavailable.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Virtual Threads &amp; SecurityContext Propagation:</b><br>
 * By default, Spring Security uses a {@link ThreadLocal} strategy for {@link SecurityContextHolder}.
 * In Spring Boot 4 and Java 25, virtual threads spawned by the web container naturally inherit the
 * request's lifecycle. For explicit concurrent handoffs (such as Structured Concurrency scopes or custom
 * executor tasks), Spring Boot automatically coordinates context propagation when using delegating task
 * executors or {@code SecurityContextHolderStrategy}.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> Security Context Access Utilities</li>
 *   <li><b>Interacts with:</b> {@link SupabaseUserPrincipal}, {@link AppRole}</li>
 * </ul>
 */
public final class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    /**
     * Private constructor to suppress instantiation of static utility class.
     */
    private SecurityUtils() {
        throw new UnsupportedOperationException("SecurityUtils is a static utility class and cannot be instantiated");
    }

    /**
     * Retrieves the current authenticated {@link SupabaseUserPrincipal} if present in the security context.
     *
     * @return {@link Optional} containing the principal, or empty if unauthenticated or invalid token type
     */
    public static Optional<SupabaseUserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object details = jwtAuth.getDetails();
            if (details instanceof SupabaseUserPrincipal principal) {
                return Optional.of(principal);
            }
        }

        log.warn("Active authentication in SecurityContext is not a JwtAuthenticationToken with SupabaseUserPrincipal: {}",
                authentication.getClass().getName());
        return Optional.empty();
    }

    /**
     * Obtains the mandatory authenticated user principal or throws an exception.
     * Intended for protected service methods where an authenticated subject is strictly required.
     *
     * @return active {@link SupabaseUserPrincipal}
     * @throws IllegalStateException if no authenticated user is bound to the current context
     */
    public static SupabaseUserPrincipal requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in security context"));
    }

    /**
     * Resolves the unique identifier of the currently authenticated operator.
     *
     * @return {@link Optional} containing user's {@link UUID}, or empty if unauthenticated
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUser().map(SupabaseUserPrincipal::userId);
    }

    /**
     * Resolves the multi-tenant organization identifier for the currently authenticated operator.
     *
     * @return {@link Optional} containing the organization ID, or empty if not present or unauthenticated
     */
    public static Optional<String> getCurrentOrganizationId() {
        return getCurrentUser()
                .map(SupabaseUserPrincipal::organizationId)
                .filter(Objects::nonNull);
    }

    /**
     * Verifies if the currently authenticated operator holds a specified application role.
     *
     * @param role role to evaluate
     * @return {@code true} if operator holds the role; {@code false} otherwise
     */
    public static boolean hasCurrentUserRole(AppRole role) {
        return getCurrentUser()
                .map(u -> u.hasRole(role))
                .orElse(false);
    }

    /**
     * Convenience utility checking if the currently authenticated operator possesses administrator privileges.
     *
     * @return {@code true} if operator has {@link AppRole#ADMIN}; {@code false} otherwise
     */
    public static boolean isCurrentUserAdmin() {
        return hasCurrentUserRole(AppRole.ADMIN);
    }
}
