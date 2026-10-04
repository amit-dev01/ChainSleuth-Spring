package com.chainsleuth.core.exception;

import java.util.UUID;

/**
 * High-severity domain exception raised when digital evidence integrity verification fails.
 * <p>
 * Signals that recomputed cryptographic hashes do not match historical seal records,
 * indicating possible evidence tampering or storage corruption under Federal Rules of Evidence
 * (FRE) Rule 902(14).
 * </p>
 * <p>
 * <b>Security Constraint:</b><br>
 * The {@code expectedHash} and {@code actualHash} fields are retained strictly for internal auditing
 * and secure logging. They must NEVER be exposed in RFC 9457 ProblemDetail payloads to prevent
 * unauthorized state discovery.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Evidence Integrity Exception</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthException}</li>
 * </ul>
 */
public final class EvidenceTamperedException extends ChainSleuthException {

    private final UUID caseId;
    private final String expectedHash;
    private final String actualHash;

    /**
     * Constructs a tamper detection failure for an active case.
     *
     * @param caseId       unique case identifier
     * @param expectedHash recorded canonical SHA-256 seal
     * @param actualHash   recomputed SHA-256 seal from current evidence state
     */
    public EvidenceTamperedException(UUID caseId, String expectedHash, String actualHash) {
        super(
                ErrorCode.TAMPER_DETECTED,
                "Evidence integrity check FAILED for case " + caseId
                        + ". The stored evidence hash does not match the recomputed hash. "
                        + "Evidence may have been altered. This incident has been logged."
        );
        this.caseId = caseId;
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
    }

    public UUID caseId() {
        return caseId;
    }

    public String expectedHash() {
        return expectedHash;
    }

    public String actualHash() {
        return actualHash;
    }
}
