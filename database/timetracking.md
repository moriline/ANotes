# Варианты отслеживания времени для TaskDto

## Текущая структура TaskDto

```json
{
  "id": 1,
  "title": "Implement login API",
  "description": "Add OAuth2 login endpoint",
  "status": "IN_PROGRESS",
  "assigneeId": 42,
  "assigneeUsername": "john",
  "dueDate": "2026-04-15",
  "createdAt": "2026-04-01T09:00:00",
  "totalSeconds": 0
}
```

---

## Сравнение с Jira API

Jira Time Tracking API (`/rest/api/2/configuration/timetracking/options`) возвращает:

```json
{
  "defaultUnit": "hour",
  "timeFormat": "pretty",
  "workingDaysPerWeek": 5.5,
  "workingHoursPerDay": 7.6
}
```

**Ключевые отличия от нашего Option 1:**

| Аспект | Jira | Наш Option 1 |
|--------|------|--------------|
| Где хранится | Глобальные настройки + worklog на задачу | Поля в каждой задаче |
| Что включается | Настройки времени, формат отображения | estimatedSeconds, totalSeconds |
| Granularity | worklog (кто, когда, сколько) в секундах | Просто сумма |
| Единица | секунды | секунды |
| API | Отдельный эндпоинт time tracking | Внутри задачи |

**Вывод:** Jira разделяет глобальные настройки (workingDaysPerWeek, workingHoursPerDay) и per-issue записи времени. Наш Option 1 ближе к полям задачи, но можно добавить глобальные настройки.

---

## Option 1: Простые поля секунд (Рекомендуется для MVP)

Добавляем числовые поля для оценки и потраченного времени в секундах. Минимальное изменение схемы.

**Добавленные поля:**
- `estimatedSeconds` (BIGINT) - Начальная оценка в секундах
- `totalSeconds` (BIGINT) - Всего затрачено в секундах

```json
{
  "id": 1,
  "title": "Implement login API",
  "description": "Add OAuth2 endpoint",
  "status": "IN_PROGRESS",
  "assigneeId": 42,
  "assigneeUsername": "john",
  "dueDate": "2026-04-15",
  "createdAt": "2026-04-01T09:00:00",
  "estimatedSeconds": 28800,
  "totalSeconds": 19800
}
```

**Плюсы:** Просто, минимум кода, легко суммировать/агрегировать  
**Минусы:** Нет деталей по записям, нет временных меток

---

## Option 2: Массив записей времени

Храним детальные записи с датой, пользователем и секундами.

**Добавленное поле:**
- `timeEntries` (TimeEntry[]) - Массив записей времени

```java
public class TimeEntry {
    public Long userId;
    public String username;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public Long seconds;
    public String description;
}
```

**JSON пример:**
```json
{
  "id": 1,
  "title": "Implement login API",
  "status": "IN_PROGRESS",
  "estimatedSeconds": 28800,
  "timeEntries": [
    {
      "userId": 42,
      "username": "john",
      "startTime": "2026-04-10T09:00:00",
      "endTime": "2026-04-10T12:30:00",
      "seconds": 12600,
      "description": "Setup OAuth2 provider"
    },
    {
      "userId": 42,
      "username": "john",
      "startTime": "2026-04-11T14:00:00",
      "endTime": "2026-04-11T15:30:00",
      "seconds": 5400,
      "description": "Token endpoint"
    }
  ]
}
```

**Плюсы:** Детальное отслеживание, аудит, видно кто и когда работал  
**Минусы:** Сложнее, больший JSON, требует обновления клиента

---

## Option 3: Гибрид (Секунды + Записи)

Объединяет Option 1 и 2 для полной гибкости.

```json
{
  "id": 1,
  "title": "Implement login API",
  "status": "IN_PROGRESS",
  "estimatedSeconds": 57600,
  "timeEntries": [
    {
      "userId": 42,
      "username": "alice",
      "startTime": "2026-04-10T09:00:00",
      "endTime": "2026-04-10T12:30:00",
      "seconds": 12600,
      "description": "Setup OAuth2 provider"
    },
    {
      "userId": 15,
      "username": "bob",
      "startTime": "2026-04-10T14:00:00",
      "endTime": "2026-04-10T17:00:00",
      "seconds": 10800,
      "description": "API endpoint implementation"
    },
    {
      "userId": 42,
      "username": "alice",
      "startTime": "2026-04-11T10:00:00",
      "endTime": "2026-04-11T13:00:00",
      "seconds": 10800,
      "description": "Token handler"
    },
    {
      "userId": 23,
      "username": "charlie",
      "startTime": "2026-04-12T09:00:00",
      "endTime": "2026-04-12T11:30:00",
      "seconds": 9000,
      "description": "Code review"
    },
    {
      "userId": 15,
      "username": "bob",
      "startTime": "2026-04-14T15:00:00",
      "endTime": "2026-04-14T18:00:00",
      "seconds": 10800,
      "description": "Fix bugs"
    }
  ]
}
```

