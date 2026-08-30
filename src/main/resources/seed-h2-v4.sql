-- ============================================================
-- SAMPLE DATA (Тестовые данные)
-- ============================================================
-- Вынесено из schema-h2-v4.sql отдельным скриптом: его подключает RUNSCRIPT при
-- инициализации базы, и его же переигрывает TestDataCleanup после очистки таблиц,
-- чтобы каждый тестовый класс стартовал с одного и того же состояния.
--
-- Скрипт обязан оставаться повторно исполняемым: он рассчитывает на пустые
-- таблицы данных со счётчиками identity, перезапущенными с 1 (см. TestDataCleanup).

-- 1. Users
INSERT INTO users (username, email, password, displayName, avatarUrl) VALUES
('admin', 'admin@taskmanager.com', '$2a$10$hash...', 'Alex Admin', 'https://i.pravatar.cc/150?u=admin'),
('dev_anna', 'anna@taskmanager.com', '$2a$10$hash...', 'Anna Developer', 'https://i.pravatar.cc/150?u=anna'),
('designer_max', 'max@taskmanager.com', '$2a$10$hash...', 'Max Designer', 'https://i.pravatar.cc/150?u=max'),
('tester_olga', 'olga@taskmanager.com', '$2a$10$hash...', 'Olga Tester', 'https://i.pravatar.cc/150?u=olga');

-- 2. Projects
INSERT INTO projects (name, description, ownerUserId, color, icon) VALUES
('Web Site Redesign', 'Redesign of the corporate website', 1, '#4A90D9', 'globe'),
('Mobile App API', 'Backend development for iOS/Android app', 1, '#2ECC71', 'server'),
('Marketing Q1', 'Q1 Marketing campaign planning', 2, '#E74C3C', 'bullhorn');

-- 3. Project Members
-- Project 1: Admin, Anna (Writer), Max (Writer), Olga (Reader)
INSERT INTO projectMembers (projectId, userId, roleId, joinedAt) VALUES
(1, 1, 1, EXTRACT(EPOCH FROM NOW()) * 1000), -- Admin
(1, 2, 2, EXTRACT(EPOCH FROM NOW()) * 1000), -- Anna (Writer)
(1, 3, 2, EXTRACT(EPOCH FROM NOW()) * 1000), -- Max (Writer)
(1, 4, 4, EXTRACT(EPOCH FROM NOW()) * 1000); -- Olga (Guest)

-- Project 2: Admin, Anna (Writer)
INSERT INTO projectMembers (projectId, userId, roleId, joinedAt) VALUES
(2, 1, 1, EXTRACT(EPOCH FROM NOW()) * 1000),
(2, 2, 2, EXTRACT(EPOCH FROM NOW()) * 1000);

-- Project 3: Admin, Max (Writer)
INSERT INTO projectMembers (projectId, userId, roleId, joinedAt) VALUES
(3, 1, 1, EXTRACT(EPOCH FROM NOW()) * 1000),
(3, 3, 2, EXTRACT(EPOCH FROM NOW()) * 1000);

-- 4. Project Statuses
-- Project 1 Statuses
INSERT INTO projectStatuses (projectId, statusName, statusColor, statusOrder, isDefault) VALUES
(1, 'Backlog', '#95A5A6', 0, TRUE),
(1, 'In Progress', '#3498DB', 1, FALSE),
(1, 'Review', '#F1C40F', 2, FALSE),
(1, 'Done', '#2ECC71', 3, FALSE);

-- Project 2 Statuses
INSERT INTO projectStatuses (projectId, statusName, statusColor, statusOrder, isDefault) VALUES
(2, 'To Do', '#95A5A6', 0, TRUE),
(2, 'Coding', '#3498DB', 1, FALSE),
(2, 'Testing', '#9B59B6', 2, FALSE),
(2, 'Deployed', '#2ECC71', 3, FALSE);

-- 5. Tasks
-- Task 1: In Project 1, Assigned to Anna
INSERT INTO tasks (projectId, title, description, creatorUserId, assignedUserId, statusId, dueDate, tags)
VALUES (
    1,
    'Create Homepage Mockup',
    'Design the new homepage based on brand guidelines.',
    1,
    3, -- Max (Designer)
    (SELECT statusId FROM projectStatuses WHERE projectId=1 AND statusName='In Progress'),
    EXTRACT(EPOCH FROM TIMESTAMP '2025-03-15 18:00:00') * 1000,
    '["design", "ui", "high-priority"]'
);

