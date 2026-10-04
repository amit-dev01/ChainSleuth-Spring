package com.chainsleuth.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Enumeration of application-level roles governing authorization across ChainSleuth.
 * <p>
 * Decoupled intentionally from Supabase's internal database {@code role} claim
 * (e.g. {@code "authenticated"}, {@code "anon"}, {@code "service_role"}).
 * The true application roles reside inside {@code app_metadata.app_role} of the JWT.
 * </p>
 * <p>
 * <b>Defense-in-depth:</b> The {@link #fromString(String)} resolver defaults securely
 * to {@link #INVESTIGATOR} for unknown or absent values rather than throwing exceptions,
 * preventing authentication denial-of-service while strictly restricting privilege.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> Security Authority Model</li>
 *   <li><b>Interacts with:</b> {@link SupabaseUserPrincipal}, {@link SupabasePrincipalExtractor}, {@link SecurityConfig}</li>
 * </ul>
 */
public enum AppRole {

    INVESTIGATOR("Investigator", "ROLE_INVESTIGATOR"),
    SENIOR_INVESTIGATOR("Senior Investigator", "ROLE_SENIOR_INVESTIGATOR"),
    SUPERVISOR("Supervisor", "ROLE_SUPERVISOR"),
    ADMIN("Administrator", "ROLE_ADMIN");

    private static final Logger log = LoggerFactory.getLogger(AppRole.class);

    private final String displayName;
    private final String springAuthority;

    /**
     * Constructs an application role with descriptive display label and Spring authority token.
     *
     * @param displayName     human-readable role title for reports and UI
     * @param springAuthority granted authority token adhering to the standard "ROLE_" prefix
     */
    AppRole(String displayName, String springAuthority) {
        this.displayName = displayName;
        this.springAuthority = springAuthority;
    }

    /**
     * Retrieves the human-readable display name for this role.
     *
     * @return display label
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retrieves the Spring Security authority representation (e.g., "ROLE_INVESTIGATOR").
     *
     * @return authority token
     */
    public String getSpringAuthority() {
        return springAuthority;
    }

    /**
     * Resolves an {@link AppRole} from a case-insensitive string value.
     * <p>
     * Implements graceful fallback to {@link #INVESTIGATOR} to maintain access
     * without compromising privilege if an invalid or unrecognized role string is passed.
     * </p>
     *
     * @param value raw role string from {@code app_metadata.app_role}
     * @return matching {@link AppRole} or {@link #INVESTIGATOR} as safe fallback
     */
    public static AppRole fromString(String value) {
        if (value == null || value.isBlank()) {
            log.warn("Null or blank role provided in token payload; defaulting safely to {}", INVESTIGATOR.name());
            return INVESTIGATOR;
        }

        String normalized = value.trim().toUpperCase();
        for (AppRole role : values()) {
            if (role.name().equals(normalized)) {
                return role;
            }
        }

        log.warn("Unrecognized app_role '{}' detected in JWT payload; applying fallback role {}", value, INVESTIGATOR.name());
        return INVESTIGATOR;
    }

    /**
     * Converts this role into a Spring Security {@link GrantedAuthority}.
     *
     * @return {@link SimpleGrantedAuthority} wrapped with the {@code ROLE_} prefix
     */
    public GrantedAuthority toGrantedAuthority() {
        return new SimpleGrantedAuthority(this.springAuthority);
    }
}
