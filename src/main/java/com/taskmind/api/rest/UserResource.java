package com.taskmind.api.rest;

import com.taskmind.api.dto.UserProfile;
import com.taskmind.api.dto.UserSummary;
import com.taskmind.application.service.UserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class UserResource {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    @Inject UserService userService;

    @GET @Path("/me")
    public UserProfile getMe(@Context SecurityContext sec) {
        String userIdStr = sec.getUserPrincipal().getName();
        var user = userService.findById(Integer.parseInt(userIdStr)).orElseThrow();
        return new UserProfile(user.id(), user.username(), user.email(), user.displayName(), user.avatarUrl());
    }

    /**
     * Справочник пользователей: кого можно назначить исполнителем или добавить в
     * проект. Раньше наружу был выведен только {@code /me}, поэтому выбрать
     * человека по имени было неоткуда.
     *
     * <p>{@code q} ищет подстроку в username, displayName и email без учёта
     * регистра; пустой запрос отдаёт начало справочника. Заблокированные
     * пользователи не показываются — назначать на них нечего.
     */
    @GET
    public List<UserSummary> search(@QueryParam("q") String query, @QueryParam("limit") Integer limit) {
        int effectiveLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (effectiveLimit <= 0) {
            throw new BadRequestException("limit должен быть положительным");
        }
        effectiveLimit = Math.min(effectiveLimit, MAX_LIMIT);

        return userService.search(query, effectiveLimit).stream()
            .map(UserSummary::from)
            .toList();
    }
}
