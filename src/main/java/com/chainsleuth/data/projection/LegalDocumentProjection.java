package com.chainsleuth.data.projection;

import com.chainsleuth.data.entity.DocumentType;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable Java 25 record projection for listing legal documents generated within an investigation.
 * <p>
 * Eliminates overhead by omitting large metadata JSONB documents and storage blobs, returning
 * only core status and metadata needed to render document management interfaces.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Legal Document DTO Projection</li>
 *   <li><b>Interacts with:</b> {@link com.chainsleuth.data.repository.LegalDocumentRepository}</li>
 * </ul>
 *
 * @param id                document unique identifier
 * @param documentType      statutory document classification
 * @param officerName       name of the law enforcement signatory
 * @param targetEntityName  target exchange or financial institution
 * @param generatedAt       generation timestamp (UTC)
 * @param generatedByUserId investigator UUID who compiled the document
 * @param uploadComplete    flag indicating whether the PDF has been successfully archived to object storage
 */
public record LegalDocumentProjection(
        UUID id,
        DocumentType documentType,
        String officerName,
        String targetEntityName,
        Instant generatedAt,
        UUID generatedByUserId,
        boolean uploadComplete
) {
}
