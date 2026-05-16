// src/main/java/com/taskmind/api/rest/AiTestResource.java
package com.taskmind.api.rest;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/test/ai")
public class AiTestResource {

    @Inject
    EmbeddingModel embeddingModel;

    @GET
    @Path("/embed")
    @Produces(MediaType.TEXT_PLAIN)
    public String testEmbed() {
        Embedding embedding = embeddingModel.embed("Hello, TaskMind!").content();
        float[] vector = embedding.vector();
        return "Vector dimension: " + vector.length + "\nFirst 5 values: " +
                java.util.Arrays.toString(java.util.Arrays.copyOfRange(vector, 0, 5));
    }
}