package com.taskmind.api.rest;

import com.taskmind.api.dto.PermissionCheckRequest;
import com.taskmind.application.service.PermissionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/api/permissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class PermissionResource {

    @Inject
    PermissionService permissionService;

    @POST
    @Path("/check")
    public Map<String, Boolean> checkPermission(PermissionCheckRequest request) {
        boolean allowed = permissionService.hasPermission(request.userId(), request.projectId(), request.action());
        return Map.of("allowed", allowed);
    }
}
