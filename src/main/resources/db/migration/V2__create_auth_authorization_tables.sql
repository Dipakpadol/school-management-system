CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(120) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_permissions_code_active ON permissions (code) WHERE deleted = FALSE;

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(60) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_roles_name_active ON roles (name) WHERE deleted = FALSE;

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(160) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80),
    phone_number VARCHAR(30),
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_user_accounts_email_active ON user_accounts (LOWER(email)) WHERE deleted = FALSE;
CREATE UNIQUE INDEX ux_user_accounts_username_active ON user_accounts (LOWER(username)) WHERE deleted = FALSE;
CREATE INDEX idx_user_accounts_status ON user_accounts (status);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES user_accounts (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_accounts (id),
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_hash VARCHAR(128),
    created_ip VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_refresh_tokens_hash_active ON refresh_tokens (token_hash) WHERE deleted = FALSE;
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens (user_id, revoked_at, expires_at) WHERE deleted = FALSE;

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_accounts (id),
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    requested_ip VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_password_reset_tokens_hash_active ON password_reset_tokens (token_hash) WHERE deleted = FALSE;
CREATE INDEX idx_password_reset_tokens_user_active ON password_reset_tokens (user_id, used_at, expires_at) WHERE deleted = FALSE;

INSERT INTO permissions (code, name, description, created_by, updated_by)
VALUES
    ('AUTH_PASSWORD_CHANGE', 'Change own password', 'Allows an authenticated user to change their own password.', 'flyway', 'flyway'),
    ('USERS_READ', 'Read users', 'Allows reading user accounts, roles, and permissions.', 'flyway', 'flyway'),
    ('USERS_CREATE', 'Create users', 'Allows creating user accounts.', 'flyway', 'flyway'),
    ('USERS_UPDATE', 'Update users', 'Allows updating user accounts and assigning roles.', 'flyway', 'flyway'),
    ('USERS_DELETE', 'Delete users', 'Allows soft deleting user accounts.', 'flyway', 'flyway'),
    ('STUDENTS_READ', 'Read students', 'Allows reading student profiles.', 'flyway', 'flyway'),
    ('STUDENTS_CREATE', 'Create students', 'Allows creating student profiles.', 'flyway', 'flyway'),
    ('STUDENTS_UPDATE', 'Update students', 'Allows updating student profiles.', 'flyway', 'flyway'),
    ('STUDENTS_DELETE', 'Delete students', 'Allows soft deleting student profiles.', 'flyway', 'flyway'),
    ('ACADEMIC_READ', 'Read academic setup', 'Allows reading academic setup data.', 'flyway', 'flyway'),
    ('ACADEMIC_MANAGE', 'Manage academic setup', 'Allows managing classes, sections, subjects, and sessions.', 'flyway', 'flyway'),
    ('FEES_READ', 'Read fees', 'Allows reading fee records.', 'flyway', 'flyway'),
    ('FEES_MANAGE', 'Manage fees', 'Allows managing fee structures, invoices, and collections.', 'flyway', 'flyway'),
    ('HOSTEL_READ', 'Read hostel', 'Allows reading hostel records.', 'flyway', 'flyway'),
    ('HOSTEL_MANAGE', 'Manage hostel', 'Allows managing hostel rooms and allocations.', 'flyway', 'flyway'),
    ('ATTENDANCE_READ', 'Read attendance', 'Allows reading attendance records.', 'flyway', 'flyway'),
    ('ATTENDANCE_MARK', 'Mark attendance', 'Allows marking attendance.', 'flyway', 'flyway'),
    ('REPORTS_READ', 'Read reports', 'Allows viewing reports.', 'flyway', 'flyway'),
    ('NOTIFICATIONS_SEND', 'Send notifications', 'Allows sending notifications.', 'flyway', 'flyway'),
    ('SETTINGS_READ', 'Read settings', 'Allows reading system settings.', 'flyway', 'flyway'),
    ('SETTINGS_UPDATE', 'Update settings', 'Allows updating system settings.', 'flyway', 'flyway');

INSERT INTO roles (name, display_name, description, created_by, updated_by)
VALUES
    ('SUPER_ADMIN', 'Super Admin', 'Full platform owner access.', 'flyway', 'flyway'),
    ('ADMIN', 'Admin', 'Institution administrator access.', 'flyway', 'flyway'),
    ('PRINCIPAL', 'Principal', 'Academic and operational leadership access.', 'flyway', 'flyway'),
    ('TEACHER', 'Teacher', 'Teaching staff access.', 'flyway', 'flyway'),
    ('ACCOUNTANT', 'Accountant', 'Fees and finance access.', 'flyway', 'flyway'),
    ('RECEPTIONIST', 'Receptionist', 'Front office access.', 'flyway', 'flyway'),
    ('STUDENT', 'Student', 'Student portal access.', 'flyway', 'flyway'),
    ('PARENT', 'Parent', 'Parent portal access.', 'flyway', 'flyway'),
    ('WARDEN', 'Warden', 'Hostel operations access.', 'flyway', 'flyway');

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
CROSS JOIN permissions
WHERE roles.name = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'AUTH_PASSWORD_CHANGE',
    'USERS_READ', 'USERS_CREATE', 'USERS_UPDATE', 'USERS_DELETE',
    'STUDENTS_READ', 'STUDENTS_CREATE', 'STUDENTS_UPDATE', 'STUDENTS_DELETE',
    'ACADEMIC_READ', 'ACADEMIC_MANAGE',
    'FEES_READ', 'FEES_MANAGE',
    'HOSTEL_READ', 'HOSTEL_MANAGE',
    'ATTENDANCE_READ', 'ATTENDANCE_MARK',
    'REPORTS_READ',
    'NOTIFICATIONS_SEND',
    'SETTINGS_READ', 'SETTINGS_UPDATE'
)
WHERE roles.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'AUTH_PASSWORD_CHANGE',
    'USERS_READ',
    'STUDENTS_READ', 'STUDENTS_UPDATE',
    'ACADEMIC_READ', 'ACADEMIC_MANAGE',
    'ATTENDANCE_READ',
    'REPORTS_READ',
    'NOTIFICATIONS_SEND',
    'SETTINGS_READ'
)
WHERE roles.name = 'PRINCIPAL';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'AUTH_PASSWORD_CHANGE',
    'STUDENTS_READ',
    'ACADEMIC_READ',
    'ATTENDANCE_READ', 'ATTENDANCE_MARK',
    'REPORTS_READ'
)
WHERE roles.name = 'TEACHER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'AUTH_PASSWORD_CHANGE',
    'STUDENTS_READ',
    'FEES_READ', 'FEES_MANAGE',
    'REPORTS_READ'
)
WHERE roles.name = 'ACCOUNTANT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'AUTH_PASSWORD_CHANGE',
    'STUDENTS_READ', 'STUDENTS_CREATE', 'STUDENTS_UPDATE',
    'FEES_READ',
    'NOTIFICATIONS_SEND'
)
WHERE roles.name = 'RECEPTIONIST';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'AUTH_PASSWORD_CHANGE',
    'STUDENTS_READ',
    'ACADEMIC_READ',
    'ATTENDANCE_READ',
    'FEES_READ'
)
WHERE roles.name = 'STUDENT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'AUTH_PASSWORD_CHANGE',
    'STUDENTS_READ',
    'ACADEMIC_READ',
    'ATTENDANCE_READ',
    'FEES_READ'
)
WHERE roles.name = 'PARENT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'AUTH_PASSWORD_CHANGE',
    'STUDENTS_READ',
    'HOSTEL_READ', 'HOSTEL_MANAGE',
    'ATTENDANCE_READ',
    'REPORTS_READ'
)
WHERE roles.name = 'WARDEN';
