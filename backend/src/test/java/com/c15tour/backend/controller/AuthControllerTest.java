package com.c15tour.backend.controller;

import com.c15tour.backend.service.RoutingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({
    com.c15tour.backend.security.SecurityConfig.class,
    com.c15tour.backend.security.JwtAuthenticationFilter.class,
    com.c15tour.backend.security.JwtUtils.class
})
@TestPropertySource(properties = {
    "app.security.admin-username=admin",
    "app.security.admin-password=admin",
    "app.security.jwt-secret=changeMeInProductionThisKeyMustBeAtLeast256BitsLong!!",
    "app.security.jwt-expiration-ms=86400000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Required because BackendApplication declares a CommandLineRunner @Bean that injects RoutingService,
    // and @WebMvcTest still processes @Bean methods on the @SpringBootApplication class.
    @MockitoBean
    private RoutingService routingService;

    @Test
    void login_WithValidCredentials_ShouldReturn200WithToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_WithWrongPassword_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_WithWrongUsername_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "hacker", "password", "admin"))))
                .andExpect(status().isUnauthorized());
    }
}
