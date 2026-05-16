// src/main/java/com/taskmind/api/rest/ConfigResource.java
package com.taskmind.api.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/config")
public class ConfigResource {

    @ConfigProperty(name = "quarkus.langchain4j.ollama.base-url")
    String ollamaUrl;

    @ConfigProperty(name = "taskmind.projects.root")
    String projectsRoot;

    @GET
    @Path("/ai")
    @Produces(MediaType.TEXT_PLAIN)
    public String aiConfig() {
        return "Ollama: " + ollamaUrl + "\nProjects: " + projectsRoot;
    }
}