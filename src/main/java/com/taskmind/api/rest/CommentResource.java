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

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class CommentResource {

    @Inject CommentService commentService;
    @Inject AuthService authService;

    @POST
    @Path("/tasks/{taskId}/comments")
    public Response addComment(@PathParam("taskId") Integer taskId, @Valid CommentRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        var comment = commentService.addComment(taskId, userId, req.content());
        return Response.status(Response.Status.CREATED).entity(comment).build();
    }

    @GET
    @Path("/tasks/{taskId}/comments")
    public List<CommentResponse> getComments(@PathParam("taskId") Integer taskId) {
        return commentService.getCommentsByTask(taskId);
    }

    @PUT
    @Path("/comments/{commentId}")
    public CommentResponse updateComment(@PathParam("commentId") Integer commentId, @Valid CommentRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        return commentService.updateComment(commentId, userId, req.content());
    }

    @DELETE
    @Path("/comments/{commentId}")
    public Response deleteComment(@PathParam("commentId") Integer commentId, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        commentService.deleteComment(commentId, userId);
        return Response.noContent().build();
    }
}
