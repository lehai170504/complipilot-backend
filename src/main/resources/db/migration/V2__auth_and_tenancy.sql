CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email VARCHAR(320) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(150) NOT NULL,
                       status VARCHAR(30) NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE organizations (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               name VARCHAR(200) NOT NULL,
                               slug VARCHAR(120) NOT NULL,
                               status VARCHAR(30) NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                               CONSTRAINT uk_organizations_slug UNIQUE (slug),
                               CONSTRAINT ck_organizations_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE organization_members (
                                      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      organization_id UUID NOT NULL,
                                      user_id UUID NOT NULL,
                                      role VARCHAR(50) NOT NULL,
                                      status VARCHAR(30) NOT NULL,
                                      joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                      CONSTRAINT fk_organization_members_organization
                                          FOREIGN KEY (organization_id)
                                              REFERENCES organizations (id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_organization_members_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES users (id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT uk_organization_members_org_user
                                          UNIQUE (organization_id, user_id),

                                      CONSTRAINT ck_organization_members_role
                                          CHECK (role IN ('OWNER', 'ADMIN', 'COMPLIANCE_MANAGER', 'MEMBER', 'AUDITOR')),

                                      CONSTRAINT ck_organization_members_status
                                          CHECK (status IN ('ACTIVE', 'INVITED', 'DISABLED'))
);

CREATE INDEX idx_organization_members_user_id
    ON organization_members (user_id);

CREATE INDEX idx_organization_members_organization_id
    ON organization_members (organization_id);

CREATE INDEX idx_users_email
    ON users (email);

CREATE INDEX idx_organizations_slug
    ON organizations (slug);