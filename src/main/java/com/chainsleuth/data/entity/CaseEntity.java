package com.chainsleuth.data.entity;

import com.chainsleuth.core.model.ChainType;
import com.chainsleuth.data.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/**
 * Root domain entity representing a ChainSleuth blockchain forensic investigation case.
 * <p>
 * Encapsulates case metadata, investigative target wallet, graph traversal configuration,
 * ML risk assessment score, and references to associated evidence snapshots and legal documents.
 * </p>
 * <p>
 * <b>Domain-Driven Design (DDD) &amp; Invariant Protection:</b><br>
 * Following DDD principles, {@link CaseEntity} acts as an aggregate root that guards its own
 * business invariants. State transitions, traversal status tracking, and snapshot associations
 * are governed via explicit business methods on the entity rather than scattered across service layers.
 * </p>
 * <p>
 * <b>Hibernate 7 Native Soft Delete:</b><br>
 * Uses {@link SoftDelete} configured with {@link SoftDeleteType#DELETED_AT}. Hibernate automatically
 * rewrites SQL queries to append {@code deleted_at IS NULL} and intercepts JPA {@code delete()} calls
 * to set the current timestamp rather than issuing physical SQL {@code DELETE} statements.
 * </p>
 * <p>
 * <b>Optimistic Locking with Virtual Threads:</b><br>
 * The {@link #version} field leverages JPA {@link Version} to guarantee serialized, conflict-free
 * state updates when concurrent requests or background graph traversals interact with the case.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Primary Relational Case Entity</li>
 *   <li><b>Interacts with:</b> {@link AuditableEntity}, {@link EvidenceSnapshotEntity}, {@link LegalDocumentEntity}</li>
 * </ul>
 */
