package com.chainsleuth.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing an immutable, cryptographically-signed forensic evidence snapshot.
 * <p>
 * <b>Legal Admissibility &amp; Section 65B Compliance:</b><br>
 * Under Section 65B of the Indian Evidence Act (and corresponding international digital evidence standards),
 * electronic records must establish an unbroken chain of custody and verifiable proof that data has not
 * been altered since acquisition. To fulfill statutory criteria:
 * <ul>
 *   <li>Snapshots are strictly <b>immutable</b> upon creation — no setters or update lifecycles are exposed.</li>
 *   <li>The payload hash ({@link #dataHash}) records the SHA-256 digest of the raw JSON content.</li>
 *   <li>The HMAC signature ({@link #signatureToken}) binds the payload to the platform's security key.</li>
 *   <li>Entity does not implement {@code @Version} or {@code AuditableEntity} since snapshots cannot be revised.</li>
 * </ul>
 * </p>
 * <p>
 * <b>JSONB Persistence:</b><br>
 * The {@link #dataPayload} field utilizes Hibernate 7's {@link JdbcTypeCode} with {@link SqlTypes#JSON}
 * and {@code columnDefinition = "jsonb"} to persist structured payloads (e.g. Neo4j graph topologies,
 * transaction ledgers, or AI briefs) directly into PostgreSQL JSONB without third-party type dependencies.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Evidence Snapshot Domain Entity</li>
 *   <li><b>Interacts with:</b> {@link CaseEntity}, {@link SnapshotType}</li>
 * </ul>
 */
@Entity
@Table(
    name = "evidence_snapshots",
    indexes = {
        @Index(name = "idx_evidence_case_id", columnList = "case_id"),
        @Index(name = "idx_evidence_snapshot_type", columnList = "snapshot_type"),
        @Index(name = "idx_evidence_created_at", columnList = "created_at")
    }
)
public class EvidenceSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, updatable = false)
    private CaseEntity caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false, length = 30)
    private SnapshotType snapshotType;

    @Column(name = "data_hash", length = 64)
    private String dataHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_payload", columnDefinition = "jsonb", nullable = false)
    private String dataPayload;

    @Column(name = "signature_token", length = 500)
    private String signatureToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", updatable = false)
    private UUID createdByUserId;

    /**
     * Protected no-arg constructor required by Jakarta Persistence and Hibernate proxies.
     */
    protected EvidenceSnapshotEntity() {
    }

    /**
     * Public factory constructor for creating an immutable evidence snapshot record.
     *
     * @param caseEntity      parent investigation case
     * @param snapshotType    evidence category
     * @param dataPayload     JSON payload representing the evidence artifact
     * @param dataHash        SHA-256 hexadecimal hash string (nullable for MANUAL_NOTE)
     * @param signatureToken  HMAC digital signature token (nullable for MANUAL_NOTE)
     * @param createdByUserId UUID of the investigator capturing the snapshot
     */
    public EvidenceSnapshotEntity(
            CaseEntity caseEntity,
            SnapshotType snapshotType,
            String dataPayload,
            String dataHash,
            String signatureToken,
            UUID createdByUserId
    ) {
        this.caseEntity = caseEntity;
        this.snapshotType = snapshotType;
        this.dataPayload = dataPayload;
        this.dataHash = dataHash;
        this.signatureToken = signatureToken;
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
    }

    /**
     * JPA lifecycle callback establishing the creation timestamp prior to database insert.
     */
    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    /**
     * Evaluates whether this snapshot possesses the requisite cryptographic hash and HMAC signature.
     *
     * @return {@code true} if integrity verification criteria are satisfied
     */
    public boolean isIntegrityVerifiable() {
        return snapshotType.requiresHash() && dataHash != null && signatureToken != null;
    }

    // =========================================================================
    // Getters & Relationship Mutator
    // =========================================================================

    public UUID getId() {
        return id;
    }

    public CaseEntity getCaseEntity() {
        return caseEntity;
    }

    /**
     * Sets the owning case reference. Package-private to preserve aggregate boundary encapsulation.
     *
     * @param caseEntity owning case entity
     */
    void setCaseEntity(CaseEntity caseEntity) {
        this.caseEntity = caseEntity;
    }

    public SnapshotType getSnapshotType() {
        return snapshotType;
    }

    public String getDataHash() {
        return dataHash;
    }

    public String getDataPayload() {
        return dataPayload;
    }

    public String getSignatureToken() {
        return signatureToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }
}
