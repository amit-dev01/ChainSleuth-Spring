package com.chainsleuth.core.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Custom parameter injection meta-annotation that binds the authenticated {@link SupabaseUserPrincipal}
 * directly into Spring MVC and WebFlux controller handler methods.
 * <p>
 * <b>SpEL Expression Architecture:</b><br>
 * By default, Spring's {@link AuthenticationPrincipal} resolves the primary principal from the current
 * security context. In a standard OAuth2 Resource Server configuration, that principal is the raw
 * {@link org.springframework.security.oauth2.jwt.Jwt} object. Because {@link ChainSleuthJwtConverter}
 * attaches the parsed domain principal via {@link org.springframework.security.authentication.AbstractAuthenticationToken#setDetails(Object)},
 * this annotation specifies {@code expression = "details"} to instruct Spring Security's SpEL engine
 * to pull the {@link SupabaseUserPrincipal} from the token's details payload.
 * </p>
 * <p>
 * <b>Behavior with Non-Standard Tokens:</b><br>
 * If the current request is unauthenticated or the authentication details are not of type
 * {@link SupabaseUserPrincipal}, the annotated parameter will resolve to {@code null}.
 * Endpoints requiring mandatory principals should be protected by {@code @PreAuthorize("isAuthenticated()")}.
 * </p>
 * <p>
 * <b>Example Controller Usage:</b>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/v1/cases")
 * public class CaseController {
 *
 *     @PostMapping
 *     @PreAuthorize("hasAnyRole('INVESTIGATOR', 'SENIOR_INVESTIGATOR')")
 *     public ResponseEntity<CaseResponse> createCase(
 *             @CurrentUser SupabaseUserPrincipal user,
 *             @Valid @RequestBody CreateCaseRequest request) {
 *
 *         log.info("User {} from org {} initiating case", user.userId(), user.organizationId());
 *         return ResponseEntity.ok(caseService.create(user, request));
 *     }
 * }
 * }</pre>
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> Controller Parameter Injection Annotation</li>
 *   <li><b>Interacts with:</b> {@link SupabaseUserPrincipal}, {@link ChainSleuthJwtConverter}</li>
 * </ul>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "details")
public @interface CurrentUser {
}
