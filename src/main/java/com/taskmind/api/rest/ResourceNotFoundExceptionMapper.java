package com.taskmind.api.rest;

import com.taskmind.application.service.ResourceNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Объекта с таким id нет — 404. */
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {
    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorResponse(exception.getMessage()))
            .build();
    }

    private record ErrorResponse(String message) {}
}
