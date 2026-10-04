package com.chainsleuth.core.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * Enterprise security audit listener capturing authentication and authorization events.
 * <p>
 * Emits standardized, high-integrity audit events to SLF4J structured logs for ingestion
 * into Security Operations Center (SOC) pipelines and Security Information and Event Management
 * (SIEM) platforms, such as Elasticsearch/Logstash (ELK), Splunk, and Google Cloud Logging.
 * </p>
 * <p>
 * <b>Security Guarantees:</b>
 * <ul>
 *   <li>PII Protection: Email addresses and passwords are systematically omitted from all log streams.</li>
 *   <li>Log Injection Defense: All runtime inputs are sanitized against CRLF characters to prevent spoofing.</li>
 * </ul>
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> Security Event &amp; Audit Pipeline</li>
 *   <li><b>Interacts with:</b> {@link SupabaseUserPrincipal}, {@link AppRole}, {@link SecurityConfig}</li>
 * </ul>
 */
@Component
public class SecurityAuditListener {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditListener.class);

    /**
     * Handles successful authentication events emitted by the OAuth2 Resource Server.
     *
     * @param event successful authentication lifecycle event
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String userId = "(unknown)";
        String role = "(unknown)";
        String orgId = "(none)";

        if (auth != null && auth.getDetails() instanceof SupabaseUserPrincipal principal) {
            userId = principal.userId().toString();
            role = principal.appRole().name();
            orgId = principal.organizationId() != null ? principal.organizationId() : "(none)";
        }

        String ip = extractIpAddress(auth);

        // Sanitize all user-supplied values before logging to prevent log injection.
        // Replace newlines and carriage returns in any string extracted from the request or JWT before including in log messages.
        log.info("AUTH_SUCCESS | userId={} | role={} | orgId={} | ip={}",
                sanitize(userId), sanitize(role), sanitize(orgId), sanitize(ip));
    }

    /**
     * Handles authentication failures such as expired tokens, signature mismatches, or malformed claims.
     *
     * @param event authentication failure event
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String reason = event.getException() != null ? event.getException().getMessage() : "Unknown authentication error";
        String ip = extractIpAddress(event.getAuthentication());
        String exceptionType = event.getException() != null ? event.getException().getClass().getSimpleName() : "None";

        // Sanitize all user-supplied values before logging to prevent log injection.
        // Replace newlines and carriage returns in any string extracted from the request or JWT before including in log messages.
        log.warn("AUTH_FAILURE | reason={} | ip={} | exceptionType={}",
                sanitize(reason), sanitize(ip), sanitize(exceptionType));
    }

    /**
     * Handles authorization rejection events when authenticated users attempt accessing forbidden resources.
     *
     * @param event authorization denied lifecycle event
     */
    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        String userId = "(unauthenticated)";
        String role = "(none)";

        Supplier<Authentication> authSupplier = event.getAuthentication();
        if (authSupplier != null) {
            Authentication auth = authSupplier.get();
            if (auth != null && auth.getDetails() instanceof SupabaseUserPrincipal principal) {
                userId = principal.userId().toString();
                role = principal.appRole().name();
            }
        }

        String targetPath = resolvePath(event.getObject());

        // Sanitize all user-supplied values before logging to prevent log injection.
        // Replace newlines and carriage returns in any string extracted from the request or JWT before including in log messages.
        log.warn("AUTHZ_DENIED | userId={} | path={} | role={}",
                sanitize(userId), sanitize(targetPath), sanitize(role));
    }

    /**
     * Extracts client remote IP address from the authentication details payload if present.
     *
     * @param auth authentication object
     * @return client remote IP address or fallback token
     */
    private String extractIpAddress(Authentication auth) {
        if (auth != null && auth.getDetails() instanceof WebAuthenticationDetails webDetails) {
            return webDetails.getRemoteAddress();
        }
        return "UNKNOWN_IP";
    }

    /**
     * Resolves target URI from authorization context objects.
     *
     * @param secureObject secured invocation target object
     * @return target endpoint path
     */
    private String resolvePath(Object secureObject) {
        if (secureObject instanceof RequestAuthorizationContext context) {
            return context.getRequest().getRequestURI();
        } else if (secureObject instanceof HttpServletRequest request) {
            return request.getRequestURI();
        } else if (secureObject instanceof FilterInvocation fi) {
            return fi.getRequestUrl();
        } else if (secureObject != null) {
            return secureObject.toString();
        }
        return "(unknown-target)";
    }

    /**
     * Sanitizes inputs to neutralize Log Injection (CWE-117) vulnerabilities.
     *
     * @param input raw input string
     * @return sanitized string capped at 200 characters
     */
    private String sanitize(String input) {
        if (input == null) {
            return "(null)";
        }
        String cleaned = input.replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();
        return cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned;
    }
}
