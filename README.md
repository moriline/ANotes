# 🧠 TaskMind — AI-First Task Manager & Knowledge Base

> **GitOps Hybrid Architecture** | **Quarkus 3.14.4** | **Java 21+ Bytecode** | **LangChain4j** | **Ollama** | **No Docker Required**

```
🤖 AI-AGENT INSTRUCTIONS:
This README is optimized for AI parsing. Key sections are marked with [AI-HINT].
When modifying code, always:
1. Keep `domain.spi.*` interfaces stable
2. Use CDI `@ApplicationScoped` for services
3. Respect the File-System-First principle (`projects/` is the single source of truth)
4. Never hardcode API keys or change BOM versions without explicit approval
```

## 📋 Quick Start

```bash
# 1. Prerequisites
• Java 21+ (bytecode target 21, runs on JDK 22/23/24/25)
• Maven 3.8.6+
• Ollama running: `ollama serve`
• Models: `ollama pull llama3.1:8b nomic-embed-text`

# 2. Build & Run
mvn clean quarkus:dev

# 3. Access
🌐 App: http://localhost:8080
🔍 Health: http://localhost:8080/q/health
🧪 Dev UI: http://localhost:8080/q/dev
```

## 🎯 Project Vision

```
[AI-HINT: Core Philosophy]
TaskMind is NOT a traditional todo app. It's a cognitive bridge between:
• Human intention ↔ AI execution
• Tasks ↔ Knowledge ↔ Context
• File system (source of truth) ↔ Database (fast, derived index)

Key principle: Files in `projects/` are immutable source of truth.
Database indexes, vector stores, and caches are rebuildable derivatives.
```

### Core Capabilities

| Feature | Description | AI Integration Point |
|---------|-------------|---------------------|
| 📥 Ingest | Parse tasks/knowledge from files, chats, emails | `IngestPipeline` → `AiOrchestrator` |
| 🔗 Link | Auto-suggest relations between tasks & knowledge | Graph/vector analysis via `KnowledgeRepository` |
| 🔍 Query | Semantic search across active projects & archives | RAG pipeline with `EmbeddingModel` |
| ✍️ Generate | Create task drafts from knowledge fragments | `AiOrchestrator.suggestTaskFromChunk()` |
| 🗂️ Archive | Snapshot old projects for efficient, read-only RAG | `ArchiveSnapshotService` (vector-only storage) |
| 🔄 Sync | Git-based collaboration, conflict resolution | `GitWatcher` + event-driven updates |

## 🏗️ Architecture Overview

```
[AI-HINT: Clean Architecture Layers]
┌─────────────────────────────────────────┐
│              API LAYER                   │
│  • REST Resources (api.rest.*)          │
│  • DTOs & Validation (api.dto.*)        │
│  • OpenAPI / Health endpoints           │
└────────────────┬────────────────────────
                 │ HTTP/JSON
                 ▼
┌─────────────────────────────────────────┐
│         APPLICATION LAYER               │
│  • Use-case orchestrators               │
│  • Transaction boundaries               │
│  • Domain service coordination          │
│  • Package: com.taskmind.application.*  │
└────────────────┬────────────────────────
                 │ SPI Interfaces
                 ▼
┌─────────────────────────────────────────┐
│           DOMAIN LAYER                  │
│  • Immutable records (domain.model.*)   │
│  • Domain events (domain.event.*)       │
│  • SPI contracts (domain.spi.*) ←───────┐
│  • ZERO external/framework dependencies │
└────────────────┬────────────────────────│
                 │                        │
                 │ Implementations        │
                 ▼                        │
┌─────────────────────────────────────────┐│
│       INFRASTRUCTURE LAYER              ││
│  • Git: JGitWatcher → GitWatcher SPI    ││
│  • DB: Panache/H2 → *Repository SPI     ││
│  • AI: LangChain4j/Ollama → AiOrch SPI  ││
│  • Storage: FS, Archive, Snapshots      ││
│  • Package: com.taskmind.infrastructure.*│
└─────────────────────────────────────────┘
```

## 📁 Project Structure

