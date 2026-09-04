package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectAppearanceResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Справочник для пикера оформления проекта: рекомендуемые цвета и имена иконок.
 * Отдельный ресурс, чтобы {@code /api/projects/appearance} не конкурировал с
 * {@code /api/projects/{id}}.
 */
@Path("/api/projects/appearance")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ProjectAppearanceResource {

    private static final List<String> COLORS = List.of(
        "#4A90D9", "#2ECC71", "#E74C3C", "#F39C12", "#9B59B6", "#1ABC9C",
        "#34495E", "#E91E63", "#7F8C8D", "#16A085", "#D35400", "#2C3E50"
    );

    private static final List<String> ICONS = List.of(
        "globe", "server", "bullhorn", "rocket", "code", "database",
        "mobile", "palette", "chart-line", "users", "bug", "book", "cog", "flask"
    );

    @GET
    public ProjectAppearanceResponse appearance() {
        return new ProjectAppearanceResponse(COLORS, ICONS);
    }
}
