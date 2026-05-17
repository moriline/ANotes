package com.taskmind.application.service;

import com.taskmind.domain.model.User;
import com.taskmind.domain.spi.UserRepository;
import com.taskmind.infrastructure.security.BCryptUtil;
import com.taskmind.infrastructure.security.JwtTokenProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthService {
    @Inject UserRepository userRepo;
    @Inject BCryptUtil bcrypt;
    @Inject JwtTokenProvider jwt;

    @Transactional
    public String register(String username, String email, String password) {
        if (userRepo.findByUsername(username).isPresent()) throw new IllegalArgumentException("Username taken");
        if (userRepo.findByEmail(email).isPresent()) throw new IllegalArgumentException("Email taken");
        var user = User.register(username, email, bcrypt.hash(password));
        userRepo.save(user);
        return login(username, password);
    }

    public String login(String username, String password) {
        var user = userRepo.findByUsername(username).orElseThrow(() -> new SecurityException("Invalid credentials"));
        if (!bcrypt.verify(password, user.password())) throw new SecurityException("Invalid credentials");
        // For simplicity, we keep a default role, but in schema-v4 roles are in projectRoles table
        return jwt.generateToken(user.id().toString(), user.username(), Set.of("USER"));
    }

    public Integer getUserIdFromToken(String subject) {
        return Integer.parseInt(subject);
    }
}
