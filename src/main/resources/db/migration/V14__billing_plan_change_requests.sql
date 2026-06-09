CREATE TABLE billing_plan_change_requests (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,

    current_plan VARCHAR(50) NOT NULL,
    requested_plan VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,

    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_billing_plan_change_requests_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_billing_plan_change_requests_requested_by_user
        FOREIGN KEY (requested_by_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_billing_plan_change_requests_reviewed_by_user
        FOREIGN KEY (reviewed_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_billing_plan_change_requests_org_created_at
    ON billing_plan_change_requests(organization_id, created_at DESC);

CREATE INDEX idx_billing_plan_change_requests_status_created_at
    ON billing_plan_change_requests(status, created_at DESC);