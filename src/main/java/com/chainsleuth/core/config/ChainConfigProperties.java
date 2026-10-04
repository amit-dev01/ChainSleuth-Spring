package com.chainsleuth.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable configuration property mapping for supported blockchain networks.
 * <p>
 * Binds RPC endpoints, block explorer API keys, and network identifiers for EVM chains
 * (Base, Ethereum) and non-EVM chains (Tron, Solana, Bitcoin).
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1 — Foundational Infrastructure &amp; Core Configuration</li>
 *   <li><b>Platform Component:</b> Multi-Chain Network Configuration</li>
 *   <li><b>Owner:</b> Lead Blockchain Integration Engineer &amp; Core Platform Team</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "chainsleuth.chains")
@Validated
public record ChainConfigProperties(
    @Valid @NotNull EvmChainConfig base,
    @Valid @NotNull EvmChainConfig ethereum,
    @Valid @NotNull TronChainConfig tron,
    @Valid @NotNull SolanaChainConfig solana,
    @Valid @NotNull BitcoinChainConfig bitcoin
) {
    /**
     * Configuration parameters for EVM-compatible chains.
     *
     * @param rpcUrl         EVM node RPC endpoint URL
     * @param explorerApiUrl Explorer REST API URL (e.g., Etherscan/Basescan)
     * @param apiKey         Explorer API key for rate-limit quota
     * @param chainId        EIP-155 unique network chain identifier
     */
    public record EvmChainConfig(
        @NotBlank String rpcUrl,
        @NotBlank String explorerApiUrl,
        @NotBlank String apiKey,
        @NotNull Integer chainId
    ) {}

    /**
     * Configuration parameters for Tron network via TronGrid.
     *
     * @param tronGridUrl TronGrid REST API base endpoint URL
     * @param apiKey      TronGrid API authentication token
     */
    public record TronChainConfig(
        @NotBlank String tronGridUrl,
        @NotBlank String apiKey
    ) {}

    /**
     * Configuration parameters for Solana RPC infrastructure.
     *
     * @param rpcUrl Solana JSON-RPC HTTP gateway URL
     */
    public record SolanaChainConfig(
        @NotBlank String rpcUrl
    ) {}

    /**
     * Configuration parameters for Bitcoin network via Mempool.space API.
     *
     * @param mempoolApiUrl Mempool.space public or self-hosted API URL
     */
    public record BitcoinChainConfig(
        @NotBlank String mempoolApiUrl
    ) {}

    /**
     * Convenience resolution method to fetch EVM configuration by chain name.
     *
     * @param chain the chain identifier string (e.g. "BASE", "ETHEREUM")
     * @return the matching {@link EvmChainConfig}
     * @throws IllegalArgumentException if the chain is unrecognized or non-EVM
     */
    public EvmChainConfig getEvmConfig(String chain) {
        if (chain == null) {
            throw new IllegalArgumentException("Chain identifier cannot be null");
        }
        return switch (chain.trim().toUpperCase()) {
            case "BASE"     -> base;
            case "ETHEREUM" -> ethereum;
            default -> throw new IllegalArgumentException(
                "Unsupported EVM chain: " + chain + ". Supported: BASE, ETHEREUM");
        };
    }
}
