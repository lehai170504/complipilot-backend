CREATE TABLE organization_invitations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    email VARCHAR(320) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    invited_by_user_id UUID NOT NULL REFERENCES users(id),
    accepted_by_user_id UUID REFERENCES users(id),
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_org_invitations_org_status
    ON organization_invitations (organization_id, status);

CREATE INDEX idx_org_invitations_org_email_status
    ON organization_invitations (organization_id, lower(email), status);

CREATE INDEX idx_org_invitations_token_hash
    ON organization_invitations (token_hash);
