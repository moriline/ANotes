package com.taskmind.infrastructure.security;

import org.mindrot.jbcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BCryptUtil {
    public String hash(String plain) { return BCrypt.hashpw(plain, BCrypt.gensalt(12)); }
    public boolean verify(String plain, String hash) { return BCrypt.checkpw(plain, hash); }
}
