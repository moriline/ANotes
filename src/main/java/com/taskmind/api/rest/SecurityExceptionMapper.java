package com.taskmind.api.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Неверные учётные данные при входе — 401.
 *
 * <p>Только этот случай: «залогинен, но не имеет права» отдаёт 403 через
 * {@link AccessDeniedExceptionMapper}.
 */
@Provider
public class SecurityExceptionMapper implements ExceptionMapper<SecurityException> {
    @Override
    public Response toResponse(SecurityException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ErrorResponse(exception.getMessage()))
            .build();
    }

    private record ErrorResponse(String message) {}
}
