CREATE INDEX IF NOT EXISTS idx_organization_members_org_role_status
    ON organization_members (organization_id, role, status);

CREATE INDEX IF NOT EXISTS idx_organization_members_org_status
    ON organization_members (organization_id, status);
