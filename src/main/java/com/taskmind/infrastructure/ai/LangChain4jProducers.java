// src/main/java/com/taskmind/infrastructure/ai/LangChain4jProducers.java
package com.taskmind.infrastructure.ai;

import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class LangChain4jProducers {


    @Produces
    @ApplicationScoped
    public EmbeddingStore<String> embeddingStore() {
        // In-memory хранилище для MVP (позже заменим на PostgreSQL/Neo4j)
        return new InMemoryEmbeddingStore<>();
    }
}