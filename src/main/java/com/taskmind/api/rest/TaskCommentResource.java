package com.taskmind.api.rest;

import com.taskmind.api.dto.CommentRequest;
import com.taskmind.api.dto.CommentResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.CommentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

/**
 * Комментарии в разрезе задачи. Раньше эти методы жили в {@link CommentResource}
 * без @Path на классе, из-за чего JAX-RS не считал его корневым ресурсом и весь
 * API комментариев отдавал 404. Полный путь обязан быть на классе: корень вида
 * {@code /api} тоже не работает — маршрутизатор Quarkus REST выбирает по префиксу
 * соседний ресурс {@code /api/tasks/{taskId}/time} и не откатывается назад.
 */
@Path("/api/tasks/{taskId}/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TaskCommentResource {

    @Inject CommentService commentService;
    @Inject AuthService authService;

    @POST
    public Response addComment(@PathParam("taskId") Integer taskId, @Valid CommentRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        var comment = commentService.addComment(taskId, userId, req.content(), req.visibility());
        return Response.status(Response.Status.CREATED).entity(comment).build();
    }

    @GET
    public List<CommentResponse> getComments(@PathParam("taskId") Integer taskId) {
        return commentService.getCommentsByTask(taskId);
    }
}
