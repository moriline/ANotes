package com.taskmind.api.rest;

import com.taskmind.api.dto.*;
import com.taskmind.application.service.AuthService;
import com.taskmind.domain.model.User;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject AuthService authService;

    @POST @Path("/register")
    public AuthResponse register(@Valid RegisterRequest req) {
        String token = authService.register(req.username(), req.email(), req.password());
        return AuthResponse.of(token, req.username(), new String[]{"USER"});
    }

    @POST @Path("/login")
    public AuthResponse login(@Valid LoginRequest req) {
        String token = authService.login(req.username(), req.password());
        return AuthResponse.of(token, req.username(), new String[]{"USER"});
    }
}
