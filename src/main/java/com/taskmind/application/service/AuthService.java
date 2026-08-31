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
        if (userRepo.findByUsername(username).isPresent()) throw new DuplicateResourceException("Логин '" + username + "' уже занят");
        if (userRepo.findByEmail(email).isPresent()) throw new DuplicateResourceException("Email '" + email + "' уже занят");
        var user = User.register(username, email, bcrypt.hash(password));
        userRepo.save(user);
        return login(username, password);
    }

    public String login(String username, String password) {
        var user = userRepo.findByUsername(username).orElseThrow(() -> new SecurityException("Invalid credentials"));
        if (!bcrypt.verify(password, user.password())) throw new SecurityException("Invalid credentials");
        // USER есть у всех; ADMIN добавляется только глобальному администратору и
        // открывает /api/admin/users. Роли проекта живут отдельно, в projectRoles.
        Set<String> groups = user.isAdmin() ? Set.of("USER", "ADMIN") : Set.of("USER");
        return jwt.generateToken(user.id().toString(), user.username(), groups);
    }

    public Integer getUserIdFromToken(String subject) {
        return Integer.parseInt(subject);
    }
}
