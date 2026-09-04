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

/**
 * Редактирование и удаление комментария по его id.
 * Создание и список комментариев задачи — в {@link TaskCommentResource}.
 */
@Path("/api/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class CommentResource {

    @Inject CommentService commentService;
    @Inject AuthService authService;

    @PUT
    @Path("/{commentId}")
    public CommentResponse updateComment(@PathParam("commentId") Integer commentId, @Valid CommentRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        return commentService.updateComment(commentId, userId, req.content(), req.visibility());
    }

    @DELETE
    @Path("/{commentId}")
    public Response deleteComment(@PathParam("commentId") Integer commentId, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        commentService.deleteComment(commentId, userId);
        return Response.noContent().build();
    }
}
