CREATE TABLE organization_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    plan VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_period_started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    current_period_ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_organization_subscriptions_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE CASCADE,

    CONSTRAINT uk_organization_subscriptions_organization
        UNIQUE (organization_id),

    CONSTRAINT ck_organization_subscriptions_plan
        CHECK (plan IN ('FREE', 'PRO', 'BUSINESS', 'ENTERPRISE')),

    CONSTRAINT ck_organization_subscriptions_status
        CHECK (status IN ('ACTIVE', 'PAST_DUE', 'CANCELLED'))
);

CREATE TABLE organization_usage_counters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    period_month VARCHAR(7) NOT NULL,
    evidence_document_count BIGINT NOT NULL DEFAULT 0,
    storage_bytes BIGINT NOT NULL DEFAULT 0,
    ai_analysis_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_organization_usage_counters_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE CASCADE,

    CONSTRAINT uk_organization_usage_counters_org_period
        UNIQUE (organization_id, period_month),

    CONSTRAINT ck_organization_usage_counters_non_negative
        CHECK (
            evidence_document_count >= 0
            AND storage_bytes >= 0
            AND ai_analysis_count >= 0
        )
);

CREATE INDEX idx_organization_subscriptions_organization_id
    ON organization_subscriptions (organization_id);

CREATE INDEX idx_organization_subscriptions_plan
    ON organization_subscriptions (plan);

CREATE INDEX idx_organization_usage_counters_org_period
    ON organization_usage_counters (organization_id, period_month);

INSERT INTO organization_subscriptions (
    id,
    organization_id,
    plan,
    status,
    current_period_started_at,
    created_at,
    updated_at
)
SELECT
    uuid_generate_v4(),
    organizations.id,
    'FREE',
    'ACTIVE',
    NOW(),
    NOW(),
    NOW()
FROM organizations
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_subscriptions
    WHERE organization_subscriptions.organization_id = organizations.id
);

INSERT INTO organization_usage_counters (
    id,
    organization_id,
    period_month,
    evidence_document_count,
    storage_bytes,
    ai_analysis_count,
    created_at,
    updated_at
)
SELECT
    uuid_generate_v4(),
    organizations.id,
    TO_CHAR(NOW(), 'YYYY-MM'),
    COALESCE(evidence_counts.evidence_document_count, 0),
    COALESCE(evidence_counts.storage_bytes, 0),
    COALESCE(ai_counts.ai_analysis_count, 0),
    NOW(),
    NOW()
FROM organizations
LEFT JOIN (
    SELECT
        organization_id,
        COUNT(*) AS evidence_document_count,
        COALESCE(SUM(COALESCE(file_size_bytes, 0)), 0) AS storage_bytes
    FROM evidence_documents
    WHERE status <> 'ARCHIVED'
    GROUP BY organization_id
) evidence_counts ON evidence_counts.organization_id = organizations.id
LEFT JOIN (
    SELECT
        organization_id,
        COUNT(*) AS ai_analysis_count
    FROM evidence_ai_analyses
    WHERE analyzed_at >= DATE_TRUNC('month', NOW())
    GROUP BY organization_id
) ai_counts ON ai_counts.organization_id = organizations.id
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_usage_counters
    WHERE organization_usage_counters.organization_id = organizations.id
      AND organization_usage_counters.period_month = TO_CHAR(NOW(), 'YYYY-MM')
);
