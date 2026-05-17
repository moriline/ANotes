package com.taskmind.api.rest;

import com.taskmind.api.dto.FileResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.FileService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Path("/api/files")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class FileResource {

    @Inject FileService fileService;
    @Inject AuthService authService;

    @POST
    @Path("/projects/{projectId}/tasks/{taskId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public FileResponse upload(
            @PathParam("projectId") Integer projectId,
            @PathParam("taskId") Integer taskId,
            @RestForm("file") FileUpload file,
            @Context SecurityContext sec) throws IOException {
        
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        byte[] data = Files.readAllBytes(file.filePath());
        
        return fileService.saveFile(
            projectId, 
            taskId, 
            userId, 
            file.fileName(), 
            file.contentType(), 
            data
        );
    }

    @GET
    @Path("/tasks/{taskId}")
    public List<FileResponse> listByTask(@PathParam("taskId") Integer taskId) {
        return fileService.getFilesByTask(taskId);
    }

    @GET
    @Path("/download/{fileName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("fileName") String fileName) {
        var path = fileService.getFilePath(fileName);
        if (!Files.exists(path)) throw new NotFoundException();
        return Response.ok(path.toFile()).header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }

    @DELETE
    @Path("/{fileId}")
    public Response delete(@PathParam("fileId") Integer fileId, @Context SecurityContext sec) throws IOException {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        fileService.deleteFile(fileId, userId);
        return Response.noContent().build();
    }
}
