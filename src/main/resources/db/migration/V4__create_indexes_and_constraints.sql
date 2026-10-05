-- Migration: V4__create_indexes_and_constraints.sql
-- Author: ChainSleuth Backend Team
-- Date: 2026-10-10
-- Description: Composite and partial performance indexes, and documentation for Supabase Row Level Security (RLS)

-- Performance indexes for common query patterns identified in CaseRepository

-- Composite index: finding active cases for an org (SUPERVISOR view)
CREATE INDEX idx_cases_org_active
    ON cases(organization_id, updated_at DESC)
    WHERE deleted_at IS NULL AND organization_id IS NOT NULL;

-- Composite index: risk score filtering (high-risk case dashboard)
CREATE INDEX idx_cases_risk_score
    ON cases(user_id, ml_risk_score DESC)
    WHERE deleted_at IS NULL AND ml_risk_score IS NOT NULL;

-- Composite index: traversal status monitoring (admin dashboard)
CREATE INDEX idx_cases_traversal_running
    ON cases(traversal_status, traversal_started_at)
    WHERE traversal_status = 'RUNNING';

-- Supabase Row Level Security (RLS) policy placeholders
-- NOTE: RLS is managed by Supabase directly on the Supabase dashboard
-- or via Supabase migrations. The Spring Boot backend uses service-role 
-- key with RLS bypass for trusted server-side queries.
-- Document this in a comment for the DBA/Supabase admin:

-- SUPABASE RLS NOTE:
-- These tables should have RLS enabled in Supabase with policies:
--   cases: SELECT WHERE auth.uid() = user_id
--   evidence_snapshots: SELECT via JOIN to cases (user_id check)
--   legal_documents: SELECT via JOIN to cases (user_id check)
-- The Spring Boot backend uses service_role key (bypasses RLS) — 
-- RLS is for direct Supabase client access from the frontend.

COMMENT ON INDEX idx_cases_user_active       IS 'Primary query index: user case list, active only.';
COMMENT ON INDEX idx_cases_traversal_running IS 'Monitoring: find all currently-running traversals.';
