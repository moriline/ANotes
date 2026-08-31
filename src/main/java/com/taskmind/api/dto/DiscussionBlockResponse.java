package com.taskmind.api.dto;

import com.taskmind.domain.model.BlockType;
import com.taskmind.domain.model.DiscussionBlock;

import java.time.Instant;
import java.util.UUID;

public record DiscussionBlockResponse(
    UUID id,
    UUID parentId,
    String author,
    BlockType type,
    String content,
    int level,
    Instant createdAt
) {
    public static DiscussionBlockResponse from(DiscussionBlock block) {
        return new DiscussionBlockResponse(
            block.id(),
            block.parentId(),
            block.author(),
            block.type(),
            block.content(),
            block.level(),
            block.createdAt()
        );
    }
}
