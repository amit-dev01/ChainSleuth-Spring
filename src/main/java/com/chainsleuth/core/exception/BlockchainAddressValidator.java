package com.chainsleuth.core.exception;

import com.chainsleuth.core.model.ChainType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidBlockchainAddress} enforcing network-specific address syntax.
 * <p>
 * <b>Validation Rules:</b>
 * <ul>
 *   <li><b>EVM (Base, Ethereum):</b> Standard 0x prefix followed by exactly 40 hexadecimal characters (case-insensitive).
 *   <i>Note:</i> This validator tests syntactic structure; cryptographic EIP-55 mixed-case checksums must be validated
 *   via Web3j's {@code Keys.toChecksumAddress()} separately during transaction signing or deep tracing.</li>
 *   <li><b>Tron:</b> Base58Check encoding beginning with 'T' and spanning exactly 34 characters.</li>
 *   <li><b>Solana:</b> Base58 public key encoding spanning between 32 and 44 alphanumeric characters.</li>
 *   <li><b>Bitcoin:</b> Legacy P2PKH (1...), script hash P2SH (3...), or native SegWit Bech32 (bc1...).</li>
 * </ul>
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Address Syntax Validator</li>
 *   <li><b>Interacts with:</b> {@link ValidBlockchainAddress}, {@link ChainType}</li>
 * </ul>
 */
public class BlockchainAddressValidator implements ConstraintValidator<ValidBlockchainAddress, String> {

    private ChainType chainType;

    @Override
    public void initialize(ValidBlockchainAddress annotation) {
        this.chainType = annotation.chain();
    }

    @Override
    public boolean isValid(String address, ConstraintValidatorContext context) {
        if (address == null || address.isBlank()) {
            // Null or blank values must be handled by @NotNull / @NotBlank
            return true;
        }

        return switch (chainType) {
            case BASE, ETHEREUM -> isValidEvmAddress(address);
            case TRON           -> isValidTronAddress(address);
            case SOLANA         -> isValidSolanaAddress(address);
            case BITCOIN        -> isValidBitcoinAddress(address);
        };
    }

    /**
     * Validates EVM hexadecimal address syntax (0x + 40 hex chars).
     */
    private boolean isValidEvmAddress(String address) {
        return address.matches("^0[xX][0-9a-fA-F]{40}$");
    }

    /**
     * Validates Tron Base58Check address format starting with 'T' and 34 characters total.
     */
    private boolean isValidTronAddress(String address) {
        if (!address.startsWith("T")) {
            return false;
        }
        return address.matches("^T[1-9A-HJ-NP-Za-km-z]{33}$");
    }

    /**
     * Validates Solana Base58 public key format (32 to 44 Base58 characters).
     */
    private boolean isValidSolanaAddress(String address) {
        return address.matches("^[1-9A-HJ-NP-Za-km-z]{32,44}$");
    }

    /**
     * Validates Bitcoin address formats across P2PKH, P2SH, and native SegWit (Bech32).
     */
    private boolean isValidBitcoinAddress(String address) {
        // P2PKH: starts with 1, 25-34 chars Base58
        if (address.matches("^1[1-9A-HJ-NP-Za-km-z]{24,33}$")) {
            return true;
        }
        // P2SH: starts with 3, 25-34 chars Base58
        if (address.matches("^3[1-9A-HJ-NP-Za-km-z]{24,33}$")) {
            return true;
        }
        // Bech32 (native SegWit): starts with bc1
        if (address.matches("^bc1[ac-hj-np-z02-9]{6,87}$")) {
            return true;
        }
        return false;
    }
}
