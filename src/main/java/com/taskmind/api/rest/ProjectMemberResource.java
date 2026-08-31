package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectMemberRequest;
import com.taskmind.api.dto.ProjectMemberResponse;
import com.taskmind.api.dto.ProjectMemberRoleRequest;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.MembershipService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.UserService;
import com.taskmind.domain.model.Project;
import com.taskmind.domain.model.ProjectMembership;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

/**
 * Состав участников проекта. Раньше единственным способом выдать человеку роль
 * в проекте была прямая вставка в {@code projectMembers}, поэтому совместную
 * работу через API показать было нельзя.
 *
 * <p>Менять состав может владелец проекта или участник с ролью Admin — без этой
 * проверки любой пользователь добавил бы себя в чужой проект и обошёл ограничения
 * списка проектов и поиска задач.
 */
@Path("/api/projects/{projectId}/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ProjectMemberResource {

    @Inject AuthService authService;
    @Inject MembershipService membershipService;
    @Inject ProjectAccessService projectAccessService;
    @Inject UserService userService;

    @GET
    public List<ProjectMemberResponse> list(@PathParam("projectId") Integer projectId, @Context SecurityContext sec) {
        Integer callerId = callerId(sec);
        requireProject(projectId);
        if (!projectAccessService.canAccess(callerId, projectId)) {
            throw new ForbiddenException("Нет доступа к проекту " + projectId);
        }

        return membershipService.listMembers(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @POST
    public Response add(@PathParam("projectId") Integer projectId,
                        @Valid ProjectMemberRequest req,
                        @Context SecurityContext sec) {
        Integer callerId = callerId(sec);
        requireProject(projectId);
        requireManager(callerId, projectId);
        requireExistingUser(req.userId());
        requireExistingRole(req.roleId());

        if (membershipService.find(projectId, req.userId()).isPresent()) {
            throw new ClientErrorException(
                "Пользователь " + req.userId() + " уже участник проекта " + projectId,
                Response.Status.CONFLICT);
        }

        var membership = membershipService.addMember(projectId, req.userId(), req.roleId());
        return Response.status(Response.Status.CREATED).entity(toResponse(membership)).build();
    }

    @PUT
    @Path("/{userId}")
    public ProjectMemberResponse changeRole(@PathParam("projectId") Integer projectId,
                                            @PathParam("userId") Integer userId,
                                            @Valid ProjectMemberRoleRequest req,
                                            @Context SecurityContext sec) {
        Integer callerId = callerId(sec);
        Project project = requireProject(projectId);
        requireManager(callerId, projectId);
        requireExistingRole(req.roleId());

        if (userId.equals(project.ownerUserId()) && req.roleId() != MembershipService.ROLE_ADMIN) {
            throw new BadRequestException("Владелец проекта должен остаться Admin");
        }

        ProjectMembership membership = requireMembership(projectId, userId);
        return toResponse(membershipService.changeRole(membership, req.roleId()));
    }

    @DELETE
    @Path("/{userId}")
    public Response remove(@PathParam("projectId") Integer projectId,
                           @PathParam("userId") Integer userId,
                           @Context SecurityContext sec) {
        Integer callerId = callerId(sec);
        Project project = requireProject(projectId);
        requireManager(callerId, projectId);

        if (userId.equals(project.ownerUserId())) {
            throw new BadRequestException("Нельзя удалить владельца проекта из участников");
        }

        membershipService.removeMember(requireMembership(projectId, userId));
        return Response.noContent().build();
    }

    private Integer callerId(SecurityContext sec) {
        return authService.getUserIdFromToken(sec.getUserPrincipal().getName());
    }

    private Project requireProject(Integer projectId) {
        return membershipService.findProject(projectId)
            .orElseThrow(() -> new NotFoundException("Проект " + projectId + " не найден"));
    }

    private void requireManager(Integer callerId, Integer projectId) {
        if (!membershipService.canManageMembers(callerId, projectId)) {
            throw new ForbiddenException("Управлять участниками проекта " + projectId
                + " может только его владелец или Admin");
        }
    }

    private void requireExistingUser(Integer userId) {
        if (userService.findById(userId).isEmpty()) {
            throw new BadRequestException("Пользователь " + userId + " не существует");
        }
    }

    private void requireExistingRole(Integer roleId) {
        if (!membershipService.roleExists(roleId)) {
            throw new BadRequestException("Роль " + roleId + " не существует");
        }
    }

    private ProjectMembership requireMembership(Integer projectId, Integer userId) {
        return membershipService.find(projectId, userId)
            .orElseThrow(() -> new NotFoundException(
                "Пользователь " + userId + " не участник проекта " + projectId));
    }

    private ProjectMemberResponse toResponse(ProjectMembership membership) {
        var user = userService.findById(membership.userId()).orElse(null);
        return ProjectMemberResponse.of(membership, user, membershipService.roleName(membership.roleId()));
    }
}
