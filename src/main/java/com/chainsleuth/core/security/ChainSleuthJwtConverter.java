package com.chainsleuth.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Custom authentication converter implementing {@link Converter} to transform a raw {@link Jwt}
 * into a fully populated Spring Security {@link JwtAuthenticationToken}.
 * <p>
 * <b>Principal Storage in Details:</b><br>
 * By design, Spring Security's {@link JwtAuthenticationToken} sets the decoded {@link Jwt} as its
 * primary principal. Rather than losing the rich metadata or performing repeated JSON parsing across
 * service invocations, this converter extracts the type-safe {@link SupabaseUserPrincipal} and stores
 * it via {@link JwtAuthenticationToken#setDetails(Object)}.
 * </p>
 * <p>
 * <b>Downstream Consumption:</b><br>
 * Downstream controllers should avoid manually casting {@code authentication.getDetails()} and instead
 * utilize the custom {@link CurrentUser} parameter annotation, which leverages SpEL expressions to
 * resolve the principal seamlessly.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> Security Token Converter</li>
 *   <li><b>Interacts with:</b> {@link SupabasePrincipalExtractor}, {@link SupabaseUserPrincipal}, {@link SecurityConfig}, {@link CurrentUser}</li>
 * </ul>
 */
@Component
public class ChainSleuthJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(ChainSleuthJwtConverter.class);

    private final SupabasePrincipalExtractor principalExtractor;

    /**
     * Constructs the converter with the required claim extraction component.
     *
     * @param principalExtractor extractor responsible for building {@link SupabaseUserPrincipal}
     */
    public ChainSleuthJwtConverter(SupabasePrincipalExtractor principalExtractor) {
        this.principalExtractor = principalExtractor;
    }

    /**
     * Converts a verified {@link Jwt} into an authenticated {@link AbstractAuthenticationToken}.
     *
     * @param jwt verified JWT instance
     * @return populated {@link JwtAuthenticationToken} containing granted authorities and principal details
     * @throws JwtException if claim extraction or validation fails (converted to 401 by Spring Security)
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Step A: Extract domain principal
        SupabaseUserPrincipal principal = principalExtractor.extract(jwt);

        // Step B & C: Build standard Spring JwtAuthenticationToken using email as the principal name
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                jwt,
                principal.toGrantedAuthorities(),
                principal.email()
        );

        // Step D: Attach the typed principal to token details for controller and SpEL injection
        token.setDetails(principal);

        // Step E: Sanitized debug logging (strictly omitting PII such as email and userId)
        log.debug("JWT converted for user: role={}, orgId={}", principal.appRole(), principal.organizationId());

        return token;
    }
}
