package com.taskmind.api.dto;

public record AuthResponse(String token, String username, String[] roles) {
    public static AuthResponse of(String token, String username, String[] roles) {
        return new AuthResponse(token, username, roles);
    }
}
