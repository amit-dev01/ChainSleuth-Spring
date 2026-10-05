package com.chainsleuth.data.repository;

import com.chainsleuth.data.entity.CaseEntity;
import com.chainsleuth.data.entity.CaseStatus;
import com.chainsleuth.data.projection.CaseSummaryProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for managing {@link CaseEntity} instances.
 * <p>
 * <b>Multi-Tenant Isolation Strategy:</b><br>
 * To guarantee strict tenant isolation and defend against cross-organization data exfiltration,
 * all entity retrieval and modification operations filter explicitly by {@code userId} and/or
 * {@code organizationId}. Even if an adversary knows a case UUID, unauthorized access is rejected
 * at the database query level because queries bind the authenticated operator's identity.
 * </p>
 * <p>
 * <b>Native Hibernate 7 Soft-Delete Integration:</b><br>
 * Because {@link CaseEntity} is configured with {@code @SoftDelete(columnName = "deleted_at")},
 * Hibernate 7 automatically injects {@code AND deleted_at IS NULL} predicates into all generated
 * SQL queries, derived repository methods, and JPQL queries. Soft-deleted cases are completely
 * invisible to standard queries without requiring manual {@code @Where} or custom JPQL filters.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Case Data Access Repository</li>
 *   <li><b>Interacts with:</b> {@link CaseEntity}, {@link CaseSummaryProjection}</li>
 * </ul>
 */
@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, UUID> {

    /**
     * Retrieves a paginated list of cases owned by the specified user, ordered by last modified date descending.
     *
     * @param userId   owner's user UUID
     * @param pageable pagination parameters
     * @return page of case entities
     */
    Page<CaseEntity> findAllByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds a case by its unique identifier and owning user ID, enforcing tenant isolation.
     *
     * @param id     case UUID
     * @param userId owner's user UUID
     * @return {@link Optional} containing the matched entity, or empty if not found or unauthorized
     */
    Optional<CaseEntity> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Retrieves a paginated list of cases belonging to a specific organization.
     * <p>
     * Typically utilized by supervisors or tenant administrators for department-wide case oversight.
     * </p>
     *
     * @param organizationId organization/tenant identifier
     * @param pageable       pagination parameters
     * @return page of case entities
     */
    Page<CaseEntity> findAllByOrganizationIdOrderByUpdatedAtDesc(String organizationId, Pageable pageable);

    /**
     * Retrieves all cases for a user matching a specific lifecycle status, ordered by last modified date descending.
     *
     * @param userId owner's user UUID
     * @param status lifecycle status filter
     * @return list of matching case entities
     */
    List<CaseEntity> findAllByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, CaseStatus status);

    /**
     * Performs a fast index-backed existence check to verify user ownership without loading the entity.
     *
     * @param id     case UUID
     * @param userId owner's user UUID
     * @return {@code true} if an active case exists matching both criteria
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);

    /**
     * Atomically updates traversal status and execution start timestamp.
     *
     * @param caseId    case UUID
     * @param userId    owner's user UUID
     * @param status    new traversal status (e.g. "RUNNING")
     * @param startedAt timestamp when traversal was launched
     * @return number of affected rows (1 if successful; 0 if unauthorized or not found)
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE CaseEntity c
        SET c.traversalStatus = :status,
            c.traversalStartedAt = :startedAt
        WHERE c.id = :caseId AND c.userId = :userId
        """)
    int updateTraversalStatus(
            @Param("caseId") UUID caseId,
            @Param("userId") UUID userId,
            @Param("status") String status,
            @Param("startedAt") Instant startedAt
    );

    /**
     * Counts the total number of cases for a specific user possessing a designated status.
     *
     * @param userId owner's user UUID
     * @param status target lifecycle status
     * @return total matching count
     */
    @Query("SELECT COUNT(c) FROM CaseEntity c WHERE c.userId = :userId AND c.status = :status")
    long countByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") CaseStatus status
    );

    /**
     * Queries lightweight case summary projections for an investigator's dashboard.
     * <p>
     * Avoids loading heavyweight L1 session graphs or lazy associations.
     * </p>
     *
     * @param userId   owner's user UUID
     * @param pageable pagination parameters
     * @return paginated projection records
     */
    @Query("""
        SELECT new com.chainsleuth.data.projection.CaseSummaryProjection(
            c.id, c.title, c.status, c.blockchain, c.rootWalletAddress,
            c.totalTrackedUsd, c.totalNodesDiscovered, c.mlRiskScore,
            c.createdAt, c.updatedAt
        )
        FROM CaseEntity c
        WHERE c.userId = :userId
        ORDER BY c.updatedAt DESC
        """)
    Page<CaseSummaryProjection> findCaseSummariesByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    /**
     * Queries lightweight case summary projections for an entire organization.
     *
     * @param organizationId organization identifier
     * @param pageable       pagination parameters
     * @return paginated projection records
     */
    @Query("""
        SELECT new com.chainsleuth.data.projection.CaseSummaryProjection(
            c.id, c.title, c.status, c.blockchain, c.rootWalletAddress,
            c.totalTrackedUsd, c.totalNodesDiscovered, c.mlRiskScore,
            c.createdAt, c.updatedAt
        )
        FROM CaseEntity c
        WHERE c.organizationId = :organizationId
        ORDER BY c.updatedAt DESC
        """)
    Page<CaseSummaryProjection> findCaseSummariesByOrganizationId(
            @Param("organizationId") String organizationId,
            Pageable pageable
    );
}
