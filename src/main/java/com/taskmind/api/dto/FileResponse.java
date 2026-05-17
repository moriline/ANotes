package com.taskmind.api.dto;

import java.time.Instant;

public record FileResponse(
    Integer id,
    Integer taskId,
    Integer projectId,
    String fileName,
    String fileOriginalName,
    Long fileSize,
    String mimeType,
    String fileUrl,
    Integer uploadedByUserId,
    Instant createdAt
) {}
