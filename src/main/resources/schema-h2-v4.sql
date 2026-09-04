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
    -- Глобальный администратор: доступ к /api/admin/users. Ролей из projectRoles
    -- для этого не хватает: они действуют только внутри одного проекта.
    isAdmin BOOLEAN NOT NULL DEFAULT FALSE,
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
    tags VARCHAR(1000), -- JSON Array: ["backend", "q1-2026"]
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

-- roleId фиксированы: 1=Admin, 2=Manager, 3=Developer, 4=Guest, 5=Disabled, 6=Client.
-- Тесты и seed завязаны на эти номера — новые роли только дописывать в конец.
-- Client (заказчик) по правам совпадает с Guest, но это внешняя роль: код скрывает
-- от неё INTERNAL/SYSTEM-комментарии и события (см. PermissionService.seesInternalContent).
INSERT INTO projectRoles (roleName, description, permissions) VALUES
('Admin', 'Полный доступ ко всем функциям проекта', '["task:create","task:read","task:update","task:delete","task:assign","project:read","project:update","project:delete","project:manage_members","role:manage","user:manage"]'),
('Manager', 'Управление задачами и участниками', '["task:create","task:read","task:update","task:delete","task:assign","project:read","project:manage_members"]'),
('Developer', 'Разработка и работа с задачами', '["task:create","task:read","task:update","task:assign","project:read"]'),
('Guest', 'Только чтение', '["task:read","project:read"]'),
('Disabled', 'Доступ закрыт полностью', '[]'),
('Client', 'Заказчик: просмотр задач и публичные комментарии, без внутренней переписки и событий', '["task:read","project:read"]');

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
    discussion TEXT,    -- JSON Array: Discussion blocks
    summary TEXT,       -- Decision/Summary
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
    -- PUBLIC | INTERNAL | SYSTEM. Заведено заранее под разделение «команда / заказчик»:
    -- добавить колонку в непустую таблицу дороже, чем провести её сейчас дефолтом.
    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
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
    actionType VARCHAR(50) NOT NULL,
    actionDetails VARCHAR(2000),
    -- PUBLIC | INTERNAL | SYSTEM. См. комментарий у comments.visibility.
    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    createdAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),

    FOREIGN KEY (projectId) REFERENCES projects(projectId) ON DELETE CASCADE,
    FOREIGN KEY (taskId) REFERENCES tasks(taskId) ON DELETE SET NULL,
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE SET NULL
);

-- ============================================================
-- 10. TIME ENTRIES (Hybrid tracking)
-- ============================================================
CREATE TABLE timeEntries (
    entryId INTEGER AUTO_INCREMENT PRIMARY KEY,
    taskId INTEGER NOT NULL,
    userId INTEGER NOT NULL,
    seconds BIGINT NOT NULL,
    description VARCHAR(500),
    startTime BIGINT NOT NULL, -- Epoch ms
    createdAt BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),

    FOREIGN KEY (taskId) REFERENCES tasks(taskId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE
);

CREATE INDEX idx_timeEntries_task ON timeEntries(taskId);
CREATE INDEX idx_timeEntries_user ON timeEntries(userId);
CREATE INDEX idx_timeEntries_start ON timeEntries(startTime);

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
    c.visibility,
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
-- Сид живёт в отдельном файле, чтобы тесты могли переиграть только его, не
-- пересоздавая схему (см. TestDataCleanup).
RUNSCRIPT FROM 'classpath:seed-h2-v4.sql';

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
