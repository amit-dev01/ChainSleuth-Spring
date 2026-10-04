package com.chainsleuth.core.exception;

import java.util.UUID;

/**
 * Domain exception thrown when a requested forensic entity cannot be located in storage.
 * <p>
 * Maps to HTTP 404 Not Found and provides entity classification for forensic records.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Entity Lookup Exception</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthException}</li>
 * </ul>
 */
public final class EntityNotFoundException extends ChainSleuthException {

    private final String entityType;
    private final String entityId;

    /**
     * Private constructor called by static factory methods.
     *
     * @param code       specific domain error code
     * @param entityType domain entity title
     * @param entityId   identifier evaluated during query
     */
    private EntityNotFoundException(ErrorCode code, String entityType, String entityId) {
        super(code, entityType + " not found with ID: " + entityId);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    /**
     * Creates an exception representing a missing forensic investigation case.
     *
     * @param caseId unique case UUID
     * @return initialized {@link EntityNotFoundException}
     */
    public static EntityNotFoundException forCase(UUID caseId) {
        return new EntityNotFoundException(ErrorCode.CASE_NOT_FOUND, "Case", caseId.toString());
    }

    /**
     * Creates an exception representing missing evidence artifact or transaction record.
     *
     * @param evidenceId unique evidence UUID
     * @return initialized {@link EntityNotFoundException}
     */
    public static EntityNotFoundException forEvidence(UUID evidenceId) {
        return new EntityNotFoundException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence", evidenceId.toString());
    }

    /**
     * Creates an exception representing a missing legal report, warrant, or affidavit document.
     *
     * @param documentId unique legal document UUID
     * @return initialized {@link EntityNotFoundException}
     */
    public static EntityNotFoundException forDocument(UUID documentId) {
        return new EntityNotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "LegalDocument", documentId.toString());
    }

    /**
     * Creates an exception representing an untracked or unknown blockchain wallet address.
     *
     * @param address public wallet address
     * @param chain   blockchain network identifier
     * @return initialized {@link EntityNotFoundException}
     */
    public static EntityNotFoundException forWallet(String address, String chain) {
        return new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND, "Wallet", address + "@" + chain);
    }

    public String entityType() {
        return entityType;
    }

    public String entityId() {
        return entityId;
    }
}
