package com.chainsleuth.data.projection;

import com.chainsleuth.data.entity.SnapshotType;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable Java 25 record projection for listing evidence snapshots associated with a case.
 * <p>
 * Bypasses reading large JSONB payloads ({@code data_payload}) from PostgreSQL storage when
 * rendering snapshot history tables and audit lists, significantly optimizing I/O throughput.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Evidence Snapshot DTO Projection</li>
 *   <li><b>Interacts with:</b> {@link com.chainsleuth.data.repository.EvidenceSnapshotRepository}</li>
 * </ul>
 *
 * @param id                  evidence snapshot UUID
 * @param snapshotType        category classification
 * @param dataHash            cryptographic SHA-256 digest hex string
 * @param createdAt           snapshot capture timestamp (UTC)
 * @param createdByUserId     investigator UUID who captured this snapshot
 * @param integrityVerifiable boolean flag indicating whether cryptographic hash and signature are present
 */
public record EvidenceSnapshotProjection(
        UUID id,
        SnapshotType snapshotType,
        String dataHash,
        Instant createdAt,
        UUID createdByUserId,
        boolean integrityVerifiable
) {
}
