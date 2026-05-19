package com.taskmind.api.rest;

import com.taskmind.application.service.PermissionService;
import com.taskmind.domain.model.Action;
import com.taskmind.infrastructure.db.UserEntity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/admin/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminUserResource {

    @Inject
    PermissionService permissionService;

    @GET
    public List<UserEntity> listAll(@Context SecurityContext ctx) {
        // Ожидаем, что в контексте безопасности есть информация о текущем пользователе.
        // Для примера просто возвращаем всех.
        return UserEntity.listAll();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteUser(@PathParam("id") Integer id) {
        UserEntity.deleteById(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/status")
    @Transactional
    public Response updateStatus(@PathParam("id") Integer id, Map<String, Boolean> body) {
        UserEntity user = UserEntity.findById(id);
        if (user == null) return Response.status(404).build();
        user.isActive = body.get("isActive");
        return Response.ok().build();
    }
}
