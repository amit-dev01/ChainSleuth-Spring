-- Migration: V3__create_legal_documents_table.sql
-- Author: ChainSleuth Backend Team
-- Date: 2026-10-10
-- Description: Creates the legal_documents table with foreign key cascade, document type check constraints, and metadata JSONB

CREATE TABLE IF NOT EXISTS legal_documents (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id                     UUID        NOT NULL,
    document_type               VARCHAR(40) NOT NULL,
    generated_pdf_storage_path  VARCHAR(500),
    content_hash                CHAR(64),
    generated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    generated_by_user_id        UUID,
    officer_name                VARCHAR(200),
    officer_rank                VARCHAR(100),
    target_entity_name          VARCHAR(300),
    document_metadata           JSONB,

    CONSTRAINT fk_legal_docs_case
        FOREIGN KEY (case_id) REFERENCES cases(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_legal_docs_case_id    ON legal_documents(case_id);
CREATE INDEX idx_legal_docs_type       ON legal_documents(case_id, document_type);
CREATE INDEX idx_legal_docs_generated  ON legal_documents(generated_at DESC);

-- Constraint: valid document types
ALTER TABLE legal_documents
    ADD CONSTRAINT chk_legal_docs_type
    CHECK (document_type IN (
        'FIR_BRIEF','SECTION_91_NOTICE','SECTION_65B_CERT',
        'EXCHANGE_FREEZING_REQ','EXCHANGE_NOTICE','COURT_SUBMISSION'));

COMMENT ON TABLE  legal_documents                           IS 'Generated legal documents: FIR briefs, Section 91 notices, Section 65B certificates.';
COMMENT ON COLUMN legal_documents.generated_pdf_storage_path IS 'Supabase Storage object path. Null until PDF is uploaded.';
COMMENT ON COLUMN legal_documents.content_hash              IS 'SHA-256 of the PDF bytes. Verifies file integrity post-upload.';
