CREATE TABLE evidence_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    title VARCHAR(250) NOT NULL,
    description TEXT,
    evidence_type VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    file_object_key VARCHAR(500),
    external_url VARCHAR(1000),
    content_type VARCHAR(150),
    file_size_bytes BIGINT,
    uploaded_by_user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_evidence_documents_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_evidence_documents_uploaded_by_user
        FOREIGN KEY (uploaded_by_user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_evidence_documents_evidence_type
        CHECK (evidence_type IN (
            'POLICY',
            'PROCEDURE',
            'SCREENSHOT',
            'REPORT',
            'CONTRACT',
            'CERTIFICATE',
            'AUDIT_NOTE',
            'OTHER'
        )),

    CONSTRAINT ck_evidence_documents_source_type
        CHECK (source_type IN (
            'FILE',
            'URL',
            'TEXT_NOTE'
        )),

    CONSTRAINT ck_evidence_documents_status
        CHECK (status IN (
            'DRAFT',
            'ACTIVE',
            'ARCHIVED'
        ))
);

CREATE TABLE compliance_item_evidence_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    compliance_item_id UUID NOT NULL,
    evidence_document_id UUID NOT NULL,
    linked_by_user_id UUID NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_compliance_item_evidence_links_item
        FOREIGN KEY (compliance_item_id)
        REFERENCES company_compliance_items (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_compliance_item_evidence_links_document
        FOREIGN KEY (evidence_document_id)
        REFERENCES evidence_documents (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_compliance_item_evidence_links_user
        FOREIGN KEY (linked_by_user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_compliance_item_evidence_links_item_document
        UNIQUE (compliance_item_id, evidence_document_id)
);

CREATE INDEX idx_evidence_documents_organization_id
    ON evidence_documents (organization_id);

CREATE INDEX idx_evidence_documents_uploaded_by_user_id
    ON evidence_documents (uploaded_by_user_id);

CREATE INDEX idx_evidence_documents_status
    ON evidence_documents (status);

CREATE INDEX idx_compliance_item_evidence_links_item_id
    ON compliance_item_evidence_links (compliance_item_id);

CREATE INDEX idx_compliance_item_evidence_links_document_id
    ON compliance_item_evidence_links (evidence_document_id);