// =========================================================
// REQUIRED ADDITION TO Phase 1C ErrorCode.java
// Add the following to the DOMAIN group in ErrorCode enum:
//   CASE_NOT_EDITABLE → 409, "Case Cannot Be Edited"
// =========================================================

package com.chainsleuth.data.service;

import com.chainsleuth.core.exception.ChainSleuthException;
import com.chainsleuth.core.exception.EntityNotFoundException;
import com.chainsleuth.core.exception.ErrorCode;
import com.chainsleuth.core.model.ChainType;
import com.chainsleuth.core.security.SupabaseUserPrincipal;
import com.chainsleuth.data.entity.CaseEntity;
import com.chainsleuth.data.entity.CaseStatus;
import com.chainsleuth.data.entity.DocumentType;
import com.chainsleuth.data.entity.EvidenceSnapshotEntity;
import com.chainsleuth.data.entity.LegalDocumentEntity;
import com.chainsleuth.data.entity.SnapshotType;
import com.chainsleuth.data.projection.CaseSummaryProjection;
import com.chainsleuth.data.projection.EvidenceSnapshotProjection;
import com.chainsleuth.data.projection.LegalDocumentProjection;
import com.chainsleuth.data.repository.CaseRepository;
import com.chainsleuth.data.repository.EvidenceSnapshotRepository;
import com.chainsleuth.data.repository.LegalDocumentRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primary application service coordinating forensic investigation case lifecycle, evidence associations,
 * multi-tenant boundaries, and domain events.
 * <p>
 * <b>Read-Only Optimization:</b><br>
 * The class is annotated with {@code @Transactional(readOnly = true)} at the class level.
 * Because the vast majority of operations in forensic review dashboards are read-intensive, marking
 * transactions read-only instructs Hibernate to skip snapshot dirty checking and flush cycles.
 * Furthermore, modern database connection pools (such as HikariCP connecting to PostgreSQL/Supabase)
 * can route read-only transactions directly to read replicas, significantly boosting overall system throughput.
 * Write methods explicitly override this behavior with {@code @Transactional}.
 * </p>
 * <p>
 * <b>Multi-Tenant Security Enforcement:</b><br>
 * Operates on {@link SupabaseUserPrincipal} passed from authenticated web layer entrypoints.
 * Every query and modification mandates matching {@code userId} and/or {@code organizationId},
 * strictly preventing cross-tenant information disclosure.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Core Case Application Service</li>
 *   <li><b>Interacts with:</b> {@link CaseRepository}, {@link EvidenceSnapshotRepository}, {@link LegalDocumentRepository}</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private final CaseRepository caseRepository;
    private final EvidenceSnapshotRepository evidenceSnapshotRepository;
    private final LegalDocumentRepository legalDocumentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs the case service with required repository and messaging dependencies.
     *
     * @param caseRepository             primary case relational repository
     * @param evidenceSnapshotRepository snapshot relational repository
     * @param legalDocumentRepository    legal document relational repository
     * @param eventPublisher             Spring application event publisher
     */
    public CaseService(
            CaseRepository caseRepository,
            EvidenceSnapshotRepository evidenceSnapshotRepository,
            LegalDocumentRepository legalDocumentRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.caseRepository = caseRepository;
        this.evidenceSnapshotRepository = evidenceSnapshotRepository;
        this.legalDocumentRepository = legalDocumentRepository;
        this.eventPublisher = eventPublisher;
    }

    // =========================================================================
    // READ METHODS (Inherit readOnly = true from class level)
    // =========================================================================

    /**
     * Retrieves an individual case by identifier, verifying that it belongs to the authenticated user.
     *
     * @param caseId      unique case UUID
     * @param currentUser authenticated operator principal
     * @return active {@link CaseEntity}
     * @throws EntityNotFoundException if the case does not exist, is soft-deleted, or belongs to another user
     */
    public CaseEntity getCase(UUID caseId, SupabaseUserPrincipal currentUser) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        return caseRepository.findByIdAndUserId(caseId, currentUser.userId())
                .orElseThrow(() -> EntityNotFoundException.forCase(caseId));
    }

    /**
     * Retrieves a paginated list of lightweight case summary projections for the current authenticated user.
     *
     * @param currentUser authenticated operator principal
     * @param pageable    pagination specification
     * @return page of {@link CaseSummaryProjection}
     */
    public Page<CaseSummaryProjection> getCaseSummaries(SupabaseUserPrincipal currentUser, Pageable pageable) {
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        return caseRepository.findCaseSummariesByUserId(currentUser.userId(), pageable);
    }

    /**
     * Retrieves a paginated list of lightweight case summary projections for an organization.
     * <p>
     * Used by supervisors and administrators for department-level oversight across all team members.
     * </p>
     *
     * @param organizationId organization identifier
     * @param pageable       pagination specification
     * @return page of {@link CaseSummaryProjection}
     */
    public Page<CaseSummaryProjection> getCasesByOrganization(String organizationId, Pageable pageable) {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        return caseRepository.findCaseSummariesByOrganizationId(organizationId, pageable);
    }

    /**
     * Retrieves the list of evidentiary snapshot projections captured for a case, enforcing ownership.
     *
     * @param caseId      case UUID
     * @param currentUser authenticated operator principal
     * @return list of {@link EvidenceSnapshotProjection}
     * @throws EntityNotFoundException if case does not exist or access is unauthorized
     */
    public List<EvidenceSnapshotProjection> getEvidenceSnapshots(UUID caseId, SupabaseUserPrincipal currentUser) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        if (!caseRepository.existsByIdAndUserId(caseId, currentUser.userId())) {
            throw EntityNotFoundException.forCase(caseId);
        }

        return evidenceSnapshotRepository.findProjectionsByCaseId(caseId);
    }

    /**
     * Retrieves the list of legal document projections generated for a case, enforcing ownership.
     *
     * @param caseId      case UUID
     * @param currentUser authenticated operator principal
     * @return list of {@link LegalDocumentProjection}
     * @throws EntityNotFoundException if case does not exist or access is unauthorized
     */
    public List<LegalDocumentProjection> getLegalDocuments(UUID caseId, SupabaseUserPrincipal currentUser) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        if (!caseRepository.existsByIdAndUserId(caseId, currentUser.userId())) {
            throw EntityNotFoundException.forCase(caseId);
        }

        return legalDocumentRepository.findProjectionsByCaseId(caseId);
    }

    // =========================================================================
    // WRITE METHODS (Explicitly annotated with @Transactional)
    // =========================================================================

    /**
     * Initializes and persists a new forensic investigation case.
     *
     * @param command     case creation parameters
     * @param currentUser authenticated investigator principal
     * @return persisted {@link CaseEntity}
     */
    @Transactional
    public CaseEntity createCase(CreateCaseCommand command, SupabaseUserPrincipal currentUser) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        CaseEntity entity = new CaseEntity(
                currentUser.userId(),
                currentUser.organizationId(),
                command.title(),
                command.blockchain(),
                command.rootWalletAddress(),
                command.maxTraversalDepth()
        );
        entity.setDescription(command.description());

        CaseEntity savedCase = caseRepository.save(entity);

        eventPublisher.publishEvent(new CaseCreatedEvent(
                savedCase.getId(),
                currentUser.userId(),
                savedCase.getBlockchain()
        ));

        // Intentionally omit case title from logs to protect confidential or sensitive investigative subject data
        log.info("Case created: caseId={}, userId={}, chain={}",
                savedCase.getId(), currentUser.userId(), savedCase.getBlockchain());

        return savedCase;
    }

    /**
     * Updates an editable case's metadata and traversal parameters.
     *
     * @param caseId      case UUID
     * @param command     update parameters
     * @param currentUser authenticated operator principal
     * @return updated {@link CaseEntity}
     * @throws CaseNotEditableException if the case is in review, submitted, or terminal status
     */
    @Transactional
    public CaseEntity updateCase(UUID caseId, UpdateCaseCommand command, SupabaseUserPrincipal currentUser) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        CaseEntity caseEntity = getCase(caseId, currentUser);

        if (!caseEntity.isEditable()) {
            throw new CaseNotEditableException(caseId, caseEntity.getStatus());
        }

        if (command.title() != null && !command.title().isBlank()) {
            caseEntity.setTitle(command.title());
        }
        if (command.description() != null) {
            caseEntity.setDescription(command.description());
        }
        if (command.maxTraversalDepth() != null) {
            caseEntity.setMaxTraversalDepth(command.maxTraversalDepth());
        }

        return caseRepository.save(caseEntity);
    }

    /**
     * Soft-deletes a case by marking its deleted_at timestamp via Hibernate 7's @SoftDelete mechanism.
     *
     * @param caseId      case UUID
     * @param currentUser authenticated operator principal
     * @throws CaseNotEditableException if the case has already been formally submitted to LEA, closed, or archived
     */
    @Transactional
    public void softDeleteCase(UUID caseId, SupabaseUserPrincipal currentUser) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        CaseEntity caseEntity = getCase(caseId, currentUser);

        if (caseEntity.getStatus() == CaseStatus.SUBMITTED_TO_LEA
                || caseEntity.getStatus() == CaseStatus.CLOSED
                || caseEntity.getStatus() == CaseStatus.ARCHIVED) {
            throw new CaseNotEditableException(caseId, caseEntity.getStatus());
        }

        // Hibernate 7's @SoftDelete intercepts delete() and issues an UPDATE setting deleted_at = NOW()
        caseRepository.delete(caseEntity);

        log.info("Case soft-deleted: caseId={}, userId={}", caseId, currentUser.userId());
    }

    /**
     * Transitions an active case forward along its statutory lifecycle.
     *
     * @param caseId      case UUID
     * @param newStatus   target lifecycle status
     * @param currentUser authenticated operator principal
     * @return updated {@link CaseEntity}
     */
    @Transactional
    public CaseEntity transitionCaseStatus(UUID caseId, CaseStatus newStatus, SupabaseUserPrincipal currentUser) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        CaseEntity caseEntity = getCase(caseId, currentUser);
        CaseStatus previousStatus = caseEntity.getStatus();

        caseEntity.transitionTo(newStatus);
        CaseEntity savedCase = caseRepository.save(caseEntity);

        eventPublisher.publishEvent(new CaseStatusChangedEvent(
                caseId,
                previousStatus,
                newStatus,
                currentUser.userId()
        ));

        log.info("Case status transitioned: caseId={}, previousStatus={}, newStatus={}, userId={}",
                caseId, previousStatus, newStatus, currentUser.userId());

        return savedCase;
    }

    /**
     * Appends an immutable, cryptographically-signed evidence snapshot to an active investigation.
     *
     * @param caseId         case UUID
     * @param type           evidence category
     * @param dataPayload    raw JSON payload
     * @param dataHash       SHA-256 digest of payload (nullable for manual notes)
     * @param signatureToken cryptographic HMAC signature token (nullable for manual notes)
     * @param currentUser    authenticated operator principal
     * @return persisted {@link EvidenceSnapshotEntity}
     * @throws CaseNotEditableException if the parent case is locked against modifications
     */
    @Transactional
    public EvidenceSnapshotEntity addEvidenceSnapshot(
            UUID caseId,
            SnapshotType type,
            String dataPayload,
            String dataHash,
            String signatureToken,
            SupabaseUserPrincipal currentUser
    ) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(dataPayload, "dataPayload must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");

        CaseEntity caseEntity = getCase(caseId, currentUser);

        if (!caseEntity.isEditable()) {
            throw new CaseNotEditableException(caseId, caseEntity.getStatus());
        }

        EvidenceSnapshotEntity snapshot = new EvidenceSnapshotEntity(
                caseEntity,
                type,
                dataPayload,
                dataHash,
                signatureToken,
                currentUser.userId()
        );

        caseEntity.addEvidenceSnapshot(snapshot);
        caseRepository.save(caseEntity);

        log.info("Evidence snapshot added: caseId={}, snapshotId={}, type={}, userId={}",
                caseId, snapshot.getId(), type, currentUser.userId());

        return snapshot;
    }

    // =========================================================================
    // Command & Event Records
    // =========================================================================

    /**
     * Input command record encapsulating parameters for case initialization.
     *
     * @param title             case title
     * @param description       detailed narrative of the complaint or investigation
     * @param blockchain         target blockchain architecture
     * @param rootWalletAddress  seed wallet address initiating forensic traversal
     * @param maxTraversalDepth  maximum traversal hop count (1 to 10)
     */
    public record CreateCaseCommand(
            String title,
            String description,
            ChainType blockchain,
            String rootWalletAddress,
            int maxTraversalDepth
    ) {
    }

    /**
     * Input command record encapsulating mutable fields for an investigation case.
     *
     * @param title             new case title (or null to preserve)
     * @param description       new narrative description (or null to preserve)
     * @param maxTraversalDepth updated traversal depth (or null to preserve)
     */
    public record UpdateCaseCommand(
            String title,
            String description,
            Integer maxTraversalDepth
    ) {
    }

    /**
     * Domain event published when a new case aggregate is initialized and committed.
     *
     * @param caseId     unique case UUID
     * @param userId     creator user UUID
     * @param blockchain target blockchain architecture
     */
    public record CaseCreatedEvent(
            UUID caseId,
            UUID userId,
            ChainType blockchain
    ) {
    }

    /**
     * Domain event published when a case's lifecycle status transitions forward.
     *
     * @param caseId         unique case UUID
     * @param previousStatus previous lifecycle status
     * @param newStatus      new lifecycle status
     * @param userId         operator user UUID executing the transition
     */
    public record CaseStatusChangedEvent(
            UUID caseId,
            CaseStatus previousStatus,
            CaseStatus newStatus,
            UUID userId
    ) {
    }

    /**
     * Domain exception thrown when an operation attempts to modify a case in a non-editable lifecycle status.
     */
    public static final class CaseNotEditableException extends ChainSleuthException {

        /**
         * Constructs a new CaseNotEditableException.
         *
         * @param caseId unique case UUID
         * @param status current lifecycle status preventing modification
         */
        public CaseNotEditableException(UUID caseId, CaseStatus status) {
            super(ErrorCode.CASE_NOT_EDITABLE,
                    "Case " + caseId + " is in non-editable or terminal status: " + status);
        }
    }
}
