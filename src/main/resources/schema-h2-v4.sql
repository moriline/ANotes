-- Подход Классический монолит: Задача принадлежит строго одному проекту. Если нужно в другом проекте — создается копия или ссылка (но физически это разные записи или просто дублирование).
-- Простота выбора пользователя: Вы делаете запрос SELECT * FROM users WHERE userId IN (SELECT userId FROM projectMembers WHERE projectId = ?)
-- механизм «Шаблонов задач» , Добавьте кнопку «Создать из шаблона» в UI. : это будет INSERT INTO tasks ... SELECT ... FROM tasks WHERE taskId = ?

-- ============================================================
-- H2 Database Schema for Task Manager (Version 4 - Simplified Monolith)
-- Pattern: Tasks belong directly to Projects
-- Features: Tags (JSON), Files, Comments, Activity Log
-- All timestamps are BIGINT (milliseconds since epoch)
-- ============================================================
SET REFERENTIAL_INTEGRITY TRUE;

-- ============================================================
-- 1. USERS TABLE
-- ============================================================
CREATE TABLE users (
    userId INTEGER AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    displayName VARCHAR(100),
    avatarUrl VARCHAR(255),
    isActive BOOLEAN DEFAULT TRUE,
    createdAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updatedAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);

-- ============================================================
-- 2. PROJECTS TABLE
-- ============================================================
CREATE TABLE projects (
    projectId INTEGER AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    ownerUserId INTEGER NOT NULL,
    color VARCHAR(7) DEFAULT '#4A90D9',
    icon VARCHAR(50),
    isActive BOOLEAN DEFAULT TRUE,
    createdAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updatedAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    FOREIGN KEY (ownerUserId) REFERENCES users(userId) ON DELETE CASCADE
);

-- ============================================================
-- 3. PROJECT ROLES
-- ============================================================
CREATE TABLE projectRoles (
    roleId INTEGER AUTO_INCREMENT PRIMARY KEY,
    roleName VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions VARCHAR(1000) -- JSON string: {"canCreate": true, ...}
);

INSERT INTO projectRoles (roleName, description, permissions) VALUES
('Admin', 'Полный доступ ко всем функциям проекта', '{"canCreate":true, "canEdit":true, "canDelete":true, "canComment":true, "canManageRoles":true}'),
('Writer', 'Может создавать и изменять задачи', '{"canCreate":true, "canEdit":true, "canDelete":false, "canComment":true, "canManageRoles":false}'),
('Reader', 'Только чтение и комментарии', '{"canCreate":false, "canEdit":false, "canDelete":false, "canComment":true, "canManageRoles":false}'),
('Disabled', 'Доступ закрыт полностью', '{"canCreate":false, "canEdit":false, "canDelete":false, "canComment":false, "canManageRoles":false}');

-- ============================================================
-- 4. PROJECT MEMBERS
-- ============================================================
CREATE TABLE projectMembers (
    projectMemberId INTEGER AUTO_INCREMENT PRIMARY KEY,
    projectId INTEGER NOT NULL,
    userId INTEGER NOT NULL,
    roleId INTEGER NOT NULL,
    joinedAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    UNIQUE(projectId, userId),
    FOREIGN KEY (projectId) REFERENCES projects(projectId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE,
    FOREIGN KEY (roleId) REFERENCES projectRoles(roleId)
);

-- ============================================================
-- 5. PROJECT STATUSES
-- ============================================================
CREATE TABLE projectStatuses (
    statusId INTEGER AUTO_INCREMENT PRIMARY KEY,
    projectId INTEGER NOT NULL,
    statusName VARCHAR(50) NOT NULL,
    statusColor VARCHAR(7) DEFAULT '#808080',
    statusOrder INTEGER DEFAULT 0,
    isDefault BOOLEAN DEFAULT FALSE,
    isClosed BOOLEAN DEFAULT FALSE,
    UNIQUE(projectId, statusName),
    FOREIGN KEY (projectId) REFERENCES projects(projectId) ON DELETE CASCADE
);

-- ============================================================
-- 6. TASKS (Центральная таблица)
-- ============================================================
CREATE TABLE tasks (
    taskId INTEGER AUTO_INCREMENT PRIMARY KEY,
    projectId INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    creatorUserId INTEGER NOT NULL,
    assignedUserId INTEGER,
    statusId INTEGER,

    -- Dates & Time
    dueDate BIGINT,
    startDate BIGINT,
    estimatedHours DOUBLE,

    -- Metadata
    tags VARCHAR(1000), -- JSON Array: ["bug", "urgent", "frontend"]
    -- priority INTEGER DEFAULT 0, -- 0: None, 1: Low, 2: Medium, 3: High, 4: Critical

    isArchived BOOLEAN DEFAULT FALSE,
    createdAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updatedAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),

    FOREIGN KEY (projectId) REFERENCES projects(projectId) ON DELETE CASCADE,
    FOREIGN KEY (creatorUserId) REFERENCES users(userId),
    FOREIGN KEY (assignedUserId) REFERENCES users(userId) ON DELETE SET NULL,
    FOREIGN KEY (statusId) REFERENCES projectStatuses(statusId) ON DELETE SET NULL
);