```
taskmind/
├── pom.xml                          # Single module, managed deps
├── .mvn/
│   └── jvm.config                   # JVM flags (Byte Buddy experimental for Java 25)
├── src/
│   ├── main/
│   │   ├── java/com/taskmind/
│   │   │   ├── config/              # @ConfigProperties, lifecycle hooks
│   │   │   ├── domain/
│   │   │   │   ├── model/           # Immutable records: Task, Project, KnowledgeChunk
│   │   │   │   ├── event/           # Domain events
│   │   │   │   └── spi/             # 🔌 Pure interfaces (NO implementations!)
│   │   │   ├── application/
│   │   │   │   ├── service/         # Use-case orchestration
│   │   │   │   └── mapper/          # Entity ↔ Domain converters
│   │   │   ├── infrastructure/
│   │   │   │   ├── git/             # JGit implementations
│   │   │   │   ├── db/              # Panache/H2 repositories
│   │   │   │   ├── ai/              # LangChain4j/Ollama adapters
│   │   │   │   └── storage/         # FileSystem & Archive logic
│   │   │   └── api/
│   │   │       ├── rest/            # @Path Quarkus resources
│   │   │       └── dto/             # Request/Response records
│   │   └── resources/
│   │       ├── application.properties      # Base configuration
│   │       ├── application-dev.properties  # Dev overrides
│   │       ├── db/migration/               # Flyway SQL migrations
│   │       └── agents/                     # System prompts (AGENTS.md style)
│   └── test/
│       ├── java/com/taskmind/       # Tests mirror main structure
│       └── resources/
│           └── application.properties  # Test config
│
├── projects/                        # 📂 WORKSPACE (Git-tracked or local)
│   ├── project-one/
│   │   ├── raw/                     # Immutable sources: logs, imports
│   │   ├── wiki/                    # Curated knowledge: Markdown + frontmatter
│   │   ├── tasks/                   # Active tasks: .task.json or .md
│   │   ├── AGENTS.md                # Project-specific AI rules
│   │   └── project.json             # Metadata: tags, members, deadlines
│   └── archives/
│       └── project-legacy/          # Snapshot-indexed archives (read-only)
│
└── data/                            # Generated: H2 files, indexes, caches
```

## 🔌 SPI Interfaces Reference

```
[AI-HINT: Extension Contract]
New features must implement these interfaces in infrastructure.* 
Domain layer must NEVER import framework or infrastructure classes.
```

| Interface | Purpose | Key Methods | Implementation Example |
|-----------|---------|-------------|----------------------|
| `ProjectRepository` | Project CRUD & listing | `findById()`, `save()`, `findAllActive()` | `infrastructure.db.H2ProjectRepository` |
| `TaskRepository` | Task persistence | `findByProjectAndStatus()`, `save()`, `delete()` | `infrastructure.db.H2TaskRepository` |
| `KnowledgeRepository` | Vector store for RAG | `index()`, `findSimilar(Embedding, topK)` | `infrastructure.ai.InMemoryKnowledgeRepo` |
| `AiOrchestrator` | AI operations abstraction | `embed()`, `answerWithContext()`, `suggestTaskFromChunk()` | `infrastructure.ai.LangChain4jOrchestrator` |
| `GitWatcher` | File system monitoring | `startWatching()`, `getStatus()` | `infrastructure.git.JGitWatcher` |
| `ArchiveStorage` | Snapshot management | `createSnapshot()`, `loadSnapshot()` | `infrastructure.storage.ZipArchiveStorage` |

### Example: Adding a New Repository

```java
// 1. Domain SPI already exists: TaskRepository
// 2. Create implementation in infrastructure:

@ApplicationScoped
public class Neo4jTaskRepository implements TaskRepository {
    @Inject Neo4jClient neo4j;
    
    @Override @Transactional
    public Optional<Task> findById(TaskId id) {
        var result = neo4j.query("MATCH (t:Task {id: $id}) RETURN t", Map.of("id", id.value())).fetchOne();
        return Optional.ofNullable(result).map(this::toDomain);
    }
    
    private Task toDomain(NodeRecord r) { /* mapping logic */ }
}
// 3. CDI auto-wires this when @Inject TaskRepository is used
// 4. Application layer remains unchanged
```

## ⚙️ Configuration Guide

### `application.properties` (Base)

