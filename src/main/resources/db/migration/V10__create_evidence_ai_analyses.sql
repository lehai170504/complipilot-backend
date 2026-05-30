CREATE TABLE evidence_ai_analyses (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    evidence_document_id UUID NOT NULL,
    analyzed_by_user_id UUID NOT NULL,

    summary TEXT NOT NULL,
    risk_level VARCHAR(50) NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,

    findings JSONB NOT NULL DEFAULT '[]'::jsonb,
    missing_information JSONB NOT NULL DEFAULT '[]'::jsonb,
    suggested_actions JSONB NOT NULL DEFAULT '[]'::jsonb,

    analyzed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_evidence_ai_analyses_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_evidence_ai_analyses_evidence_document
        FOREIGN KEY (evidence_document_id)
        REFERENCES evidence_documents (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_evidence_ai_analyses_analyzed_by_user
        FOREIGN KEY (analyzed_by_user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_evidence_ai_analyses_org_evidence_latest
    ON evidence_ai_analyses (organization_id, evidence_document_id, analyzed_at DESC);

CREATE INDEX idx_evidence_ai_analyses_evidence_document
    ON evidence_ai_analyses (evidence_document_id);