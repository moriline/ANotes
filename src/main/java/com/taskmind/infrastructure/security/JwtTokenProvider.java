package com.taskmind.infrastructure.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Set;

@ApplicationScoped
public class JwtTokenProvider {
    @ConfigProperty(name = "mp.jwt.verify.issuer") String issuer;
    @ConfigProperty(name = "mp.jwt.verify.audiences") String audience;

    public String generateToken(String userId, String username, Set<String> roles) {
        return Jwt.issuer(issuer)
            .subject(userId)
            .audience(audience)
            .groups(roles)
            .claim("username", username)
            .sign();
    }
}
