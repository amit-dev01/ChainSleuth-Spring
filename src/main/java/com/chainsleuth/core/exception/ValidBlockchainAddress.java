package com.chainsleuth.core.exception;

import com.chainsleuth.core.model.ChainType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation constraint validating blockchain address formatting
 * against network-specific cryptographic character sets and lengths.
 * <p>
 * Supports EVM (Base, Ethereum), Tron, Solana, and Bitcoin addresses.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Blockchain Validation Constraint Annotation</li>
 *   <li><b>Interacts with:</b> {@link BlockchainAddressValidator}, {@link ChainType}</li>
 * </ul>
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BlockchainAddressValidator.class)
public @interface ValidBlockchainAddress {

    String message() default "Invalid blockchain address format for the specified chain";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Target blockchain network schema used for pattern validation.
     * Defaults to {@link ChainType#ETHEREUM} (standard EVM).
     *
     * @return blockchain network type
     */
    ChainType chain() default ChainType.ETHEREUM;
}
