package com.chainsleuth.core.security;

import com.chainsleuth.core.config.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Master Spring Security 7 configuration establishing an OAuth2 Stateless Resource Server.
 * <p>
 * Configures fine-grained authorization endpoints, RFC 7807 Problem Detail error rendering for 401/403
 * status codes, strict CORS policies, and explicit Nimbus JWT decoding coupled with Supabase audience
 * and non-anonymous token validators.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1B — Authentication &amp; Multi-Tenant Security</li>
 *   <li><b>Platform Component:</b> Core Security Filter Chain &amp; OAuth2 Architecture</li>
 *   <li><b>Interacts with:</b> {@link ChainSleuthJwtConverter}, {@link SupabaseAudienceValidator}, {@link SecurityProperties}</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final SecurityProperties securityProperties;
    private final ChainSleuthJwtConverter jwtConverter;
    private final SupabasePrincipalExtractor principalExtractor;
    private final ObjectMapper objectMapper;
    private final String jwksUri;

    /**
     * Constructs the central security configuration using explicit constructor injection.
     *
     * @param securityProperties custom security properties binding CORS and JWT expectations
     * @param jwtConverter       custom converter translating JWT claims into {@link SupabaseUserPrincipal}
     * @param principalExtractor extractor parsing Supabase JWT metadata
     * @param objectMapper       Jackson serializer for emitting RFC 7807 problem details
     * @param jwksUri            Supabase JWKS URI configured in application properties
     */
    public SecurityConfig(
            SecurityProperties securityProperties,
            ChainSleuthJwtConverter jwtConverter,
            SupabasePrincipalExtractor principalExtractor,
            ObjectMapper objectMapper,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwksUri
    ) {
        this.securityProperties = securityProperties;
        this.jwtConverter = jwtConverter;
        this.principalExtractor = principalExtractor;
        this.objectMapper = objectMapper;
        this.jwksUri = jwksUri;
    }

    /**
     * Configures the primary {@link SecurityFilterChain} regulating HTTP request dispatch,
     * session policy, CORS/CSRF configurations, and OAuth2 JWT authentication.
     *
     * @param http Spring Security HTTP DSL builder
     * @return assembled {@link SecurityFilterChain}
     * @throws Exception if filter chain assembly fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security 7 OAuth2 Resource Server filter chain...");

        // a) CSRF — disable completely:
        // Stateless REST API — no session cookies, CSRF is irrelevant.
        // JWTs in the Authorization header are inherently CSRF-safe.
        http.csrf(csrf -> csrf.disable());

        // b) CORS — configure dynamically from SecurityProperties:
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // c) Session Management — strictly STATELESS:
        // JWT is self-contained. No server-side session needed.
        // Virtual threads + stateless architecture = maximum scalability.
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // d) Authorization Rules (ordered from most specific to general):
        http.authorizeHttpRequests(auth -> auth
                // Public: Supabase handles auth callbacks (pass-through)
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                // Public: Liveness & readiness probes for Kubernetes/Docker health checks
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Public: Evidence verification (law enforcement can verify signed reports without login)
                .requestMatchers(HttpMethod.GET, "/verify/**").permitAll()
                // Admin only: Actuator internals (metrics, loggers, prometheus)
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Admin only: Administrative management endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // All other API calls must be authenticated
                .anyRequest().authenticated()
        );

        // e) OAuth2 Resource Server — JWT mode:
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .decoder(supabaseJwtDecoder())
                        .jwtAuthenticationConverter(jwtConverter)
                )
                .authenticationEntryPoint(authenticationEntryPoint())
        );

        // f) Exception handling:
        http.exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler())
        );

        return http.build();
    }

    /**
     * Builds and exposes the Nimbus-backed {@link JwtDecoder} using the remote Supabase JWKS URI
     * and attaching strict custom token validators.
     *
     * @return configured {@link JwtDecoder}
     */
    @Bean
    public JwtDecoder supabaseJwtDecoder() {
        log.info("Building NimbusJwtDecoder for JWKS endpoint: {}", jwksUri);

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(this.jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new SupabaseAudienceValidator(securityProperties.jwt().audience())
        );

        decoder.setJwtValidator(validators);
        return decoder;
    }

    /**
     * Generates a {@link CorsConfigurationSource} populated from {@link SecurityProperties}.
     *
     * @return registered {@link CorsConfigurationSource}
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(securityProperties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Custom {@link AuthenticationEntryPoint} writing RFC 7807 ProblemDetail payloads upon 401 Unauthenticated.
     *
     * @return lambda {@link AuthenticationEntryPoint}
     */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Unauthorized access attempt to {}: {}", request.getRequestURI(), authException.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            Map<String, Object> problem = Map.of(
                    "type", "urn:chainsleuth:error:auth:unauthenticated",
                    "title", "Unauthenticated",
                    "status", HttpServletResponse.SC_UNAUTHORIZED,
                    "detail", "A valid Bearer JWT token is required.",
                    "instance", request.getRequestURI()
            );

            response.getWriter().write(objectMapper.writeValueAsString(problem));
        };
    }

    /**
     * Custom {@link AccessDeniedHandler} writing RFC 7807 ProblemDetail payloads upon 403 Forbidden.
     *
     * @return lambda {@link AccessDeniedHandler}
     */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Forbidden access attempt to {}: {}", request.getRequestURI(), accessDeniedException.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            Map<String, Object> problem = Map.of(
                    "type", "urn:chainsleuth:error:auth:forbidden",
                    "title", "Forbidden",
                    "status", HttpServletResponse.SC_FORBIDDEN,
                    "detail", "You do not have the required role to access this resource.",
                    "instance", request.getRequestURI()
            );

            response.getWriter().write(objectMapper.writeValueAsString(problem));
        };
    }
}
