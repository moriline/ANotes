package com.taskmind.infrastructure.ai;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class NoOpEmbeddingModel implements EmbeddingModel {

    private static final Embedding ZERO = new Embedding(new float[768]);

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return Response.from(ZERO, new TokenUsage(0, 0));
    }

    public Response<Embedding> embed(String text) {
        return Response.from(ZERO, new TokenUsage(0, 0));
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        var embeddings = textSegments.stream()
            .map(ts -> ZERO)
            .toList();
        return Response.from(embeddings, new TokenUsage(0, 0));
    }
}
