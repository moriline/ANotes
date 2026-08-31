package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.User;
import com.taskmind.domain.spi.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2UserRepository implements UserRepository {
    @Override @Transactional
    public User save(User user) {
        var entity = UserEntity.fromDomain(user);
        if (entity.id == null) {
            entity.persist();
        } else {
            entity = entity.getEntityManager().merge(entity);
        }
        return entity.toDomainModel();
    }
    @Override public Optional<User> findById(Integer id) {
        return UserEntity.<UserEntity>findByIdOptional(id).map(UserEntity::toDomainModel);
    }
    @Override public Optional<User> findByUsername(String username) {
        return UserEntity.<UserEntity>find("username", username).firstResultOptional().map(UserEntity::toDomainModel);
    }
    @Override public Optional<User> findByEmail(String email) {
        return UserEntity.<UserEntity>find("email", email).firstResultOptional().map(UserEntity::toDomainModel);
    }

    @Override public List<User> search(String query, int limit) {
        String pattern = "%" + (query == null ? "" : query.toLowerCase()) + "%";
        return UserEntity.<UserEntity>find(
                "isActive = true and (lower(username) like ?1 or lower(displayName) like ?1 or lower(email) like ?1)"
                    + " order by username",
                pattern)
            .page(0, limit)
            .list().stream()
            .map(UserEntity::toDomainModel)
            .collect(Collectors.toList());
    }
}
