package com.c15tour.backend.controller;

import com.c15tour.backend.entity.User;
import com.c15tour.backend.repository.UserRepository;
import com.c15tour.backend.security.JwtUtils;
import com.c15tour.backend.security.TokenHasher;
import com.c15tour.backend.service.MailjetEmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String MESSAGE_KEY = "message";
    private static final int MIN_PASSWORD_LENGTH = 8;

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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> dbUser = userRepository.findByEmail(request.email());
        if (dbUser.isPresent() && passwordEncoder.matches(request.password(), dbUser.get().getPasswordHash())) {
            String token = jwtUtils.generateToken(dbUser.get().getEmail(), "ADMIN");
            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
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
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
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
        user.setPasswordChangedAt(Instant.now().getEpochSecond());
        userRepository.save(user);

        String newToken = jwtUtils.generateToken(user.getEmail(), "ADMIN");
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Password updated successfully",
                "token", newToken));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<User> dbUser = userRepository.findByEmail(request.email());

        if (dbUser.isPresent()) {
            User user = dbUser.get();
            String rawToken = UUID.randomUUID().toString();
            user.setResetToken(TokenHasher.sha256(rawToken));
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/reset-password")
                    .queryParam("token", rawToken)
                    .toUriString();
            mailjetEmailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
        }

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "If that email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Optional<User> dbUser = userRepository.findByResetToken(TokenHasher.sha256(request.token()));

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
        user.setPasswordChangedAt(Instant.now().getEpochSecond());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Password reset successfully"));
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH) String password) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH) String newPassword) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH) String newPassword) {}
}
