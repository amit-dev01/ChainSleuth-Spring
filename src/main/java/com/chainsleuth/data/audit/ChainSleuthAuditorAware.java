package com.chainsleuth.data.audit;

import com.chainsleuth.core.security.SecurityUtils;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/**
 * Auditor provider component that extracts the current authenticated user's UUID from the security context.
 * <p>
 * Implements Spring Data's {@link AuditorAware} contract to populate {@code @CreatedBy} and
 * {@code @LastModifiedBy} annotations automatically during JPA lifecycle transitions.
 * </p>
 * <p>
 * <b>Virtual Threads &amp; SecurityContext Propagation:</b><br>
 * With virtual threads, {@link org.springframework.security.core.context.SecurityContextHolder} uses
 * the {@code InheritableThreadLocal} strategy (configured by Spring Boot 4). This means the security context
 * IS available in virtual thread child tasks spawned from an authenticated request thread, making
 * {@link AuditorAware} work correctly in async traversal tasks.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Security-Aware JPA Auditor Provider</li>
 *   <li><b>Interacts with:</b> {@link AuditableEntity}, {@link SecurityUtils}</li>
 * </ul>
 */
@Component("auditorAware")
public class ChainSleuthAuditorAware implements AuditorAware<UUID> {

    private static final Logger log = LoggerFactory.getLogger(ChainSleuthAuditorAware.class);

    /**
     * Default no-argument constructor.
     */
    public ChainSleuthAuditorAware() {
    }

    /**
     * Resolves the current auditor's user UUID from the active security context.
     *
     * @return {@link Optional} containing the authenticated user's UUID, or {@link Optional#empty()}
     *         if executing in an unauthenticated or system context
     */
    @Override
    public Optional<UUID> getCurrentAuditor() {
        Optional<UUID> userIdOpt = SecurityUtils.getCurrentUserId();

        if (userIdOpt.isPresent()) {
            return userIdOpt;
        }

        // Spring Data JPA handles Optional.empty() gracefully:
        // @CreatedBy and @LastModifiedBy stay null for system-initiated operations.
        log.trace("No authenticated user present in SecurityContext for auditing; returning empty auditor");
        return Optional.empty();
    }
}
