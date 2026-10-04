/*
 *  ██████╗██╗  ██╗ █████╗ ██╗███╗   ██╗███████╗██╗     ███████╗██╗   ██╗████████╗██╗  ██╗
 *  ██╔════╝██║  ██║██╔══██╗██║████╗  ██║██╔════╝██║     ██╔════╝██║   ██║╚══██╔══╝██║  ██║
 *  ██║     ███████║███████║██║██╔██╗ ██║███████╗██║     █████╗  ██║   ██║   ██║   ███████║
 *  ██║     ██╔══██║██╔══██║██║██║╚██╗██║╚════██║██║     ██╔══╝  ██║   ██║   ██║   ██╔══██║
 *  ╚██████╗██║  ██║██║  ██║██║██║ ╚████║███████║███████╗███████╗╚██████╔╝   ██║   ██║  ██║
 *   ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝╚══════╝╚══════╝╚══════╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝
 *
 *  ChainSleuth Core — Enterprise Blockchain Forensics Platform
 *  Stack: Java 25 LTS | Spring Boot 4.1.1 | Spring AI 2.0.1 | Web3j 6.0.0
 *  Author: Antigravity Systems
 */

package com.chainsleuth;

import com.chainsleuth.core.config.ChainConfigProperties;
import com.chainsleuth.core.config.EvidenceProperties;
import com.chainsleuth.core.config.MlSidecarProperties;
import com.chainsleuth.core.config.SecurityProperties;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Entry point and bootstrap harness for the ChainSleuth Core platform.
 * <p>
 * Responsible for initializing the Spring Boot container, enabling immutable configuration
 * property bindings across all enterprise subsystems (multi-chain RPCs, gRPC ML sidecar,
 * cryptographic evidence verification, and OAuth2 security), and logging runtime topology
 * upon startup.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1 — Foundational Infrastructure &amp; Core Configuration</li>
 *   <li><b>Platform Component:</b> Core Application Bootstrapper</li>
 *   <li><b>Owner:</b> Principal Systems Architect &amp; Core Platform Team</li>
 * </ul>
 */
@SpringBootApplication
@EnableConfigurationProperties({
    ChainConfigProperties.class,
    MlSidecarProperties.class,
    SecurityProperties.class,
    EvidenceProperties.class
})
public class ChainSleuthApplication {

    private static final Logger log = LoggerFactory.getLogger(ChainSleuthApplication.class);

    private final Environment environment;

    /**
     * Constructor injection for Spring core environment.
     *
     * @param environment the runtime Spring environment abstraction
     */
    public ChainSleuthApplication(Environment environment) {
        this.environment = environment;
    }

    /**
     * Primary application launch routine.
     *
     * @param args runtime CLI arguments passed to JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(ChainSleuthApplication.class, args);
    }

    /**
     * Listens for the {@link ApplicationReadyEvent} to log operational parameters,
     * confirming active profiles, port bindings, and preview feature availability.
     *
     * @param event the lifecycle application ready event
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        String appName = environment.getProperty("spring.application.name", "chainsleuth-core");
        String serverPort = environment.getProperty("server.port", "8080");
        String[] activeProfiles = environment.getActiveProfiles();
        String profilesDisplay = activeProfiles.length > 0
                ? Arrays.toString(activeProfiles)
                : "[default]";

        log.info("================================================================================");
        log.info(" ChainSleuth Core Forensics Engine initialized and operational");
        log.info(" Application Name : {}", appName);
        log.info(" Active Profiles  : {}", profilesDisplay);
        log.info(" Server Listening : http://localhost:{}", serverPort);
        log.info(" Java Runtime     : {} (Temurin 25 LTS, JEP 505 Preview Enabled)", Runtime.version());
        log.info("================================================================================");
    }
}
