package com.c15tour.backend.controller;

import com.c15tour.backend.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final String adminUsername;
    private final String adminPassword;
    private final JwtUtils jwtUtils;

    public AuthController(
            @Value("${app.security.admin-username}") String adminUsername,
            @Value("${app.security.admin-password}") String adminPassword,
            JwtUtils jwtUtils) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (adminUsername.equals(request.username()) && adminPassword.equals(request.password())) {
            String token = jwtUtils.generateToken(adminUsername, "ADMIN");
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
    }

    public record LoginRequest(String username, String password) {}
}