**Вычисляется:** `totalSeconds` = 12600 + 10800 + 10800 + 9000 + 10800 = **54000 секунд** (= 15 часов)

---

### SQL: Сумма секунд по пользователям за месяц

```sql
SELECT 
    u.id AS userId,
    u.username,
    SUM(te.seconds) / 3600.0 AS hoursWorked
FROM users u
CROSS JOIN LATERAL JSON_TABLE(
    tasks.timeEntries,
    '$[*]' COLUMNS (
        userId NUMBER PATH '$.userId',
        seconds NUMBER PATH '$.seconds'
    )
) AS te
WHERE te.userId = u.id
  AND MONTH(CAST(te.startTime AS DATE)) = 4
  AND YEAR(CAST(te.startTime AS DATE)) = 2026
GROUP BY u.id, u.username
ORDER BY hoursWorked DESC;
```

**Результат:**

| userId | username | hoursWorked |
|--------|----------|------------|
| 42     | alice   | 6.5        |
| 15     | bob     | 6.0        |
| 23     | charlie | 2.5        |

**Примечание:** Для H2 может потребоваться иной синтаксис парсинга JSON. Альтернатива — десериализовать JSON в Java и считать там.

---

## Рекомендация

**Option 1** для быстрой реализации — просто два числовых поля.  
**Option 2/3** для продакшена, когда нужны аудит и отслеживание по пользователям.

Для Quarkus JSON в TEXT колонке использовать `io.quarkus.hibernate.orm.panache.common.PanacheEntity` с сериализацией через Jackson.

---

## Вариант с отдельной таблицей timeEntries (Рекомендуется)

timeEntries хранятся в **отдельной таблице**, не в JSON. Это даёт быстрый поиск и отчёты.

### Таблица timeEntries (H2)

```sql
CREATE TABLE timeEntries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taskId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    startTime TIMESTAMP NOT NULL,    -- TIMESTAMP в H2
    endTime TIMESTAMP,              -- nullable если таймер запущен
    seconds BIGINT NOT NULL,        -- время в секундах
    description VARCHAR(500),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (taskId) REFERENCES tasks(id),
    FOREIGN KEY (userId) REFERENCES users(id)
);
```

### Структура TaskDto (БЕЗ timeEntries)

```json
{
  "id": 1,
  "title": "Implement login API",
  "status": "IN_PROGRESS",
  "totalSeconds": 54000
}
```

**Примечание:** Добавляем поле `totalSeconds` в TaskDto — вычисляется и изменяется только при изменении записей в таблице timeEntries.

### Java Entity (H2)

```java
@Entity
@Table(name = "timeEntries")
public class TimeEntry extends PanacheEntity {
    public Long taskId;
    public Long userId;
    public LocalDateTime startTime;   // TIMESTAMP in H2
    public LocalDateTime endTime;     // nullable
    public Long seconds;              // BIGINT - время в секундах
    public String description;
    public LocalDateTime createdAt;
}
```

**Плюсы:**
- Быстрые SQL-запросы для отчётов (SUM, GROUP BY, фильтрация по датам)
- Внешние ключи для целостности
- Индексы для поиска
- Не нужно парсить JSON

**Запрос: сумма секунд по пользователям за месяц (H2):**
```sql
SELECT 
    u.id AS userId,
    u.username,
    SUM(te.seconds) AS totalSeconds
FROM timeEntries te
JOIN users u ON te.userId = u.id
WHERE DATEADD('MONTH', MONTH(te.startTime) - 1, DATE_TRUNC('YEAR', te.startTime)) = DATE '2026-04-01'
GROUP BY u.id, u.username
ORDER BY totalSeconds DESC;
```

**Или проще (H2):**
```sql
SELECT 
    u.id AS userId,
    u.username,
    SUM(te.seconds) AS totalSeconds
FROM timeEntries te
JOIN users u ON te.userId = u.id
WHERE te.startTime >= TIMESTAMP '2026-04-01 00:00:00'
  AND te.startTime < TIMESTAMP '2026-05-01 00:00:00'
GROUP BY u.id, u.username
ORDER BY totalSeconds DESC;
```

**Результат:**

| userId | username | totalSeconds |
|--------|----------|--------------|
| 42     | alice   | 23400        |
| 15     | bob     | 21600        |
| 23     | charlie | 9000         |
