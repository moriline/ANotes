package com.taskmind.application.service;

import com.taskmind.api.dto.FileResponse;
import com.taskmind.infrastructure.db.FileEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class FileService {

    private final Path root = Paths.get("uploads");

    @Inject ProjectAccessService projectAccess;

    public FileService() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @Transactional
    public FileResponse saveFile(Integer projectId, Integer taskId, Integer userId, String originalName, String mimeType, byte[] data) throws IOException {
        requireProjectAccess(userId, projectId);

        String fileName = UUID.randomUUID().toString() + "_" + originalName;
        Path filePath = root.resolve(fileName);
        Files.write(filePath, data);

        var entity = new FileEntity();
        entity.projectId = projectId;
        entity.taskId = taskId;
        entity.uploadedByUserId = userId;
        entity.fileOriginalName = originalName;
        entity.fileName = fileName;
        entity.fileSize = (long) data.length;
        entity.mimeType = mimeType;
        entity.fileUrl = "/api/files/download/" + fileName;
        entity.persist();

        return mapToResponse(entity);
    }

    public List<FileResponse> getFilesByTask(Integer taskId) {
        return FileEntity.<FileEntity>list("taskId", taskId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Путь к файлу для отдачи по имени. Раньше имя просто резолвилось в путь без
     * единой проверки — любой залогиненный пользователь скачивал любое вложение,
     * зная его имя. Теперь скачать может только участник проекта, к задаче
     * которого файл прикреплён.
     */
    public Path pathForDownload(String fileName, Integer userId) {
        FileEntity entity = FileEntity.find("fileName", fileName).firstResult();
        if (entity == null) throw new ResourceNotFoundException("Файл " + fileName + " не найден");
        requireProjectAccess(userId, entity.projectId);
        return root.resolve(entity.fileName);
    }

    @Transactional
    public void deleteFile(Integer fileId, Integer userId) throws IOException {
        FileEntity entity = FileEntity.findById(fileId);
        if (entity == null) throw new ResourceNotFoundException("Файл " + fileId + " не найден");
        requireProjectAccess(userId, entity.projectId);
        // Удалить вложение может только тот, кто его загрузил.
        if (!entity.uploadedByUserId.equals(userId)) throw new AccessDeniedException("Удалить можно только свой файл");

        Path filePath = root.resolve(entity.fileName);
        Files.deleteIfExists(filePath);
        entity.delete();
    }

    private void requireProjectAccess(Integer userId, Integer projectId) {
        if (!projectAccess.canAccess(userId, projectId)) {
            throw new AccessDeniedException("Нет доступа к проекту " + projectId);
        }
    }

    private FileResponse mapToResponse(FileEntity e) {
        return new FileResponse(
            e.id,
            e.taskId,
            e.projectId,
            e.fileName,
            e.fileOriginalName,
            e.fileSize,
            e.mimeType,
            e.fileUrl,
            e.uploadedByUserId,
            e.createdAt != null ? Instant.ofEpochMilli(e.createdAt) : null
        );
    }
}
