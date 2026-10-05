package com.chainsleuth.data.entity;

/**
 * Enumeration classifying evidentiary snapshot categories captured during blockchain forensic investigations.
 * <p>
 * Under Indian Bhartiya Nagarik Suraksha Sanhita (BNSS) Section 91 and Section 65B of the Indian Evidence Act,
 * digital evidence admitted to court must prove cryptographic integrity. Snapshots capturing machine-generated
 * outputs (transaction graphs, logs, AI briefs, and ML risk scores) mandate SHA-256 cryptographic hashing.
 * Human investigator annotations ({@link #MANUAL_NOTE}) represent subjective commentary and do not enforce hashing.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Evidence Snapshot Domain Classification</li>
 *   <li><b>Interacts with:</b> {@link EvidenceSnapshotEntity}</li>
 * </ul>
 */
public enum SnapshotType {

    /**
     * Complete Neo4j transaction subgraph exported as structured JSON graph topology.
     */
    GRAPH_EXPORT("Graph Export", true),

    /**
     * Exhaustive tabular transaction ledger capturing on-chain transfer details and token movements.
     */
    TRANSACTION_LOG("Transaction Log", true),

    /**
     * GenAI-synthesized forensic investigation brief and chronological timeline.
     */
    AI_ANALYSIS("AI Analysis Brief", true),

    /**
     * Machine learning sidecar scoring output incorporating GraphSAGE embeddings and XGBoost fraud probabilities.
     */
    RISK_SCORE("ML Risk Score", true),

    /**
     * Formal statutory notice or crypto asset preservation request generated for VASPs.
     */
    LEGAL_NOTICE("Legal Notice", true),

    /**
     * Subjective investigator notes, operational annotations, or manual intelligence entries.
     */
    MANUAL_NOTE("Manual Note", false);

    private final String displayName;
    private final boolean requiresHash;

    /**
     * Constructs a snapshot type constant.
     *
     * @param displayName  human-readable category description
     * @param requiresHash indicates whether cryptographic SHA-256 integrity hashing is mandatory
     */
    SnapshotType(String displayName, boolean requiresHash) {
        this.displayName = displayName;
        this.requiresHash = requiresHash;
    }

    /**
     * Retrieves the human-readable display name.
     *
     * @return category title
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Indicates whether this snapshot category mandates cryptographic SHA-256 verification.
     *
     * @return {@code true} if hashing is mandatory; {@code false} for subjective notes
     */
    public boolean requiresHash() {
        return requiresHash;
    }
}
