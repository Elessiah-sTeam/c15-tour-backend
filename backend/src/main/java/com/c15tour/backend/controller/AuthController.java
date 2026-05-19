package com.c15tour.backend.controller;

import com.c15tour.backend.entity.User;
import com.c15tour.backend.repository.UserRepository;
import com.c15tour.backend.security.JwtUtils;
import com.c15tour.backend.service.MailjetEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailjetEmailService mailjetEmailService;
    private final String frontendUrl;

    public AuthController(
            JwtUtils jwtUtils,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MailjetEmailService mailjetEmailService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailjetEmailService = mailjetEmailService;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> dbUser = userRepository.findByEmail(request.email());
        if (dbUser.isPresent() && passwordEncoder.matches(request.password(), dbUser.get().getPasswordHash())) {
            String token = jwtUtils.generateToken(dbUser.get().getEmail(), "ADMIN");
            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already taken"));
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getEmail(), "ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", token));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> dbUser = userRepository.findByEmail(email);

        if (dbUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        User user = dbUser.get();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Current password is incorrect"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        Optional<User> dbUser = userRepository.findByEmail(request.email());

        if (dbUser.isPresent()) {
            User user = dbUser.get();
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            String resetUrl = frontendUrl + "/reset-password?token=" + token;
            mailjetEmailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
        }

        return ResponseEntity.ok(Map.of("message", "If that email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Optional<User> dbUser = userRepository.findByResetToken(request.token());

        if (dbUser.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or expired token"));
        }

        User user = dbUser.get();
        if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or expired token"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    public record LoginRequest(String email, String password) {}
    public record RegisterRequest(String email, String password) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
    public record ForgotPasswordRequest(String email) {}
    public record ResetPasswordRequest(String token, String newPassword) {}
}