-- Task 2: In Project 1, Assigned to Anna (Dev)
INSERT INTO tasks (projectId, title, description, creatorUserId, assignedUserId, statusId, dueDate, tags)
VALUES (
    1,
    'Implement Login API',
    'Create JWT authentication endpoints.',
    1,
    2, -- Anna
    (SELECT statusId FROM projectStatuses WHERE projectId=1 AND statusName='Backlog'),
    EXTRACT(EPOCH FROM TIMESTAMP '2025-03-20 18:00:00') * 1000,
    '["backend", "security"]'
);

-- Task 3: In Project 2, Assigned to Anna
INSERT INTO tasks (projectId, title, description, creatorUserId, assignedUserId, statusId, tags)
VALUES (
    2,
    'Setup Database Schema',
    'Define tables for users and sessions.',
    1,
    2,
    (SELECT statusId FROM projectStatuses WHERE projectId=2 AND statusName='Coding'),
    '["db", "sql"]'
);

-- Task 4: In Project 3 (Marketing), Unassigned
INSERT INTO tasks (projectId, title, description, creatorUserId, assignedUserId, statusId, tags)
VALUES (
    3,
    'Plan Social Media Campaign',
    'Draft posts for LinkedIn and Twitter.',
    1,
    NULL,
    (SELECT statusId FROM projectStatuses WHERE projectId=3 AND statusName='To Do'), -- Assuming default exists or using ID logic
    '["marketing", "social"]'
);
-- Note: For Task 4 to work perfectly in sample, ensure Project 3 has statuses. Let's add them quickly if missing logically,
-- but assuming the insert above for Project 3 statuses was omitted for brevity, let's add them now:
INSERT INTO projectStatuses (projectId, statusName, statusColor, statusOrder, isDefault) VALUES
(3, 'Idea', '#95A5A6', 0, TRUE),
(3, 'Planning', '#3498DB', 1, FALSE),
(3, 'Active', '#F1C40F', 2, FALSE),
(3, 'Completed', '#2ECC71', 3, FALSE);

-- Update Task 4 statusId now that statuses exist
UPDATE tasks SET statusId = (SELECT statusId FROM projectStatuses WHERE projectId=3 AND statusName='Idea') WHERE taskId=4;


-- 6. Files
-- Attach a file to Task 1
INSERT INTO files (taskId, projectId, fileName, fileOriginalName, fileSize, mimeType, fileUrl, uploadedByUserId, checksum)
VALUES (
    1, 1, 'mockup_v1.fig', 'Homepage Mockup v1.fig', 2540000, 'application/octet-stream', '/files/mockup_v1.fig', 3, 'a1b2c3d4'
);

-- Attach a file to Task 2
INSERT INTO files (taskId, projectId, fileName, fileOriginalName, fileSize, mimeType, fileUrl, uploadedByUserId, checksum)
VALUES (
    2, 1, 'api_spec.json', 'API Specification.json', 15000, 'application/json', '/files/api_spec.json', 1, 'e5f6g7h8'
);

-- 7. Comments
INSERT INTO comments (taskId, userId, content, createdAt) VALUES
(1, 1, 'Please make sure to follow the new color palette.', EXTRACT(EPOCH FROM NOW()) * 1000),
(1, 3, 'Started working on it. Will share preview by Friday.', EXTRACT(EPOCH FROM NOW()) * 1000 + 10000),
(2, 2, 'Do we need OAuth2 support as well?', EXTRACT(EPOCH FROM NOW()) * 1000 + 20000),
(2, 1, 'Yes, definitely. Add it to the scope.', EXTRACT(EPOCH FROM NOW()) * 1000 + 30000);

-- 8. Activity Log
INSERT INTO activityLog (projectId, taskId, userId, actionType, actionDetails, createdAt) VALUES
(1, 1, 1, 'TASK_CREATED', '{"title": "Create Homepage Mockup"}', EXTRACT(EPOCH FROM NOW()) * 1000),
(1, 1, 1, 'ASSIGNEE_UPDATED', '{"from": null, "to": "Max Designer"}', EXTRACT(EPOCH FROM NOW()) * 1000 + 5000),
(1, 1, 3, 'STATUS_CHANGED', '{"from": "Backlog", "to": "In Progress"}', EXTRACT(EPOCH FROM NOW()) * 1000 + 60000),
(1, 2, 2, 'COMMENT_ADDED', '{"commentId": 3}', EXTRACT(EPOCH FROM NOW()) * 1000 + 20000);
