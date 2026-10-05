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
 * Entity representing a formally compiled statutory legal document or judicial brief.
 * <p>
 * Encapsulates metadata, digital signature references, and cloud storage pointers for
 * generated PDF artifacts (e.g. BNSS Section 91 notices, Section 65B certificates, FIR briefs).
 * </p>
 * <p>
 * <b>Storage &amp; Post-Upload Integrity Verification:</b><br>
 * When a document is generated, it initially exists as in-memory or transient PDF bytes.
 * Upon successful transmission to Supabase Storage, {@link #recordUpload(String, String)}
 * records the cloud storage object path alongside the cryptographic SHA-256 hash of the
 * uploaded byte stream. This prevents post-upload file substitution or silent bitrot.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Legal Document Entity</li>
 *   <li><b>Interacts with:</b> {@link CaseEntity}, {@link DocumentType}</li>
 * </ul>
 */
@Entity
@Table(
    name = "legal_documents",
    indexes = {
        @Index(name = "idx_legal_docs_case_id", columnList = "case_id"),
        @Index(name = "idx_legal_docs_type", columnList = "document_type"),
        @Index(name = "idx_legal_docs_generated", columnList = "generated_at")
    }
)
public class LegalDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, updatable = false)
    private CaseEntity caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Column(name = "generated_pdf_storage_path", length = 500)
    private String generatedPdfStoragePath;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "generated_by_user_id", updatable = false)
    private UUID generatedByUserId;

    @Column(name = "officer_name", length = 200)
    private String officerName;

    @Column(name = "officer_rank", length = 100)
    private String officerRank;

    @Column(name = "target_entity_name", length = 300)
    private String targetEntityName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_metadata", columnDefinition = "jsonb")
    private String documentMetadata;

    /**
     * Protected no-arg constructor required by Jakarta Persistence and Hibernate proxies.
     */
    protected LegalDocumentEntity() {
    }

    /**
     * Public constructor initializing a legal document entity prior to PDF compilation and storage upload.
     *
     * @param caseEntity        associated investigation case
     * @param documentType      statutory document classification
     * @param generatedByUserId investigator UUID compiling the document
     * @param officerName       name of the signing law enforcement officer
     * @param officerRank       rank / title of the signing officer
     * @param targetEntityName  target VASP, bank, exchange, or judicial entity
     */
    public LegalDocumentEntity(
            CaseEntity caseEntity,
            DocumentType documentType,
            UUID generatedByUserId,
            String officerName,
            String officerRank,
            String targetEntityName
    ) {
        this.caseEntity = caseEntity;
        this.documentType = documentType;
        this.generatedByUserId = generatedByUserId;
        this.officerName = officerName;
        this.officerRank = officerRank;
        this.targetEntityName = targetEntityName;
        this.generatedAt = Instant.now();
    }

    /**
     * JPA lifecycle callback establishing the generation timestamp before initial persistence.
     */
    @PrePersist
    void onPrePersist() {
        if (this.generatedAt == null) {
            this.generatedAt = Instant.now();
        }
    }

    /**
     * Evaluates whether the generated PDF has been uploaded to remote object storage and hashed.
     *
     * @return {@code true} if storage path and SHA-256 content hash are non-null
     */
    public boolean isUploadComplete() {
        return this.generatedPdfStoragePath != null && this.contentHash != null;
    }

    /**
     * Records completion of the remote PDF upload and binds the SHA-256 byte digest.
     *
     * @param storagePath Supabase Storage object URI or relative path
     * @param sha256Hash  SHA-256 cryptographic digest of the PDF file
     */
    public void recordUpload(String storagePath, String sha256Hash) {
        this.generatedPdfStoragePath = storagePath;
        this.contentHash = sha256Hash;
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

    void setCaseEntity(CaseEntity caseEntity) {
        this.caseEntity = caseEntity;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getGeneratedPdfStoragePath() {
        return generatedPdfStoragePath;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public UUID getGeneratedByUserId() {
        return generatedByUserId;
    }

    public String getOfficerName() {
        return officerName;
    }

    public String getOfficerRank() {
        return officerRank;
    }

    public String getTargetEntityName() {
        return targetEntityName;
    }

    public String getDocumentMetadata() {
        return documentMetadata;
    }

    public void setDocumentMetadata(String documentMetadata) {
        this.documentMetadata = documentMetadata;
    }
}
