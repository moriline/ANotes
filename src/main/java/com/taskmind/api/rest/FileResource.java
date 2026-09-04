package com.taskmind.api.rest;

import com.taskmind.api.dto.FileResponse;
import com.taskmind.application.service.FileService;
import com.taskmind.domain.model.Task;
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

/**
 * Вложения задачи. Доступ — через {@link TaskAccessGuard} и
 * {@code ProjectAccessService} в {@link FileService}: загружать, смотреть список,
 * скачивать и удалять файлы может только участник проекта, к задаче которого они
 * прикреплены (создатель проекта попадает в участники автоматически). Прежде ни
 * одна из четырёх ручек ничего не проверяла — файл скачивался по одному имени,
 * список отдавался по одному taskId, удаление смотрело только на автора.
 */
@Path("/api/files")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class FileResource {

    @Inject FileService fileService;
    @Inject TaskAccessGuard guard;

    @POST
    @Path("/projects/{projectId}/tasks/{taskId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public FileResponse upload(
            @PathParam("projectId") Integer projectId,
            @PathParam("taskId") Integer taskId,
            @RestForm("file") FileUpload file,
            @Context SecurityContext sec) throws IOException {

        Task task = guard.requireAccessibleTask(taskId, sec);
        if (!task.projectId().equals(projectId)) {
            throw new NotFoundException("Задача " + taskId + " не найдена в проекте " + projectId);
        }

        byte[] data = Files.readAllBytes(file.filePath());
        return fileService.saveFile(
            task.projectId(),
            taskId,
            guard.callerId(sec),
            file.fileName(),
            file.contentType(),
            data
        );
    }

    @GET
    @Path("/tasks/{taskId}")
    public List<FileResponse> listByTask(@PathParam("taskId") Integer taskId, @Context SecurityContext sec) {
        guard.requireAccessibleTask(taskId, sec);
        return fileService.getFilesByTask(taskId);
    }

    @GET
    @Path("/download/{fileName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("fileName") String fileName, @Context SecurityContext sec) {
        var path = fileService.pathForDownload(fileName, guard.callerId(sec));
        if (!Files.exists(path)) throw new NotFoundException();
        return Response.ok(path.toFile()).header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }

    @DELETE
    @Path("/{fileId}")
    public Response delete(@PathParam("fileId") Integer fileId, @Context SecurityContext sec) throws IOException {
        fileService.deleteFile(fileId, guard.callerId(sec));
        return Response.noContent().build();
    }
}
