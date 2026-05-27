CREATE TABLE compliance_frameworks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_system_template BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_compliance_frameworks_code UNIQUE (code)
);

CREATE TABLE compliance_requirements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    framework_id UUID NOT NULL,
    code VARCHAR(100) NOT NULL,
    title VARCHAR(250) NOT NULL,
    description TEXT,
    category VARCHAR(120),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_compliance_requirements_framework
        FOREIGN KEY (framework_id)
        REFERENCES compliance_frameworks (id)
        ON DELETE CASCADE,

    CONSTRAINT uk_compliance_requirements_framework_code
        UNIQUE (framework_id, code)
);

CREATE TABLE company_compliance_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    requirement_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    owner_user_id UUID,
    due_date DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_company_compliance_items_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_company_compliance_items_requirement
        FOREIGN KEY (requirement_id)
        REFERENCES compliance_requirements (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_company_compliance_items_owner_user
        FOREIGN KEY (owner_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT uk_company_compliance_items_org_requirement
        UNIQUE (organization_id, requirement_id),

    CONSTRAINT ck_company_compliance_items_status
        CHECK (status IN (
            'OPEN',
            'IN_PROGRESS',
            'READY_FOR_REVIEW',
            'COMPLIANT',
            'NON_COMPLIANT',
            'WAIVED'
        ))
);

CREATE INDEX idx_compliance_requirements_framework_id
    ON compliance_requirements (framework_id);

CREATE INDEX idx_company_compliance_items_organization_id
    ON company_compliance_items (organization_id);

CREATE INDEX idx_company_compliance_items_requirement_id
    ON company_compliance_items (requirement_id);

CREATE INDEX idx_company_compliance_items_status
    ON company_compliance_items (status);