package com.taskmind.infrastructure.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class JwtTokenProvider {
    @ConfigProperty(name = "mp.jwt.verify.issuer") String issuer;

    public String generateToken(UUID userId, String username, Set<String> roles) {
        return Jwt.issuer(issuer)
            .subject(userId.toString())
            .groups(roles)
            .claim("username", username)
            .sign();
    }
}
