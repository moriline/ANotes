package com.taskmind.application.service;

import com.taskmind.domain.model.User;
import com.taskmind.domain.spi.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserService {
    @Inject UserRepository userRepo;

    public Optional<User> findById(UUID id) { return userRepo.findById(id); }
}
