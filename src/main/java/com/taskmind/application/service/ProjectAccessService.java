package com.taskmind.application.service;

import com.taskmind.domain.spi.MembershipRepository;
import com.taskmind.domain.spi.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;

/**
 * Одно место, отвечающее на вопрос «какие проекты вообще видит этот пользователь».
 *
 * <p>Видимость складывается из двух источников: проекты, которыми пользователь
 * владеет, и проекты, где он числится участником. Владельца приходится учитывать
 * отдельно, потому что проекты, созданные до появления автоматической записи в
 * {@code projectMembers}, не содержат владельца в списке участников.
 *
 * <p>Раньше эта логика лежала приватным методом в FindResource, и второй
 * потребитель (список проектов) её просто не имел — поэтому {@code GET /api/projects}
 * отдавал вообще все проекты всех пользователей.
 */
@ApplicationScoped
public class ProjectAccessService {

    @Inject ProjectRepository projectRepository;
    @Inject MembershipRepository membershipRepository;

    public Set<Integer> accessibleProjectIds(Integer userId) {
        Set<Integer> ids = new HashSet<>();
        projectRepository.findByOwner(userId).forEach(project -> ids.add(project.id()));
        membershipRepository.findByUser(userId).forEach(membership -> ids.add(membership.projectId()));
        return ids;
    }

    public boolean canAccess(Integer userId, Integer projectId) {
        return accessibleProjectIds(userId).contains(projectId);
    }
}
