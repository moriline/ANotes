package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.User;
import com.taskmind.domain.spi.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class H2UserRepository implements UserRepository {
    @Override @Transactional
    public User save(User user) {
        var entity = UserEntity.fromDomain(user);
        entity.persist();
        return entity.toDomainModel();
    }
    @Override public Optional<User> findById(UUID id) {
        return UserEntity.<UserEntity>find("id", id).firstResultOptional().map(UserEntity::toDomainModel);
    }
    @Override public Optional<User> findByUsername(String username) {
        return UserEntity.<UserEntity>find("username", username).firstResultOptional().map(UserEntity::toDomainModel);
    }
    @Override public Optional<User> findByEmail(String email) {
        return UserEntity.<UserEntity>find("email", email).firstResultOptional().map(UserEntity::toDomainModel);
    }
}
