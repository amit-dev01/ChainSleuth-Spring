package com.chainsleuth.data.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Enterprise JPA and transaction management configuration for ChainSleuth.
 * <p>
 * <b>AuditorAware Bean Reference:</b><br>
 * The {@code @EnableJpaAuditing} annotation explicitly specifies {@code auditorAwareRef = "auditorAware"}
 * by bean name rather than relying on type resolution. This breaks potential circular dependencies during
 * early container startup, ensuring that the {@link com.chainsleuth.data.audit.ChainSleuthAuditorAware}
 * component and its security infrastructure dependencies are wired cleanly before entity listeners initialize.
 * </p>
 * <p>
 * <b>JSONB ObjectMapper Configuration:</b><br>
 * Hibernate 7 natively serializes and deserializes {@code @JdbcTypeCode(SqlTypes.JSON)} JSONB columns
 * using Jackson. This configuration establishes a calibrated {@link ObjectMapper} bean with
 * {@link JavaTimeModule} registered, timestamp writing disabled (preserving ISO-8601 string formatting),
 * and unknown property failures suppressed. Aligning the persistence ObjectMapper with the application's
 * web formatting guarantees consistent date serialization across REST APIs and PostgreSQL JSONB stores.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> JPA &amp; Auditing Configuration</li>
 *   <li><b>Interacts with:</b> {@link com.chainsleuth.data.audit.ChainSleuthAuditorAware}</li>
 * </ul>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(basePackages = "com.chainsleuth.data.repository")
@EnableTransactionManagement
public class JpaConfig {

    /**
     * Default no-argument constructor.
     */
    public JpaConfig() {
    }

    /**
     * Supplies the primary {@link ObjectMapper} configured for JSONB serialization and deserialization.
     * <p>
     * Picked up automatically by Hibernate's JSON type format mapper and Spring Boot's web layer.
     * </p>
     *
     * @return customized {@link ObjectMapper}
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
