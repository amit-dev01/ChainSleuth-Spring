package com.chainsleuth.data.entity;

/**
 * Enumeration representing the lifecycle stages of a forensic investigation case.
 * <p>
 * <b>State Machine Progression:</b><br>
 * The case lifecycle follows a strict forward-only linear progression designed to uphold
 * chain-of-custody integrity and electronic evidence discovery standards:
 * <pre>{@code
 *   DRAFT ──► ACTIVE ──► PENDING_REVIEW ──► SUBMITTED_TO_LEA ──► CLOSED ──► ARCHIVED
 * }</pre>
 * Backward transitions are forbidden by domain validation rules. Once a case enters
 * {@link #SUBMITTED_TO_LEA}, {@link #CLOSED}, or {@link #ARCHIVED}, investigative parameters
 * are locked against further modification.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 2A — Relational Data Layer &amp; Persistence Infrastructure</li>
 *   <li><b>Platform Component:</b> Case Lifecycle Domain Model</li>
 *   <li><b>Interacts with:</b> {@link CaseEntity}</li>
 * </ul>
 */
public enum CaseStatus {

    /**
     * Case initialized by the investigator; parameters and root wallet assigned but investigation not active.
     */
    DRAFT("Draft", false),

    /**
     * Case is actively being investigated; graph traversals and evidence collections are ongoing.
     */
    ACTIVE("Active", false),

    /**
     * Investigation completed; case is locked awaiting supervisor or team lead sign-off.
     */
    PENDING_REVIEW("Pending Review", false),

    /**
     * Formal investigation brief and evidence package submitted to a Law Enforcement Agency (LEA).
     */
    SUBMITTED_TO_LEA("Submitted to LEA", false),

    /**
     * Investigation formally concluded; terminal state.
     */
    CLOSED("Closed", true),

    /**
     * Retained for statutory compliance and archival reference; immutable terminal state.
     */
    ARCHIVED("Archived", true);

    private final String displayName;
    private final boolean terminal;

    /**
     * Constructs a case status constant.
     *
     * @param displayName human-readable label for UI rendering
     * @param terminal    indicates whether no further state transitions are permitted
     */
    CaseStatus(String displayName, boolean terminal) {
        this.displayName = displayName;
        this.terminal = terminal;
    }

    /**
     * Retrieves the human-readable display name.
     *
     * @return display label
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Indicates whether this status represents a terminal state.
     *
     * @return {@code true} if terminal (CLOSED or ARCHIVED); {@code false} otherwise
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Checks whether an investigation in this status permits modification of its core parameters.
     * <p>
     * Only cases in {@link #DRAFT} or {@link #ACTIVE} status are editable. Once submitted for review,
     * submitted to LEA, closed, or archived, the record is locked against alterations.
     * </p>
     *
     * @return {@code true} if status is {@link #DRAFT} or {@link #ACTIVE}; {@code false} otherwise
     */
    public boolean isEditable() {
        return this == DRAFT || this == ACTIVE;
    }
}
