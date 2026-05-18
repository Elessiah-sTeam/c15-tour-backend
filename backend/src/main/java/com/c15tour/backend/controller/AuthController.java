package com.c15tour.backend.controller;

import com.c15tour.backend.entity.User;
import com.c15tour.backend.repository.UserRepository;
import com.c15tour.backend.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final String adminUsername;
    private final String adminPassword;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            @Value("${app.security.admin-username}") String adminUsername,
            @Value("${app.security.admin-password}") String adminPassword,
            JwtUtils jwtUtils,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> dbUser = userRepository.findByUsername(request.username());
        if (dbUser.isPresent() && passwordEncoder.matches(request.password(), dbUser.get().getPasswordHash())) {
            String token = jwtUtils.generateToken(request.username(), "ADMIN");
            return ResponseEntity.ok(Map.of("token", token));
        }

        if (adminUsername.equals(request.username()) && adminPassword.equals(request.password())) {
            String token = jwtUtils.generateToken(adminUsername, "ADMIN");
            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        if (adminUsername.equals(request.username()) || userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already taken"));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        String token = jwtUtils.generateToken(request.username(), "ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", token));
    }

    public record LoginRequest(String username, String password) {}
    public record RegisterRequest(String username, String password) {}
}
