package com.taskmind.api.rest;

import com.taskmind.api.dto.DiscussionBlockRequest;
import com.taskmind.api.dto.DiscussionBlockResponse;
import com.taskmind.application.service.TaskService;
import com.taskmind.application.service.UserService;
import com.taskmind.domain.model.BlockType;
import com.taskmind.domain.model.DiscussionBlock;
import com.taskmind.domain.model.Task;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Обсуждение задачи: дерево блоков, куда пишется ход рассуждений и принятые
 * решения. Модель и репозиторий существовали давно (JSON в {@code tasks.discussion},
 * поиск через {@code findByDiscussionContent}), но REST-ручки не было — писать и
 * читать обсуждение можно было только из кода.
 *
 * <p>{@code author} — свободная строка, а не userId, поэтому агенту не нужен
 * отдельный аккаунт: он подписывается своим именем под токеном того, от чьего
 * имени работает.
 */
@Path("/api/tasks/{taskId}/discussion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TaskDiscussionResource {

    @Inject TaskService taskService;
    @Inject UserService userService;
    @Inject TaskAccessGuard guard;

    @GET
    public List<DiscussionBlockResponse> list(@PathParam("taskId") Integer taskId, @Context SecurityContext sec) {
        Task task = guard.requireAccessibleTask(taskId, sec);
        return task.discussion().stream().map(DiscussionBlockResponse::from).toList();
    }

    @POST
    public Response add(@PathParam("taskId") Integer taskId,
                        @Valid DiscussionBlockRequest req,
                        @Context SecurityContext sec) {
        Task task = guard.requireAccessibleTask(taskId, sec);

        UUID blockId = resolveBlockId(task, req.id());
        int level = resolveLevel(task, req.parentId());
        String author = req.author() != null && !req.author().isBlank()
            ? req.author()
            : callerName(guard.callerId(sec));

        var block = new DiscussionBlock(
            blockId,
            req.parentId(),
            author,
            req.type() != null ? req.type() : BlockType.MESSAGE,
            req.content(),
            level,
            Instant.now()
        );

        taskService.addDiscussionBlock(task, block, guard.callerId(sec));
        return Response.status(Response.Status.CREATED).entity(DiscussionBlockResponse.from(block)).build();
    }

    /**
     * Полная замена дерева: правка формулировок, перевешивание веток, удаление
     * лишнего. Пустой список стирает обсуждение.
     *
     * <p>Уровни вложенности пересчитываются по {@code parentId}, а {@code createdAt}
     * у блоков, которые уже были в задаче, сохраняется — переписывание текста не
     * должно выглядеть как заново созданное обсуждение.
     */
    @PUT
    public List<DiscussionBlockResponse> replace(@PathParam("taskId") Integer taskId,
                                                 List<DiscussionBlockRequest> body,
                                                 @Context SecurityContext sec) {
        Task task = guard.requireAccessibleTask(taskId, sec);
        List<DiscussionBlockRequest> requested = body == null ? List.of() : body;

        List<UUID> ids = assignIds(requested);
        Map<UUID, UUID> parentById = mapParents(requested, ids);
        Map<UUID, Instant> createdBefore = existingCreationTimes(task);
        String fallbackAuthor = callerName(guard.callerId(sec));

        List<DiscussionBlock> blocks = new ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            DiscussionBlockRequest req = requested.get(i);
            UUID id = ids.get(i);
            blocks.add(new DiscussionBlock(
                id,
                req.parentId(),
                req.author() != null && !req.author().isBlank() ? req.author() : fallbackAuthor,
                req.type() != null ? req.type() : BlockType.MESSAGE,
                req.content(),
                levelOf(id, parentById),
                createdBefore.getOrDefault(id, Instant.now())
            ));
        }

        taskService.replaceDiscussion(task, blocks, guard.callerId(sec));
        return blocks.stream().map(DiscussionBlockResponse::from).toList();
    }

    /** Клиентский UUID сохраняется как есть; повтор — конфликт, а не молчаливый дубль. */
    private UUID resolveBlockId(Task task, UUID requestedId) {
        if (requestedId == null) {
            return UUID.randomUUID();
        }
        boolean alreadyUsed = task.discussion().stream()
            .anyMatch(existing -> requestedId.equals(existing.id()));
        if (alreadyUsed) {
            throw new ClientErrorException(
                "Блок " + requestedId + " уже есть в обсуждении задачи", Response.Status.CONFLICT);
        }
        return requestedId;
    }

    /** Уровень вложенности считает сервер: на единицу глубже родителя. */
    private int resolveLevel(Task task, UUID parentId) {
        if (parentId == null) {
            return 0;
        }
        Optional<DiscussionBlock> parent = task.discussion().stream()
            .filter(block -> parentId.equals(block.id()))
            .findFirst();
        return parent
            .orElseThrow(() -> new BadRequestException(
                "Родительский блок " + parentId + " не найден в обсуждении задачи"))
            .level() + 1;
    }

    private List<UUID> assignIds(List<DiscussionBlockRequest> requested) {
        List<UUID> ids = new ArrayList<>();
        Set<UUID> used = new HashSet<>();
        for (DiscussionBlockRequest req : requested) {
            if (req.content() == null || req.content().isBlank()) {
                throw new BadRequestException("У каждого блока обсуждения должен быть content");
            }
            UUID id = req.id() != null ? req.id() : UUID.randomUUID();
            if (!used.add(id)) {
                throw new BadRequestException("Блок " + id + " встречается в дереве дважды");
            }
            ids.add(id);
        }
        return ids;
    }

    private Map<UUID, UUID> mapParents(List<DiscussionBlockRequest> requested, List<UUID> ids) {
        Set<UUID> known = new HashSet<>(ids);
        Map<UUID, UUID> parentById = new HashMap<>();
        for (int i = 0; i < requested.size(); i++) {
            UUID parentId = requested.get(i).parentId();
            if (parentId != null && !known.contains(parentId)) {
                throw new BadRequestException(
                    "Родительский блок " + parentId + " отсутствует в присланном дереве");
            }
            parentById.put(ids.get(i), parentId);
        }
        return parentById;
    }

    private int levelOf(UUID id, Map<UUID, UUID> parentById) {
        Set<UUID> visited = new HashSet<>();
        visited.add(id);

        int level = 0;
        UUID current = parentById.get(id);
        while (current != null) {
            if (!visited.add(current)) {
                throw new BadRequestException("В дереве обсуждения есть цикл на блоке " + current);
            }
            level++;
            current = parentById.get(current);
        }
        return level;
    }

    private Map<UUID, Instant> existingCreationTimes(Task task) {
        Map<UUID, Instant> created = new HashMap<>();
        for (DiscussionBlock block : task.discussion()) {
            if (block.id() != null && block.createdAt() != null) {
                created.put(block.id(), block.createdAt());
            }
        }
        return created;
    }

    private String callerName(Integer callerId) {
        return userService.findById(callerId).map(user -> user.username()).orElse(String.valueOf(callerId));
    }
}
