package com.taskmind.api.rest;

import com.taskmind.application.service.DuplicateResourceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Конфликт с уже существующей записью — 409. */
@Provider
public class DuplicateResourceExceptionMapper implements ExceptionMapper<DuplicateResourceException> {
    @Override
    public Response toResponse(DuplicateResourceException exception) {
        return Response.status(Response.Status.CONFLICT)
            .entity(new ErrorResponse(exception.getMessage()))
            .build();
    }

    private record ErrorResponse(String message) {}
}
