package com.chainsleuth.data.repository;

import com.chainsleuth.data.entity.DocumentType;
import com.chainsleuth.data.entity.LegalDocumentEntity;
import com.chainsleuth.data.projection.LegalDocumentProjection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for managing {@link LegalDocumentEntity} instances.
 * <p>
 * Provides high-throughput retrieval of legal documents and lightweight record projections
 * for tracking statutory filings, VASP freeze orders, and Section 65B certificates.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Legal Document Repository</li>
 *   <li><b>Interacts with:</b> {@link LegalDocumentEntity}, {@link LegalDocumentProjection}</li>
 * </ul>
 */
@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocumentEntity, UUID> {

    /**
     * Retrieves all legal document records for a case, ordered by generation timestamp descending.
     *
     * @param caseId case UUID
     * @return list of legal document entities
     */
    List<LegalDocumentEntity> findAllByCaseEntityIdOrderByGeneratedAtDesc(UUID caseId);

    /**
     * Finds a legal document of a specific statutory type associated with a case.
     * <p>
     * Documents such as FIR briefs or Section 65B certificates are typically unique per case.
     * </p>
     *
     * @param caseId case UUID
     * @param type   statutory document classification
     * @return {@link Optional} containing matching entity or empty if not yet compiled
     */
    Optional<LegalDocumentEntity> findByCaseEntityIdAndDocumentType(UUID caseId, DocumentType type);

    /**
     * Counts the total number of legal documents generated for an investigation.
     *
     * @param caseId case UUID
     * @return total document count
     */
    long countByCaseEntityId(UUID caseId);

    /**
     * Queries lightweight legal document projections for UI listing, avoiding JSONB metadata overhead.
     *
     * @param caseId case UUID
     * @return list of legal document projection records
     */
    @Query("""
        SELECT new com.chainsleuth.data.projection.LegalDocumentProjection(
            l.id, l.documentType, l.officerName, l.targetEntityName,
            l.generatedAt, l.generatedByUserId,
            CASE WHEN l.generatedPdfStoragePath IS NOT NULL
                 THEN true ELSE false END
        )
        FROM LegalDocumentEntity l
        WHERE l.caseEntity.id = :caseId
        ORDER BY l.generatedAt DESC
        """)
    List<LegalDocumentProjection> findProjectionsByCaseId(@Param("caseId") UUID caseId);
}
