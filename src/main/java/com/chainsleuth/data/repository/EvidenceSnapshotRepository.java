package com.chainsleuth.data.repository;

import com.chainsleuth.data.entity.EvidenceSnapshotEntity;
import com.chainsleuth.data.entity.SnapshotType;
import com.chainsleuth.data.projection.EvidenceSnapshotProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for managing {@link EvidenceSnapshotEntity} instances.
 * <p>
 * Provides high-throughput projection queries to list evidence snapshots without loading
 * expensive JSONB data payloads into memory, and specialized queries to extract ordered
 * cryptographic hashes for digital evidence chain verification under Section 65B.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Evidence Snapshot Repository</li>
 *   <li><b>Interacts with:</b> {@link EvidenceSnapshotEntity}, {@link EvidenceSnapshotProjection}</li>
 * </ul>
 */
@Repository
public interface EvidenceSnapshotRepository extends JpaRepository<EvidenceSnapshotEntity, UUID> {

    /**
     * Retrieves all full evidence snapshot entities for a specific case, ordered by creation date descending.
     *
     * @param caseId case UUID
     * @return list of evidence snapshot entities
     */
    List<EvidenceSnapshotEntity> findAllByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    /**
     * Retrieves evidence snapshots for a case matching a specific evidentiary category.
     *
     * @param caseId case UUID
     * @param type   snapshot category
     * @return list of matching snapshot entities
     */
    List<EvidenceSnapshotEntity> findAllByCaseEntityIdAndSnapshotType(UUID caseId, SnapshotType type);

    /**
     * Counts the total number of snapshots captured for a case.
     *
     * @param caseId case UUID
     * @return total snapshot count
     */
    long countByCaseEntityId(UUID caseId);

    /**
     * Queries lightweight evidence snapshot projections for UI listing, avoiding JSONB payload overhead.
     *
     * @param caseId case UUID
     * @return list of snapshot projection records
     */
    @Query("""
        SELECT new com.chainsleuth.data.projection.EvidenceSnapshotProjection(
            e.id, e.snapshotType, e.dataHash, e.createdAt, e.createdByUserId,
            CASE WHEN e.dataHash IS NOT NULL AND e.signatureToken IS NOT NULL
                 THEN true ELSE false END
        )
        FROM EvidenceSnapshotEntity e
        WHERE e.caseEntity.id = :caseId
        ORDER BY e.createdAt DESC
        """)
    List<EvidenceSnapshotProjection> findProjectionsByCaseId(@Param("caseId") UUID caseId);

    /**
     * Extracts an ordered sequence of all cryptographic SHA-256 hashes for snapshots within a case.
     * <p>
     * Used by {@code EvidenceCertificationService} to construct the Merkle root or chained cumulative hash
     * for statutory Section 65B electronic evidence certification affidavits.
     * </p>
     *
     * @param caseId case UUID
     * @return list of SHA-256 hash strings ordered chronologically by capture time
     */
    @Query("""
        SELECT e.dataHash
        FROM EvidenceSnapshotEntity e
        WHERE e.caseEntity.id = :caseId
          AND e.dataHash IS NOT NULL
        ORDER BY e.createdAt ASC
        """)
    List<String> findAllHashesByCaseIdOrderByCreatedAt(@Param("caseId") UUID caseId);
}
