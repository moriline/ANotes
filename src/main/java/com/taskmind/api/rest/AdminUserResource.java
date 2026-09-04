package com.taskmind.api.rest;

import com.taskmind.api.dto.AdminUserResponse;
import com.taskmind.api.dto.AdminUserStatusRequest;
import com.taskmind.application.service.AuthService;
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

/**
 * Управление учётными записями. Ручки глобальные, поэтому и право на них
 * глобальное — {@code users.isAdmin}, а не роль внутри проекта.
 *
 * <p>Раньше у класса не было ни {@code @RolesAllowed}, ни проверки прав: список
 * пользователей вместе с bcrypt-хешами паролей отдавался кому угодно, включая
 * неаутентифицированных, а удалить чужую учётку мог любой.
 */
@Path("/api/admin/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminUserResource {

    @Inject AuthService authService;

    @GET
    public List<AdminUserResponse> listAll() {
        return UserEntity.<UserEntity>listAll().stream()
            .map(AdminUserResponse::from)
            .toList();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteUser(@PathParam("id") Integer id, @Context SecurityContext sec) {
        UserEntity user = requireUser(id);
        // Иначе администратор способен снести самого себя и оставить систему
        // вообще без администраторов.
        if (id.equals(callerId(sec))) {
            throw new BadRequestException("Нельзя удалить собственную учётную запись");
        }
        user.delete();
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/status")
    @Transactional
    public AdminUserResponse updateStatus(@PathParam("id") Integer id,
                                          AdminUserStatusRequest body,
                                          @Context SecurityContext sec) {
        UserEntity user = requireUser(id);

        Boolean isActive = body == null ? null : body.isActive();
        if (isActive == null) {
            throw new BadRequestException("Требуется поле isActive (true или false)");
        }
        if (!isActive && id.equals(callerId(sec))) {
            throw new BadRequestException("Нельзя заблокировать собственную учётную запись");
        }

        user.isActive = isActive;
        return AdminUserResponse.from(user);
    }

    private UserEntity requireUser(Integer id) {
        UserEntity user = UserEntity.findById(id);
        if (user == null) {
            throw new NotFoundException("Пользователь " + id + " не найден");
        }
        return user;
    }

    private Integer callerId(SecurityContext sec) {
        return authService.getUserIdFromToken(sec.getUserPrincipal().getName());
    }
}