```properties
# ===== DATABASE =====
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:file:./data/taskmind;DB_CLOSE_ON_EXIT=FALSE
quarkus.hibernate-orm.database.generation=update
quarkus.flyway.baseline-on-migrate=true

# ===== AI / LANGCHAIN4J =====
quarkus.langchain4j.chat-model.provider=ollama
quarkus.langchain4j.ollama.base-url=http://localhost:11434
quarkus.langchain4j.ollama.chat-model.model-id=llama3.1:8b

quarkus.langchain4j.embedding-model.provider=ollama
quarkus.langchain4j.ollama.embedding-model.model-id=nomic-embed-text

# ===== FILE SYSTEM & ARCHIVE =====
taskmind.projects.root=${user.home}/taskmind/projects
taskmind.git.poll-interval=30s
taskmind.archive.strategy=snapshot
taskmind.archive.snapshot.cron=0 0 2 * * ?
taskmind.archive.snapshot.retention-days=90

# ===== LOGGING =====
quarkus.log.category."com.taskmind".level=DEBUG
quarkus.log.category."io.quarkiverse.langchain4j".level=INFO
```

## 🤖 AI Agent Interaction Patterns

```
[AI-HINT: How to interact with TaskMind programmatically]
```

### Pattern 1: RAG Query Flow

```java
@Inject AiOrchestrator ai;
@Inject KnowledgeRepository knowledgeRepo;

public String query(String question) {
    var embedding = ai.embed(question);
    var context = knowledgeRepo.findSimilar(embedding, 5)
        .stream().map(KnowledgeChunk::content).toList();
    return ai.answerWithContext(question, context);
}
```

### Pattern 2: Task Generation from Knowledge

```java
var draft = ai.suggestTaskFromChunk(selectedChunk, "developer");
if (draft.confidenceScore() > 0.8) {
    taskRepo.save(draft.toTask()); // Human-in-the-loop confirmation recommended
}
```

## 🧪 Testing Strategy

| Type | Tool | Scope |
|------|------|-------|
| **Unit** | JUnit 5 + Mockito | Pure domain logic, SPI mocks |
| **Integration** | `@QuarkusTest` + REST Assured | Full stack, H2 in-memory |
| **AI Mocking** | `@Alternative` + `@Priority` | Deterministic LLM responses in tests |

## 🆘 Troubleshooting

| Symptom | Likely Cause | Solution |
|---------|-------------|----------|
| `AmbiguousResolutionException: EmbeddingModel` | Two embedding providers active | Use ONLY Ollama OR local model, remove duplicate dependency |
| `Byte Buddy Java 25 not supported` | Missing experimental flag | Ensure `.mvn/jvm.config` contains `-Dnet.bytebuddy.experimental=true` |
| `Hibernate disabled: no entities found` | No `@Entity` classes yet | Normal warning; add first Panache entity or ignore |
| `Ollama connection refused` | Ollama not running or wrong URL | `ollama serve`; verify `base-url` in properties |
| `GitWatcher silent` | Not a Git repo or wrong path | Ensure `projects/*/` has `.git`; check `taskmind.projects.root` |

### Diagnostic Endpoints

```bash
curl http://localhost:8080/q/health
curl http://localhost:8080/q/metrics
curl http://localhost:8080/q/config  # Dev only
```

## 🔄 Deployment

### Development

```bash
mvn quarkus:dev  # Hot reload, Live Coding, Dev UI
```

### Production (JVM)

```bash
mvn clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

### Native (Optional, requires GraalVM)

```bash
mvn package -Pnative -Dquarkus.native.container-build=true
./target/taskmind-1.0.0-SNAPSHOT-runner
```

## 🤝 Contributing & AI Guidelines

```
[AI-HINT: Rules for AI-assisted contributions]
1. Keep SPIs stable. Deprecate before breaking.
2. Test AI outputs. Add assertions for orchestrator responses.
3. Store prompts in `agents/` with version comments.
4. Respect layer boundaries. Domain ↔ Infrastructure only via SPI.
5. Use virtual threads for I/O: `Executors.newVirtualThreadPerTaskExecutor()`
```

> 💡 **Final Note for AI Agents**:
> TaskMind is a **collaborative system**, not an autonomous executor.
> Augment human decisions with context, suggestions, and automation.
> Never auto-execute destructive operations without explicit human confirmation.
> When in doubt: log, ask, or propose — don't assume.

---

*Generated for AI-assisted development • Last updated: 2026-05-16 • Quarkus 3.14.4 • Java 21+ Compatible*
