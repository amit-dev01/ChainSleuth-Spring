-- Migration: V2__create_evidence_snapshots_table.sql
-- Author: ChainSleuth Backend Team
-- Date: 2026-10-10
-- Description: Creates the evidence_snapshots table with JSONB payload, cryptographic hash constraints, and foreign key cascade

CREATE TABLE IF NOT EXISTS evidence_snapshots (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id             UUID        NOT NULL,
    snapshot_type       VARCHAR(30) NOT NULL,
    data_hash           CHAR(64),               -- SHA-256 hex, exactly 64 chars
    data_payload        JSONB       NOT NULL,
    signature_token     VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by_user_id  UUID,

    CONSTRAINT fk_evidence_case
        FOREIGN KEY (case_id) REFERENCES cases(id)
        ON DELETE CASCADE                       -- cascade: delete snapshots when case deleted
);

CREATE INDEX idx_evidence_case_id       ON evidence_snapshots(case_id);
CREATE INDEX idx_evidence_snapshot_type ON evidence_snapshots(case_id, snapshot_type);
CREATE INDEX idx_evidence_created_at    ON evidence_snapshots(created_at DESC);

-- GIN index on JSONB for potential payload queries
CREATE INDEX idx_evidence_payload_gin
    ON evidence_snapshots USING GIN (data_payload);

-- Constraint: valid snapshot types
ALTER TABLE evidence_snapshots
    ADD CONSTRAINT chk_evidence_snapshot_type
    CHECK (snapshot_type IN (
        'GRAPH_EXPORT','TRANSACTION_LOG','AI_ANALYSIS',
        'RISK_SCORE','LEGAL_NOTICE','MANUAL_NOTE'));

-- Constraint: if snapshot_type is not MANUAL_NOTE, data_hash must be present
ALTER TABLE evidence_snapshots
    ADD CONSTRAINT chk_evidence_hash_required
    CHECK (
        snapshot_type = 'MANUAL_NOTE' OR data_hash IS NOT NULL
    );

-- Constraint: SHA-256 hex format (64 lowercase hex chars)
ALTER TABLE evidence_snapshots
    ADD CONSTRAINT chk_evidence_hash_format
    CHECK (
        data_hash IS NULL OR data_hash ~ '^[a-f0-9]{64}$'
    );

COMMENT ON TABLE  evidence_snapshots          IS 'Immutable, cryptographically-signed evidence snapshots for Section 65B admissibility.';
COMMENT ON COLUMN evidence_snapshots.data_hash IS 'SHA-256 hex of the data_payload content. Required for all types except MANUAL_NOTE.';
