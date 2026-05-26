CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE app_health_checks (
                                   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                   name VARCHAR(100) NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);