package com.chainsleuth.core.model;

import java.util.Arrays;

/**
 * Enumeration representing the target blockchain architectures supported by ChainSleuth.
 * <p>
 * Encapsulates network metadata including human-readable display names, native currency
 * ticker symbols, EIP-155 / network IDs, and EVM compatibility flags.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1 — Foundational Infrastructure &amp; Core Configuration</li>
 *   <li><b>Platform Component:</b> Multi-Chain Domain Models</li>
 *   <li><b>Owner:</b> Lead Blockchain Integration Engineer</li>
 * </ul>
 */
public enum ChainType {
    BASE("Base", "ETH", 8453, true),
    ETHEREUM("Ethereum", "ETH", 1, true),
    TRON("Tron", "TRX", 195, false),        // 195 = TronGrid chain ID
    SOLANA("Solana", "SOL", null, false),
    BITCOIN("Bitcoin", "BTC", null, false);

    private final String displayName;
    private final String nativeSymbol;
    private final Integer chainId;          // null for non-EVM chains
    private final boolean evm;             // true if EVM-compatible

    /**
     * Initializes enum constant with blockchain specifications.
     *
     * @param displayName  human-readable network title
     * @param nativeSymbol primary native ticker symbol
     * @param chainId      network identifier or EIP-155 ID (null for non-EVM or dynamic chains)
     * @param evm          true if network implements Ethereum Virtual Machine semantics
     */
    ChainType(String displayName, String nativeSymbol, Integer chainId, boolean evm) {
        this.displayName = displayName;
        this.nativeSymbol = nativeSymbol;
        this.chainId = chainId;
        this.evm = evm;
    }

    /**
     * Retrieves the network display name.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retrieves the native currency symbol (e.g., "ETH", "SOL", "BTC").
     *
     * @return native currency symbol
     */
    public String getNativeSymbol() {
        return nativeSymbol;
    }

    /**
     * Retrieves the static network or chain identifier.
     *
     * @return numeric chain ID or null if unassigned
     */
    public Integer getChainId() {
        return chainId;
    }

    /**
     * Checks if the blockchain operates on the Ethereum Virtual Machine (EVM).
     *
     * @return {@code true} for EVM chains, {@code false} otherwise
     */
    public boolean isEvm() {
        return evm;
    }

    /**
     * Checks if a distinct numeric network ID is assigned to this chain.
     *
     * @return {@code true} if chainId is non-null
     */
    public boolean hasPredefinedChainId() {
        return chainId != null;
    }

    /**
     * Resolves a {@link ChainType} constant from a string identifier (case-insensitive).
     *
     * @param value chain name or ticker symbol
     * @return matched {@link ChainType}
     * @throws IllegalArgumentException if the provided string does not match any supported chain
     */
    public static ChainType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Chain identifier must not be null or blank");
        }
        String normalized = value.trim().toUpperCase();
        for (ChainType type : values()) {
            if (type.name().equals(normalized) || type.displayName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported chain identifier: '" + value + "'. Supported chains: " + Arrays.toString(values()));
    }
}
