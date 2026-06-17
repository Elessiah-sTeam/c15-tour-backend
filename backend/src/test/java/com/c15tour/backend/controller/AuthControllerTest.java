package com.c15tour.backend.controller;

import com.c15tour.backend.entity.User;
import com.c15tour.backend.repository.UserRepository;
import com.c15tour.backend.security.TokenHasher;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @MockitoBean
    private com.c15tour.backend.repository.TourRepository tourRepository;

    @MockitoBean
    private com.c15tour.backend.service.RoutingService routingService;

    @Autowired
    private com.c15tour.backend.security.JwtUtils jwtUtils;

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
                                Map.of("email", "test@test.com", "password", "wrongpwd"))))
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
                                Map.of("email", "newuser@test.com", "password", "secret12"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_WithExistingEmail_ShouldReturn409() throws Exception {
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "taken@test.com", "password", "secret12"))))
                .andExpect(status().isConflict());
    }

    @Test
    void register_WithBlankEmail_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "", "password", "secret12"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithInvalidEmailFormat_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "not-an-email", "password", "secret12"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithShortPassword_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "ok@test.com", "password", "short"))))
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
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser("user@test.com", "pass1234")));
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
        String rawToken = "valid-token";
        User user = mockUser("u@test.com", "oldpass12");
        user.setResetToken(TokenHasher.sha256(rawToken));
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetToken(TokenHasher.sha256(rawToken))).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", rawToken, "newPassword", "newpass12"))))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldReturn400() throws Exception {
        String rawToken = "expired-token";
        User user = mockUser("u@test.com", "oldpass12");
        user.setResetToken(TokenHasher.sha256(rawToken));
        user.setResetTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userRepository.findByResetToken(TokenHasher.sha256(rawToken))).thenReturn(Optional.of(user));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", rawToken, "newPassword", "newpass12"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", "bad-token", "newPassword", "newpass12"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_WithoutAuth_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "x", "newPassword", "newpass12"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_WithValidAuth_ShouldReturn200WithNewToken() throws Exception {
        User user = mockUser("auth@test.com", "currentPass");
        when(userRepository.findByEmail("auth@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String token = jwtUtils.generateToken("auth@test.com", "ADMIN");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "currentPass", "newPassword", "newPass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void changePassword_WithWrongCurrentPassword_ShouldReturn401() throws Exception {
        User user = mockUser("auth@test.com", "currentPass");
        when(userRepository.findByEmail("auth@test.com")).thenReturn(Optional.of(user));
        String token = jwtUtils.generateToken("auth@test.com", "ADMIN");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "wrongpwd", "newPassword", "newPass123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_WithBlankNewPassword_ShouldReturn400() throws Exception {
        User user = mockUser("auth@test.com", "currentPass");
        when(userRepository.findByEmail("auth@test.com")).thenReturn(Optional.of(user));
        String token = jwtUtils.generateToken("auth@test.com", "ADMIN");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "currentPass", "newPassword", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccount_WithoutAuth_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/auth/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("password", "currentPass"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAccount_WithValidAuth_ShouldReturn200AndDeleteUser() throws Exception {
        User user = mockUser("auth@test.com", "currentPass");
        when(userRepository.findByEmail("auth@test.com")).thenReturn(Optional.of(user));
        when(tourRepository.findByOwner(user)).thenReturn(java.util.List.of());
        String token = jwtUtils.generateToken("auth@test.com", "ADMIN");

        mockMvc.perform(delete("/auth/account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("password", "currentPass"))))
                .andExpect(status().isOk());

        verify(tourRepository).deleteAll(anyList());
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccount_WithWrongPassword_ShouldReturn401() throws Exception {
        User user = mockUser("auth@test.com", "currentPass");
        when(userRepository.findByEmail("auth@test.com")).thenReturn(Optional.of(user));
        String token = jwtUtils.generateToken("auth@test.com", "ADMIN");

        mockMvc.perform(delete("/auth/account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("password", "wrongpwd"))))
                .andExpect(status().isUnauthorized());

        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteAccount_WithBlankPassword_ShouldReturn400() throws Exception {
        User user = mockUser("auth@test.com", "currentPass");
        when(userRepository.findByEmail("auth@test.com")).thenReturn(Optional.of(user));
        String token = jwtUtils.generateToken("auth@test.com", "ADMIN");

        mockMvc.perform(delete("/auth/account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("password", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedRequest_AfterPasswordChange_ShouldRejectOldToken() throws Exception {
        String oldToken = jwtUtils.generateToken("rotated@test.com", "ADMIN");
        User user = mockUser("rotated@test.com", "currentPass");
        user.setPasswordChangedAt(Long.MAX_VALUE);
        when(userRepository.findByEmail("rotated@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "currentPass", "newPassword", "newPass123"))))
                .andExpect(status().isForbidden());
    }
}
