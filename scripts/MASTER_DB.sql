INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('cfa9a58d-e0de-4e50-bdb0-a54ec882921f'::uuid, 'SUPER_ADMIN', 'Super Admin', 'Full platform owner access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);
INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('160bccf8-3ca8-45f1-b39c-e64c148a3423'::uuid, 'ADMIN', 'Admin', 'Institution administrator access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);
INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('aaa9b9f4-8850-4452-a342-62decb9ffc99'::uuid, 'PRINCIPAL', 'Principal', 'Academic and operational leadership access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);
INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('dd417387-d18e-4c3a-a764-89549283d598'::uuid, 'TEACHER', 'Teacher', 'Teaching staff access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);
INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('26666b09-37ac-4652-a704-c58fe150754d'::uuid, 'ACCOUNTANT', 'Accountant', 'Fees and finance access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);
INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('35054eb7-b1ba-46d1-afae-4833757471f2'::uuid, 'RECEPTIONIST', 'Receptionist', 'Front office access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);
INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('e6058755-10bf-411e-98f2-aad4a084025b'::uuid, 'STUDENT', 'Student', 'Student portal access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);
INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('cdb221dc-4612-40b9-b203-3aa0b12d48cb'::uuid, 'PARENT', 'Parent', 'Parent portal access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);
INSERT INTO roles
(id, "name", display_name, description, system_role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES('3180c332-9198-420f-bc7b-dfd1693d3d5b'::uuid, 'WARDEN', 'Warden', 'Hostel operations access.', true, '2026-05-23 12:26:34.900', '2026-05-23 12:26:34.900', 'flyway', 'flyway', false, NULL, NULL, 0);


INSERT INTO user_accounts
(id, email, username, password_hash, first_name, last_name, phone_number, status,
 failed_login_attempts, locked_until, last_login_at, password_changed_at,
 created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, "version")
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid, 'admin@school.com', 'admin',
 '$2a$10$CwTycUXWue0Thq9StjUM0uJ8Ff9QW0d96e0r1s6Tx2z.d4b4YuN/a',
 'System', 'Admin', '9999999999', 'ACTIVE',
 0, NULL, NULL, NOW(),
 NOW(), NOW(), 'manual', 'manual', false, NULL, NULL, 0),

('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid, 'principal@school.com', 'principal',
 '$2a$10$CwTycUXWue0Thq9StjUM0uJ8Ff9QW0d96e0r1s6Tx2z.d4b4YuN/a',
 'Main', 'Principal', '9999999998', 'ACTIVE',
 0, NULL, NULL, NOW(),
 NOW(), NOW(), 'manual', 'manual', false, NULL, NULL, 0),

('cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid, 'teacher1@school.com', 'teacher1',
 '$2a$10$CwTycUXWue0Thq9StjUM0uJ8Ff9QW0d96e0r1s6Tx2z.d4b4YuN/a',
 'Amit', 'Sharma', '9999999997', 'ACTIVE',
 0, NULL, NULL, NOW(),
 NOW(), NOW(), 'manual', 'manual', false, NULL, NULL, 0);
 
 
 INSERT INTO user_roles (user_id, role_id)
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid, 'cfa9a58d-e0de-4e50-bdb0-a54ec882921f'::uuid), -- SUPER_ADMIN
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid, 'aaa9b9f4-8850-4452-a342-62decb9ffc99'::uuid), -- PRINCIPAL
('cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid, 'dd417387-d18e-4c3a-a764-89549283d598'::uuid); -- TEACHER