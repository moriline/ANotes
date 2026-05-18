package com.taskmind.domain.model;

import java.time.Instant;

public record TimeEntry(
    Integer id,
    Integer taskId,
    Integer userId,
    long seconds,
    String description,
    Instant startTime,
    Instant createdAt
) {}
