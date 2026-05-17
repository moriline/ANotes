package com.taskmind.infrastructure.ai;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class RagTaskIndexer {

    @Inject EmbeddingStore<TextSegment> embeddingStore;
    @Inject Instance<EmbeddingModel> embeddingModelInstance;

    public void indexTask(Integer taskId, Integer projectId, String content) {
        if (content == null || content.isBlank()) return;
        try {
            EmbeddingModel model = embeddingModelInstance.get();
            Embedding embedding = model.embed(content).content();
            Metadata meta = Metadata.metadata("taskId", taskId.toString())
                                    .metadata("projectId", projectId.toString());
            embeddingStore.add(embedding, TextSegment.from(content, meta));
        } catch (Exception e) {
            System.err.println("RAG indexing failed (Ollama may be offline): " + e.getMessage());
        }
    }

    public List<String> searchTaskIds(Integer projectId, String query, int topK) {
        try {
            EmbeddingModel model = embeddingModelInstance.get();
            Embedding queryEmbed = model.embed(query).content();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbed, topK, 0.5);
            return matches.stream()
                .filter(m -> m.embedded() != null && m.embedded().metadata() != null)
                .map(m -> m.embedded().metadata().getString("taskId"))
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}
