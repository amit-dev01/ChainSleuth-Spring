package com.chainsleuth.data.projection;

import com.chainsleuth.core.model.ChainType;
import com.chainsleuth.data.entity.CaseStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable Java 25 record projection for case summary dashboard and tabular list views.
 * <p>
 * <b>Performance Benefits of Record Projections:</b><br>
 * Projections decouple data access from the Hibernate Persistence Context (L1 cache) and dirty tracking.
 * By querying only the scalar columns required for list displays directly via JPQL constructor expressions,
 * the application completely avoids loading heavyweight domain entities, soft-delete filters on associations,
 * and lazy-loaded one-to-many collections (preventing N+1 queries entirely).
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Case Summary DTO Projection</li>
 *   <li><b>Interacts with:</b> {@link com.chainsleuth.data.repository.CaseRepository}</li>
 * </ul>
 *
 * @param id                   case unique identifier
 * @param title                investigation case title
 * @param status               current lifecycle status
 * @param blockchain           target blockchain architecture
 * @param rootWalletAddress    seed wallet address
 * @param totalTrackedUsd      aggregated transaction volume tracked
 * @param totalNodesDiscovered count of distinct wallet addresses analyzed
 * @param mlRiskScore          composite machine learning risk score (0.0 to 1.0)
 * @param createdAt            case creation timestamp (UTC)
 * @param updatedAt            case last modified timestamp (UTC)
 */
public record CaseSummaryProjection(
        UUID id,
        String title,
        CaseStatus status,
        ChainType blockchain,
        String rootWalletAddress,
        BigDecimal totalTrackedUsd,
        Integer totalNodesDiscovered,
        Double mlRiskScore,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Evaluates if this case represents a high-risk financial crime target.
     *
     * @return {@code true} if composite ML risk score is greater than or equal to 0.75
     */
    public boolean isHighRisk() {
        return mlRiskScore != null && mlRiskScore >= 0.75;
    }

    /**
     * Determines whether graph traversal execution has successfully discovered on-chain nodes.
     *
     * @return {@code true} if node count is non-null and greater than zero
     */
    public boolean isTraversalComplete() {
        return totalNodesDiscovered != null && totalNodesDiscovered > 0;
    }

    /**
     * Computes a standardized risk tier label for UI badge formatting.
     *
     * @return "HIGH", "MEDIUM", "LOW", or "UNSCORED"
     */
    public String riskLabel() {
        if (mlRiskScore == null) {
            return "UNSCORED";
        }
        if (mlRiskScore >= 0.75) {
            return "HIGH";
        }
        if (mlRiskScore >= 0.40) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