@Entity
@Table(
    name = "cases",
    indexes = {
        @Index(name = "idx_cases_user_id", columnList = "user_id"),
        @Index(name = "idx_cases_organization_id", columnList = "organization_id"),
        @Index(name = "idx_cases_status", columnList = "status"),
        @Index(name = "idx_cases_blockchain", columnList = "blockchain"),
        @Index(name = "idx_cases_root_wallet", columnList = "root_wallet_address"),
        @Index(name = "idx_cases_deleted_at", columnList = "deleted_at")
    }
)
@SoftDelete(columnName = "deleted_at", strategy = SoftDeleteType.DELETED_AT)
public class CaseEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "organization_id", length = 100)
    private String organizationId;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 5000)
    @Column(name = "description", length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "blockchain", nullable = false, length = 20)
    private ChainType blockchain;

    @NotBlank
    @Column(name = "root_wallet_address", nullable = false, length = 100)
    private String rootWalletAddress;

    @Column(name = "total_tracked_usd", precision = 24, scale = 8)
    private BigDecimal totalTrackedUsd;

    @Min(1)
    @Max(10)
    @Column(name = "max_traversal_depth", nullable = false)
    private Integer maxTraversalDepth;

    @Column(name = "traversal_status", length = 20)
    private String traversalStatus;

    @Column(name = "traversal_started_at")
    private Instant traversalStartedAt;

    @Column(name = "traversal_completed_at")
    private Instant traversalCompletedAt;

    @Column(name = "total_nodes_discovered")
    private Integer totalNodesDiscovered;

    @Column(name = "total_edges_discovered")
    private Integer totalEdgesDiscovered;

    @Column(name = "ml_risk_score")
    private Double mlRiskScore;

    @Column(name = "ai_analysis_summary", columnDefinition = "TEXT")
    private String aiAnalysisSummary;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private Instant deletedAt;

    @OneToMany(
        mappedBy = "caseEntity",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("createdAt DESC")
    private List<EvidenceSnapshotEntity> evidenceSnapshots = new ArrayList<>();

    @OneToMany(
        mappedBy = "caseEntity",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("generatedAt DESC")
    private List<LegalDocumentEntity> legalDocuments = new ArrayList<>();

    /**
     * Protected no-arg constructor required by Jakarta Persistence specification and Hibernate proxies.
     */
    protected CaseEntity() {
    }

    /**
     * Public domain constructor initializing a new investigation case.
     *
     * @param userId             unique identifier of the creating investigator
     * @param organizationId     tenant or law enforcement agency ID
     * @param title              investigation case title
     * @param blockchain         target blockchain architecture
     * @param rootWalletAddress  seed wallet address initiating forensic traversal
     * @param maxTraversalDepth  maximum traversal hop count (1 to 10)
     */
    public CaseEntity(
            UUID userId,
            String organizationId,
            String title,
            ChainType blockchain,
            String rootWalletAddress,
            int maxTraversalDepth
    ) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.title = title;
        this.blockchain = blockchain;
        this.rootWalletAddress = rootWalletAddress;
        this.maxTraversalDepth = maxTraversalDepth;
        this.status = CaseStatus.DRAFT;
        this.version = 0L;
        this.totalNodesDiscovered = 0;
        this.totalEdgesDiscovered = 0;
    }

    // =========================================================================
    // Domain Business Methods
    // =========================================================================

    /**
     * Checks if this case can accept investigative modifications.
     *
     * @return {@code true} if case is in an editable status and has not been soft-deleted
     */
    public boolean isEditable() {
        return status != null && status.isEditable() && deletedAt == null;
    }

    /**
     * Evaluates if a graph traversal execution is currently active for this case.
     *
     * @return {@code true} if traversal status equals "RUNNING"
     */
    public boolean isTraversalRunning() {
        return "RUNNING".equals(this.traversalStatus);
    }

    /**
     * Marks the initiation of a background graph traversal job.
     */
    public void markTraversalStarted() {
        this.traversalStatus = "RUNNING";
        this.traversalStartedAt = Instant.now();
    }

    /**
     * Records successful completion of a graph traversal run and persists discovery metrics.
     *
     * @param nodes    discovered on-chain wallet nodes count
     * @param edges    discovered transaction transfer edges count
     * @param totalUsd aggregated USD value tracked through the subgraph
     */
    public void markTraversalCompleted(int nodes, int edges, BigDecimal totalUsd) {
        this.traversalStatus = "COMPLETED";
        this.traversalCompletedAt = Instant.now();
        this.totalNodesDiscovered = nodes;
        this.totalEdgesDiscovered = edges;
        this.totalTrackedUsd = totalUsd;
    }

    /**
     * Flags a graph traversal execution failure.
     */
    public void markTraversalFailed() {
        this.traversalStatus = "FAILED";
        this.traversalCompletedAt = Instant.now();
    }

    /**
     * Associates an evidence snapshot with this case, maintaining bidirectional integrity.
     *
     * @param snapshot snapshot to append
     */
    public void addEvidenceSnapshot(EvidenceSnapshotEntity snapshot) {
        if (snapshot != null) {
            this.evidenceSnapshots.add(snapshot);
            snapshot.setCaseEntity(this);
        }
    }

    /**
     * Transitions the case lifecycle to a target status, enforcing forward-only state progression.
     *
     * @param newStatus target lifecycle status
     * @throws IllegalStateException if the current status is terminal or transition represents a backward movement
     */
    public void transitionTo(CaseStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Target CaseStatus must not be null");
        }
        if (this.status != null && this.status.isTerminal()) {
            throw new IllegalStateException("Cannot transition from terminal case status: " + this.status);
        }
        if (this.status != null && newStatus.ordinal() <= this.status.ordinal()) {
            throw new IllegalStateException(
                    "Backward or identical status transitions are prohibited. Current: " + this.status + ", Target: " + newStatus);
        }
        this.status = newStatus;
    }

    // =========================================================================
    // Getters & Permitted Setters
    // =========================================================================

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public ChainType getBlockchain() {
        return blockchain;
    }

    public String getRootWalletAddress() {
        return rootWalletAddress;
    }

    public BigDecimal getTotalTrackedUsd() {
        return totalTrackedUsd;
    }

    public void setTotalTrackedUsd(BigDecimal totalTrackedUsd) {
        this.totalTrackedUsd = totalTrackedUsd;
    }

    public Integer getMaxTraversalDepth() {
        return maxTraversalDepth;
    }

    public void setMaxTraversalDepth(Integer maxTraversalDepth) {
        this.maxTraversalDepth = maxTraversalDepth;
    }

    public String getTraversalStatus() {
        return traversalStatus;
    }

    public void setTraversalStatus(String traversalStatus) {
        this.traversalStatus = traversalStatus;
    }

    public Instant getTraversalStartedAt() {
        return traversalStartedAt;
    }

    public void setTraversalStartedAt(Instant traversalStartedAt) {
        this.traversalStartedAt = traversalStartedAt;
    }

    public Instant getTraversalCompletedAt() {
        return traversalCompletedAt;
    }

    public void setTraversalCompletedAt(Instant traversalCompletedAt) {
        this.traversalCompletedAt = traversalCompletedAt;
    }

    public Integer getTotalNodesDiscovered() {
        return totalNodesDiscovered;
    }

    public void setTotalNodesDiscovered(Integer totalNodesDiscovered) {
        this.totalNodesDiscovered = totalNodesDiscovered;
    }

    public Integer getTotalEdgesDiscovered() {
        return totalEdgesDiscovered;
    }

    public void setTotalEdgesDiscovered(Integer totalEdgesDiscovered) {
        this.totalEdgesDiscovered = totalEdgesDiscovered;
    }

    public Double getMlRiskScore() {
        return mlRiskScore;
    }

    public void setMlRiskScore(Double mlRiskScore) {
        this.mlRiskScore = mlRiskScore;
    }

    public String getAiAnalysisSummary() {
        return aiAnalysisSummary;
    }

    public void setAiAnalysisSummary(String aiAnalysisSummary) {
        this.aiAnalysisSummary = aiAnalysisSummary;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public List<EvidenceSnapshotEntity> getEvidenceSnapshots() {
        return evidenceSnapshots;
    }

    public List<LegalDocumentEntity> getLegalDocuments() {
        return legalDocuments;
    }
}
