package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.User;
import com.taskmind.domain.spi.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;

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
}
