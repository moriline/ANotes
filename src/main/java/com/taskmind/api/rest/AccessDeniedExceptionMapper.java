package com.taskmind.api.rest;

import com.taskmind.application.service.AccessDeniedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Отказ по правам — 403. Не 401: вызывающий залогинен, ему просто нельзя. */
@Provider
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {
    @Override
    public Response toResponse(AccessDeniedException exception) {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(new ErrorResponse(exception.getMessage()))
            .build();
    }

    private record ErrorResponse(String message) {}
}
