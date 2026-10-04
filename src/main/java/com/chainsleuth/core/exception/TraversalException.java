package com.chainsleuth.core.exception;

import java.util.UUID;

/**
 * Domain exception signaling failures or algorithmic anomalies encountered during
 * transaction hop traversals across the Neo4j graph engine.
 * <p>
 * Handles graph cycle detections, execution timeouts, concurrent job collisions,
 * and search depth limits.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Graph Traversal Exception</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthException}</li>
 * </ul>
 */
public final class TraversalException extends ChainSleuthException {

    private final UUID caseId;

    /**
     * Private constructor called by static factory methods.
     *
     * @param code   traversal error code
     * @param caseId case identifier undergoing analysis
     * @param detail human-readable explanation
     */
    private TraversalException(ErrorCode code, UUID caseId, String detail) {
        super(code, detail);
        this.caseId = caseId;
    }

    /**
     * Creates an exception when traversal exceeds the licensed or safe maximum hop depth.
     *
     * @param caseId   case identifier
     * @param maxDepth hop count limit
     * @return initialized {@link TraversalException}
     */
    public static TraversalException depthExceeded(UUID caseId, int maxDepth) {
        return new TraversalException(
                ErrorCode.DEPTH_EXCEEDED,
                caseId,
                "Max traversal depth of " + maxDepth + " hops exceeded for case: " + caseId
        );
    }

    /**
     * Creates an exception when an active traversal is already executing for the case.
     *
     * @param caseId case identifier
     * @return initialized {@link TraversalException}
     */
    public static TraversalException alreadyInProgress(UUID caseId) {
        return new TraversalException(
                ErrorCode.TRAVERSAL_IN_PROGRESS,
                caseId,
                "A traversal is already running for case: " + caseId + ". Wait for it to complete or cancel it first."
        );
    }

    /**
     * Creates an exception when graph computation exceeds the operational deadline.
     *
     * @param caseId         case identifier
     * @param timeoutSeconds maximum duration threshold in seconds
     * @return initialized {@link TraversalException}
     */
    public static TraversalException timedOut(UUID caseId, long timeoutSeconds) {
        return new TraversalException(
                ErrorCode.TRAVERSAL_TIMEOUT,
                caseId,
                "Traversal for case " + caseId + " exceeded the maximum allowed duration of " + timeoutSeconds + "s."
        );
    }

    /**
     * Creates an exception when a circular fund routing loop is detected in transaction hops.
     *
     * @param caseId  case identifier
     * @param address wallet address where the loop converged
     * @return initialized {@link TraversalException}
     */
    public static TraversalException cycleDetected(UUID caseId, String address) {
        return new TraversalException(
                ErrorCode.CYCLE_DETECTED,
                caseId,
                "Cycle detected at address " + address + " in case " + caseId + ". Graph may contain circular fund flows."
        );
    }

    public UUID caseId() {
        return caseId;
    }
}
