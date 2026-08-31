package com.taskmind.application.service;

import com.taskmind.domain.model.User;
import com.taskmind.domain.spi.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserService {
    @Inject UserRepository userRepo;

    public Optional<User> findById(Integer id) { return userRepo.findById(id); }

    public List<User> search(String query, int limit) { return userRepo.search(query, limit); }
}
