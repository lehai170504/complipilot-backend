CREATE TABLE compliance_framework_translations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    framework_id UUID NOT NULL,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_compliance_framework_translations_framework
        FOREIGN KEY (framework_id)
        REFERENCES compliance_frameworks (id)
        ON DELETE CASCADE,

    CONSTRAINT uk_compliance_framework_translations_framework_locale
        UNIQUE (framework_id, locale)
);

CREATE TABLE compliance_requirement_translations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requirement_id UUID NOT NULL,
    locale VARCHAR(10) NOT NULL,
    title VARCHAR(250) NOT NULL,
    description TEXT,
    category VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_compliance_requirement_translations_requirement
        FOREIGN KEY (requirement_id)
        REFERENCES compliance_requirements (id)
        ON DELETE CASCADE,

    CONSTRAINT uk_compliance_requirement_translations_requirement_locale
        UNIQUE (requirement_id, locale)
);

CREATE INDEX idx_compliance_framework_translations_framework_locale
    ON compliance_framework_translations (framework_id, locale);

CREATE INDEX idx_compliance_requirement_translations_requirement_locale
    ON compliance_requirement_translations (requirement_id, locale);

INSERT INTO compliance_framework_translations (
    framework_id,
    locale,
    name,
    description
)
SELECT
    id,
    'en',
    name,
    description
FROM compliance_frameworks
ON CONFLICT (framework_id, locale) DO NOTHING;

INSERT INTO compliance_requirement_translations (
    requirement_id,
    locale,
    title,
    description,
    category
)
SELECT
    id,
    'en',
    title,
    description,
    category
FROM compliance_requirements
ON CONFLICT (requirement_id, locale) DO NOTHING;

INSERT INTO compliance_framework_translations (
    framework_id,
    locale,
    name,
    description
)
SELECT
    id,
    'vi',
    'Baseline bảo mật cho SME',
    'Framework baseline bảo mật thực tế dành cho tổ chức vừa và nhỏ.'
FROM compliance_frameworks
WHERE code = 'SME-SECURITY-BASELINE'
ON CONFLICT (framework_id, locale) DO NOTHING;

INSERT INTO compliance_requirement_translations (
    requirement_id,
    locale,
    title,
    description,
    category
)
SELECT
    requirement.id,
    'vi',
    translation.title,
    translation.description,
    translation.category
FROM compliance_requirements requirement
JOIN compliance_frameworks framework ON framework.id = requirement.framework_id
JOIN (VALUES
    (
        'SEC-001',
        'Bật xác thực đa yếu tố',
        'Tất cả tài khoản đặc quyền và quản trị viên nên sử dụng xác thực đa yếu tố.',
        'Kiểm soát truy cập'
    ),
    (
        'SEC-002',
        'Duy trì rà soát quyền truy cập người dùng',
        'Quyền truy cập của người dùng nên được rà soát định kỳ để loại bỏ truy cập không còn hoạt động hoặc không cần thiết.',
        'Kiểm soát truy cập'
    ),
    (
        'SEC-003',
        'Lưu giữ bằng chứng cho các control quan trọng',
        'Tài liệu bằng chứng nên được thu thập và lưu giữ cho các control tuân thủ trọng yếu.',
        'Quản lý bằng chứng'
    ),
    (
        'SEC-004',
        'Xác định đầu mối ứng phó sự cố',
        'Tổ chức nên xác định người phụ trách liên hệ khi xảy ra sự cố bảo mật.',
        'Ứng phó sự cố'
    ),
    (
        'SEC-005',
        'Sao lưu dữ liệu kinh doanh quan trọng',
        'Dữ liệu kinh doanh quan trọng nên được sao lưu và có khả năng khôi phục.',
        'Liên tục kinh doanh'
    )
) AS translation(code, title, description, category) ON translation.code = requirement.code
WHERE framework.code = 'SME-SECURITY-BASELINE'
ON CONFLICT (requirement_id, locale) DO NOTHING;
