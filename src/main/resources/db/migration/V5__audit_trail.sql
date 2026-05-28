CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    actor_user_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    summary VARCHAR(500) NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_audit_events_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_audit_events_actor_user
        FOREIGN KEY (actor_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_audit_events_organization_id
    ON audit_events (organization_id);

CREATE INDEX idx_audit_events_actor_user_id
    ON audit_events (actor_user_id);

CREATE INDEX idx_audit_events_resource
    ON audit_events (resource_type, resource_id);

CREATE INDEX idx_audit_events_created_at
    ON audit_events (created_at);