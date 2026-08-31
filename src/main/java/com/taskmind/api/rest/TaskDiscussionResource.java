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
import java.util.List;
import java.util.Optional;
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
        String author = resolveAuthor(req.author(), guard.callerId(sec));

        var block = new DiscussionBlock(
            blockId,
            req.parentId(),
            author,
            req.type() != null ? req.type() : BlockType.MESSAGE,
            req.content(),
            level,
            Instant.now()
        );

        taskService.addDiscussionBlock(task, block);
        return Response.status(Response.Status.CREATED).entity(DiscussionBlockResponse.from(block)).build();
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

    private String resolveAuthor(String requestedAuthor, Integer callerId) {
        if (requestedAuthor != null && !requestedAuthor.isBlank()) {
            return requestedAuthor;
        }
        return userService.findById(callerId).map(user -> user.username()).orElse(String.valueOf(callerId));
    }
}