-- ============================================================
-- 7. FILES (Привязаны напрямую к задаче и проекту)
-- ============================================================
CREATE TABLE files (
    fileId INTEGER AUTO_INCREMENT PRIMARY KEY,
    taskId INTEGER NOT NULL,       -- Прямая связь с задачей
    projectId INTEGER NOT NULL,    -- Дублирование для быстрого доступа/проверок
    fileName VARCHAR(255) NOT NULL,
    fileOriginalName VARCHAR(255) NOT NULL,
    fileSize BIGINT NOT NULL,
    mimeType VARCHAR(100) NOT NULL,
    fileUrl VARCHAR(500) NOT NULL,
    uploadedByUserId INTEGER NOT NULL,
    checksum VARCHAR(64),
    createdAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),

    FOREIGN KEY (taskId) REFERENCES tasks(taskId) ON DELETE CASCADE,
    FOREIGN KEY (projectId) REFERENCES projects(projectId) ON DELETE CASCADE,
    FOREIGN KEY (uploadedByUserId) REFERENCES users(userId)
);

-- ============================================================
-- 8. COMMENTS
-- ============================================================
CREATE TABLE comments (
    commentId INTEGER AUTO_INCREMENT PRIMARY KEY,
    taskId INTEGER NOT NULL,
    userId INTEGER NOT NULL,
    content TEXT NOT NULL,
    createdAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    isEdited BOOLEAN DEFAULT FALSE,
    updatedAt BIGINT,

    FOREIGN KEY (taskId) REFERENCES tasks(taskId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE
);

-- ============================================================
-- 9. ACTIVITY LOG
-- ============================================================
CREATE TABLE activityLog (
    activityId INTEGER AUTO_INCREMENT PRIMARY KEY,
    projectId INTEGER NOT NULL,
    taskId INTEGER,
    userId INTEGER,
    actionType VARCHAR(50) NOT NULL, -- e.g., 'TASK_CREATED', 'STATUS_CHANGED', 'ASSIGNEE_UPDATED'
    actionDetails VARCHAR(2000),     -- JSON with details
    createdAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),

    FOREIGN KEY (projectId) REFERENCES projects(projectId) ON DELETE CASCADE,
    FOREIGN KEY (taskId) REFERENCES tasks(taskId) ON DELETE SET NULL,
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE SET NULL
);

-- ============================================================
-- INDEXES (Оптимизация производительности)
-- ============================================================
CREATE INDEX idx_projects_owner ON projects(ownerUserId);
CREATE INDEX idx_projects_active ON projects(isActive);

CREATE INDEX idx_projectMembers_project ON projectMembers(projectId);
CREATE INDEX idx_projectMembers_user ON projectMembers(userId);

CREATE INDEX idx_projectStatuses_project ON projectStatuses(projectId);
CREATE INDEX idx_projectStatuses_order ON projectStatuses(statusOrder);

CREATE INDEX idx_tasks_project ON tasks(projectId);
CREATE INDEX idx_tasks_assigned ON tasks(assignedUserId);
CREATE INDEX idx_tasks_status ON tasks(statusId);
CREATE INDEX idx_tasks_creator ON tasks(creatorUserId);
CREATE INDEX idx_tasks_dueDate ON tasks(dueDate);
CREATE INDEX idx_tasks_archived ON tasks(isArchived);

CREATE INDEX idx_files_task ON files(taskId);
CREATE INDEX idx_files_project ON files(projectId);

CREATE INDEX idx_comments_task ON comments(taskId);
CREATE INDEX idx_comments_user ON comments(userId);

CREATE INDEX idx_activity_project ON activityLog(projectId);
CREATE INDEX idx_activity_task ON activityLog(taskId);
CREATE INDEX idx_activity_user ON activityLog(userId);

-- ============================================================
-- VIEWS (Для удобного чтения данных)
-- ============================================================

