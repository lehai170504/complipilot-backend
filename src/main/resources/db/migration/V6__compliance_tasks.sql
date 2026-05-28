CREATE TABLE compliance_tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    compliance_item_id UUID,
    title VARCHAR(250) NOT NULL,
    description TEXT,
    assignee_user_id UUID,
    created_by_user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    due_date DATE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_compliance_tasks_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_compliance_tasks_compliance_item
        FOREIGN KEY (compliance_item_id)
        REFERENCES company_compliance_items (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_compliance_tasks_assignee_user
        FOREIGN KEY (assignee_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_compliance_tasks_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_compliance_tasks_status
        CHECK (status IN (
            'OPEN',
            'IN_PROGRESS',
            'DONE',
            'CANCELLED'
        )),

    CONSTRAINT ck_compliance_tasks_priority
        CHECK (priority IN (
            'LOW',
            'MEDIUM',
            'HIGH',
            'CRITICAL'
        ))
);

CREATE INDEX idx_compliance_tasks_organization_id
    ON compliance_tasks (organization_id);

CREATE INDEX idx_compliance_tasks_compliance_item_id
    ON compliance_tasks (compliance_item_id);

CREATE INDEX idx_compliance_tasks_assignee_user_id
    ON compliance_tasks (assignee_user_id);

CREATE INDEX idx_compliance_tasks_status
    ON compliance_tasks (status);

CREATE INDEX idx_compliance_tasks_due_date
    ON compliance_tasks (due_date);