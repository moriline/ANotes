package com.taskmind.api.rest;

import com.taskmind.api.dto.UserProfile;
import com.taskmind.application.service.UserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class UserResource {
    @Inject UserService userService;

    @GET @Path("/me")
    public UserProfile getMe(@Context SecurityContext sec) {
        String userIdStr = sec.getUserPrincipal().getName();
        var user = userService.findById(Integer.parseInt(userIdStr)).orElseThrow();
        return new UserProfile(user.id(), user.username(), user.email(), user.displayName(), user.avatarUrl());
    }
}
