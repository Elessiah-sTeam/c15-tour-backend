package com.c15tour.backend.controller;

import com.c15tour.backend.entity.User;
import com.c15tour.backend.repository.UserRepository;
import com.c15tour.backend.service.MailjetEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({
    com.c15tour.backend.security.SecurityConfig.class,
    com.c15tour.backend.security.JwtAuthenticationFilter.class,
    com.c15tour.backend.security.JwtUtils.class
})
@TestPropertySource(properties = {
    "app.security.jwt-secret=changeMeInProductionThisKeyMustBeAtLeast256BitsLong!!",
    "app.security.jwt-expiration-ms=86400000",
    "app.frontend.url=http://localhost"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MailjetEmailService mailjetEmailService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByResetToken(anyString())).thenReturn(Optional.empty());
        doNothing().when(mailjetEmailService).sendPasswordResetEmail(anyString(), anyString());
    }

    private User mockUser(String email, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(new BCryptPasswordEncoder().encode(rawPassword));
        return user;
    }

    @Test
    void login_WithValidEmailCredentials_ShouldReturn200WithToken() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser("test@test.com", "password")));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "test@test.com", "password", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_WithWrongPassword_ShouldReturn401() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser("test@test.com", "password")));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "test@test.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_WithUnknownEmail_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "unknown@test.com", "password", "password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_WithNewEmail_ShouldReturn201WithToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "newuser@test.com", "password", "secret"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_WithExistingEmail_ShouldReturn409() throws Exception {
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "taken@test.com", "password", "secret"))))
                .andExpect(status().isConflict());
    }

    @Test
    void register_WithBlankEmail_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "", "password", "secret"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_WithUnknownEmail_ShouldAlwaysReturn200() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "nobody@test.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_WithExistingEmail_ShouldSendResetEmail() throws Exception {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser("user@test.com", "pass")));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(mailjetEmailService).sendPasswordResetEmail(eq("user@test.com"), anyString());
    }

    @Test
    void resetPassword_WithValidToken_ShouldReturn200() throws Exception {
        User user = mockUser("u@test.com", "old");
        user.setResetToken("valid-token");
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", "valid-token", "newPassword", "newpass"))))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldReturn400() throws Exception {
        User user = mockUser("u@test.com", "old");
        user.setResetToken("expired-token");
        user.setResetTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", "expired-token", "newPassword", "newpass"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", "bad-token", "newPassword", "newpass"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_WithoutAuth_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "x", "newPassword", "y"))))
                .andExpect(status().isForbidden());
    }
}
