-- Migration: V1__create_cases_table.sql
-- Author: ChainSleuth Backend Team
-- Date: 2026-10-10
-- Description: Creates the core cases table, indexes, updated_at trigger, and check constraints

CREATE TABLE IF NOT EXISTS cases (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL,
    organization_id         VARCHAR(100),
    title                   VARCHAR(200)    NOT NULL,
    description             TEXT,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    blockchain              VARCHAR(20)     NOT NULL,
    root_wallet_address     VARCHAR(100)    NOT NULL,
    total_tracked_usd       NUMERIC(24, 8),
    max_traversal_depth     INTEGER         NOT NULL DEFAULT 5,
    traversal_status        VARCHAR(20),
    traversal_started_at    TIMESTAMPTZ,
    traversal_completed_at  TIMESTAMPTZ,
    total_nodes_discovered  INTEGER         NOT NULL DEFAULT 0,
    total_edges_discovered  INTEGER         NOT NULL DEFAULT 0,
    ml_risk_score           DOUBLE PRECISION,
    ai_analysis_summary     TEXT,
    version                 BIGINT          NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMPTZ,    -- Soft delete: NULL = active
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by_user_id      UUID,
    last_modified_by_user_id UUID
);

-- Indexes (matching @Table indexes in CaseEntity)
CREATE INDEX idx_cases_user_id          ON cases(user_id);
CREATE INDEX idx_cases_organization_id  ON cases(organization_id)   WHERE organization_id IS NOT NULL;
CREATE INDEX idx_cases_status           ON cases(status);
CREATE INDEX idx_cases_blockchain       ON cases(blockchain);
CREATE INDEX idx_cases_root_wallet      ON cases(root_wallet_address);
CREATE INDEX idx_cases_deleted_at       ON cases(deleted_at)         WHERE deleted_at IS NULL;

-- Partial index: active cases by user (most common query pattern)
CREATE INDEX idx_cases_user_active 
    ON cases(user_id, updated_at DESC) 
    WHERE deleted_at IS NULL;

-- Constraint: valid status values
ALTER TABLE cases 
    ADD CONSTRAINT chk_cases_status 
    CHECK (status IN ('DRAFT','ACTIVE','PENDING_REVIEW',
                      'SUBMITTED_TO_LEA','CLOSED','ARCHIVED'));

-- Constraint: valid blockchain values
ALTER TABLE cases 
    ADD CONSTRAINT chk_cases_blockchain 
    CHECK (blockchain IN ('BASE','ETHEREUM','TRON','SOLANA','BITCOIN'));

-- Constraint: traversal depth range
ALTER TABLE cases 
    ADD CONSTRAINT chk_cases_max_depth 
    CHECK (max_traversal_depth BETWEEN 1 AND 10);

-- Function + trigger: auto-update updated_at on any row change
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cases_updated_at
    BEFORE UPDATE ON cases
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comment on table and key columns
COMMENT ON TABLE  cases                       IS 'ChainSleuth investigation cases';
COMMENT ON COLUMN cases.deleted_at            IS 'NULL = active case. Set to timestamp when soft-deleted.';
COMMENT ON COLUMN cases.version               IS 'Optimistic locking version counter (Hibernate @Version).';
COMMENT ON COLUMN cases.root_wallet_address   IS 'The starting blockchain address for BFS traversal.';
COMMENT ON COLUMN cases.traversal_status      IS 'NULL | RUNNING | COMPLETED | FAILED';