-- View: Полная информация о задачах с именами статусов и исполнителей
CREATE VIEW v_projectTasks AS
SELECT
    t.taskId,
    t.projectId,
    t.title,
    t.description,
    t.tags,
    t.dueDate,
    t.startDate,
    t.isArchived,
    t.createdAt,
    t.updatedAt,

    -- Status Info
    s.statusId,
    s.statusName,
    s.statusColor,
    s.isClosed,

    -- Assignee Info
    t.assignedUserId,
    u_assign.displayName AS assignedUserName,
    u_assign.avatarUrl AS assignedUserAvatar,

    -- Creator Info
    t.creatorUserId,
    u_cre.displayName AS creatorName
FROM tasks t
LEFT JOIN projectStatuses s ON t.statusId = s.statusId
LEFT JOIN users u_assign ON t.assignedUserId = u_assign.userId
LEFT JOIN users u_cre ON t.creatorUserId = u_cre.userId;

-- View: Комментарии с контекстом задачи и пользователя
CREATE VIEW v_taskComments AS
SELECT
    c.commentId,
    c.taskId,
    t.projectId,
    t.title AS taskTitle,
    c.userId,
    u.displayName AS userDisplayName,
    u.avatarUrl AS userAvatar,
    c.content,
    c.createdAt,
    c.isEdited
FROM comments c
JOIN tasks t ON c.taskId = t.taskId
JOIN users u ON c.userId = u.userId;

-- View: Файлы с контекстом
CREATE VIEW v_taskFiles AS
SELECT
    f.fileId,
    f.taskId,
    t.title AS taskTitle,
    f.projectId,
    p.name AS projectName,
    f.fileName,
    f.fileOriginalName,
    f.fileSize,
    f.mimeType,
    f.fileUrl,
    f.uploadedByUserId,
    u.displayName AS uploadedBy,
    f.createdAt
FROM files f
JOIN tasks t ON f.taskId = t.taskId
JOIN projects p ON f.projectId = p.projectId
JOIN users u ON f.uploadedByUserId = u.userId;

-- ============================================================
-- SAMPLE DATA (Тестовые данные)
-- ============================================================

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
(1, 4, 3, EXTRACT(EPOCH FROM NOW()) * 1000); -- Olga (Reader)

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

-- ============================================================
-- TEST QUERIES (Проверка работы)
-- ============================================================

-- 1. Get all active tasks for Project 1 with assignee names
SELECT * FROM v_projectTasks WHERE projectId = 1 AND isArchived = FALSE;

-- 2. Get all files for Task 1
SELECT * FROM v_taskFiles WHERE taskId = 1;

-- 3. Get all comments for Task 1
SELECT * FROM v_taskComments WHERE taskId = 1 ORDER BY createdAt ASC;

-- 4. Find all tasks assigned to Anna (userId=2)
SELECT t.title, p.name as project, s.statusName
FROM tasks t
JOIN projects p ON t.projectId = p.projectId
JOIN projectStatuses s ON t.statusId = s.statusId
WHERE t.assignedUserId = 2 AND t.isArchived = FALSE;

-- 5. Search tasks by tag "backend" in Project 1
-- Note: H2 supports JSON_VALUE or simple string matching for JSON arrays stored as text
SELECT title, tags FROM tasks
WHERE projectId = 1 AND tags LIKE '%backend%';

/*
Работа с тегами: В поле tags хранится JSON-массив, например: ["bug", "urgent"].
Поиск: Используйте LIKE '%tag_name%' (простой вариант) или функции работы с JSON, если версия H2 поддерживает их полноценно.
Добавление: Обновляйте строку JSON целиком при изменении списка тегов.
Работа с файлами: Файлы жестко привязаны к задаче. Если задача удаляется, файл тоже удаляется (ON DELETE CASCADE). Это гарантирует чистоту хранилища (логически).
Безопасность: Все запросы на выборку задач должны сначала проверять наличие пользователя в таблице projectMembers для данного projectId.
*/

/*
-- Кого можно назначить? Запрос для выпадающего списка (Assignee Dropdown):
SELECT u.userId, u.displayName, u.avatarUrl
FROM projectMembers pm
JOIN users u ON pm.userId = u.userId
WHERE pm.projectId = :currentProjectId
AND u.isActive = TRUE
ORDER BY u.displayName ASC;
*/

