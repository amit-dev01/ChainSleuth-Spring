package com.chainsleuth.core.config;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Enterprise Web3j configuration factory for EVM blockchain interaction.
 * <p>
 * Manages dedicated {@link Web3j} client singletons for Base and Ethereum networks,
 * configuring connection pools, read/write timeouts, and connection failure retry logic.
 * Exposes multi-chain resolution utilities and guarantees graceful connection shutdown
 * during container termination.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1 — Foundational Infrastructure &amp; Core Configuration</li>
 *   <li><b>Platform Component:</b> Blockchain RPC Gateway Configuration</li>
 *   <li><b>Owner:</b> Lead Blockchain Integration Engineer</li>
 * </ul>
 */
@Configuration
public class Web3jConfig {

    private static final Logger log = LoggerFactory.getLogger(Web3jConfig.class);

    private final ChainConfigProperties chainConfig;
    private Web3j baseWeb3jInstance;
    private Web3j ethereumWeb3jInstance;

    /**
     * Constructor injection for immutable chain configuration properties.
     *
     * @param chainConfig immutable chain configurations
     */
    public Web3jConfig(ChainConfigProperties chainConfig) {
        this.chainConfig = chainConfig;
    }

    /**
     * Builds and exposes the Web3j client for Base network RPC endpoints.
     *
     * @return configured {@link Web3j} instance for Base
     */
    @Bean("baseWeb3j")
    public Web3j baseWeb3j() {
        OkHttpClient httpClient = buildOkHttpClient();
        String rpcUrl = chainConfig.base().rpcUrl();
        log.info("Initializing Web3j Base client connection to {}", rpcUrl);
        this.baseWeb3jInstance = Web3j.build(new HttpService(rpcUrl, httpClient));
        return this.baseWeb3jInstance;
    }

    /**
     * Builds and exposes the Web3j client for Ethereum Mainnet RPC endpoints.
     *
     * @return configured {@link Web3j} instance for Ethereum
     */
    @Bean("ethereumWeb3j")
    public Web3j ethereumWeb3j() {
        OkHttpClient httpClient = buildOkHttpClient();
        String rpcUrl = chainConfig.ethereum().rpcUrl();
        log.info("Initializing Web3j Ethereum client connection to {}", rpcUrl);
        this.ethereumWeb3jInstance = Web3j.build(new HttpService(rpcUrl, httpClient));
        return this.ethereumWeb3jInstance;
    }

    /**
     * Resolves the appropriate Web3j instance dynamically based on the target EVM chain name.
     * Called by tracing services to avoid hardcoded chain dispatch logic.
     *
     * @param chain the chain identifier (e.g. "BASE", "ETHEREUM")
     * @return active {@link Web3j} instance
     * @throws IllegalArgumentException if the chain is unrecognized or non-EVM
     */
    public Web3j getWeb3j(String chain) {
        if (chain == null) {
            throw new IllegalArgumentException("Chain identifier cannot be null");
        }
        return switch (chain.trim().toUpperCase()) {
            case "BASE"     -> baseWeb3jInstance != null ? baseWeb3jInstance : baseWeb3j();
            case "ETHEREUM" -> ethereumWeb3jInstance != null ? ethereumWeb3jInstance : ethereumWeb3j();
            default -> throw new IllegalArgumentException(
                "Unsupported Web3j chain: " + chain + ". Supported EVM networks: BASE, ETHEREUM");
        };
    }

    /**
     * Constructs a resilient OkHttpClient with enterprise timeouts and automatic retry policies.
     *
     * @return configured {@link OkHttpClient}
     */
    private OkHttpClient buildOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * Graceful teardown hook to terminate active Web3j scheduled executors and connection pools.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Initiating graceful shutdown of Web3j RPC connections...");
        if (baseWeb3jInstance != null) {
            try {
                baseWeb3jInstance.shutdown();
                log.info("Base Web3j client shutdown successfully.");
            } catch (Exception e) {
                log.warn("Error during Base Web3j client shutdown: {}", e.getMessage());
            }
        }
        if (ethereumWeb3jInstance != null) {
            try {
                ethereumWeb3jInstance.shutdown();
                log.info("Ethereum Web3j client shutdown successfully.");
            } catch (Exception e) {
                log.warn("Error during Ethereum Web3j client shutdown: {}", e.getMessage());
            }
        }
    }
}