/*
-- Мои задачи (Dashboard):
SELECT t.*, p.name as projectName
FROM tasks t
JOIN projects p ON t.projectId = p.projectId
WHERE t.assignedUserId = :myUserId
AND t.isArchived = FALSE
ORDER BY t.dueDate ASC;
*/
/*
-- Добавление тега: Вы читаете текущий JSON, добавляете строку в массив (проверив на дубликат в коде Java/JS), и делаете один UPDATE:

UPDATE tasks
SET tags = '["bug", "ui", "critical"]', updatedAt = ...
WHERE taskId = 123;
*/
/*
-- Поиск задач с тегом "bug" в проекте:

SELECT taskId, title, tags
FROM tasks
WHERE projectId = 5
  AND tags LIKE '%"bug"%'; -- Ищем точное вхождение слова в JSON массиве
*/

/*
Атомарность: Операции добавления/удаления должны быть транзакционными.
Защита владельца: Всегда проверяйте projects.ownerUserId. Никто не должен иметь возможность удалить владельца или понизить его роль до уровня, где он потеряет доступ (если это не передача владения, что требует отдельного сложного флоу).
Кеширование: Таблица projectMembers читается очень часто (при каждом запросе к задаче). Рекомендуется кешировать права пользователя в памяти (Redis или in-memory cache) на время сессии или на короткий промежуток времени (5-10 мин), чтобы не делать JOIN при каждом запросе к задачам.
События: Любое изменение в projectMembers должно триггерить отправку уведомления пользователю (email/push) и запись в activityLog.

1. Публичная часть (Public / Member Access)

1.1. Получить список участников проекта
Позволяет увидеть, кто работает в проекте, их роли и дату вступления. Используется для выпадающих списков при назначении задач.
Method: GET
Path: /api/v1/projects/{projectId}/members
Permissions: Любой участник проекта (projectMembers check).
Query Params:
search (optional): Фильтр по имени/email.
roleId (optional): Фильтр по конкретной роли.
Response (200 OK):

1.2. Получить доступные роли в проекте
Нужно, чтобы фронтенд знал, какие роли можно выбрать при добавлении пользователя (например, скрыть роль Admin от обычных пользователей).
Method: GET
Path: /api/v1/projects/{projectId}/roles
Permissions: Любой участник проекта.
Response (200 OK):
[
  { "roleId": 2, "roleName": "Writer", "description": "Может создавать задачи" },
  { "roleId": 3, "roleName": "Reader", "description": "Только чтение" }
]

1.3. Получить информацию о своем членстве
Пользователь проверяет свои права в конкретном проекте (кеширование прав на клиенте).
Method: GET
Path: /api/v1/projects/{projectId}/me/member
Permissions: Только сам пользователь.
Response (200 OK):

2. Административная часть (Admin / Owner Access)
Доступно только пользователям с ролью Admin в данном проекте или глобальным супер-админам.
2.1. Пригласить пользователя в проект (Назначить роль)
Основной endpoint для добавления участника.
Важно: Если пользователь еще не зарегистрирован в системе, логика может отличаться (отправка email-приглашения), но здесь рассматриваем случай с существующим userId.
Method: POST
Path: /api/v1/admin/projects/{projectId}/members
Permissions: Роль Admin в проекте.

Логика:
Проверить, что вызывающий имеет роль Admin.
Проверить, существует ли userId и roleId.
Проверить уникальность пары (projectId, userId) (чтобы не добавить дубль).
Вставить запись в projectMembers.

2.2. Изменить роль пользователя
Повышение или понижение прав участника.
Method: PUT (или PATCH)
Path: /api/v1/admin/projects/{projectId}/members/{projectMemberId}
Permissions: Роль Admin в проекте.
Ограничение безопасности: Admin не может изменить роль другого Admin, если он не владелец проекта (опционально).

2.3. Удалить пользователя из проекта
Отзывает доступ. Пользователь перестает видеть задачи проекта.
Method: DELETE
Path: /api/v1/admin/projects/{projectId}/members/{projectMemberId}
Permissions: Роль Admin в проекте.
Ограничение: Нельзя удалить владельца проекта (projects.ownerUserId).
Ограничение: Нельзя удалить самого себя через этот метод (обычно есть отдельный endpoint "Покинуть проект").
Response (204 No Content)

3.2. Проверка прав перед действием (Middleware logic)
Хотя это не прямой CRUD запрос к таблице, это критический паттерн использования данных из projectMembers.
Перед любым действием с задачей (создание, редактирование), API должно выполнять неявную проверку:
SELECT roleId, permissions FROM projectMembers JOIN projectRoles ON ... WHERE projectId = ? AND userId = ?
Если записи нет -> 403 Forbidden.
Если permissions.canEdit = false -> 403 Forbidden.
*/
