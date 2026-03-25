package com.c15tour.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(
                "changeMeInProductionThisKeyMustBeAtLeast256BitsLong!!",
                86400000L
        );
    }

    @Test
    void generateToken_ShouldReturnNonNullToken() {
        String token = jwtUtils.generateToken("admin", "ADMIN");
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void getUsername_ShouldReturnCorrectUsername() {
        String token = jwtUtils.generateToken("admin", "ADMIN");
        assertThat(jwtUtils.getUsername(token)).isEqualTo("admin");
    }

    @Test
    void getRole_ShouldReturnCorrectRole() {
        String token = jwtUtils.generateToken("admin", "ADMIN");
        assertThat(jwtUtils.getRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isValid_ShouldReturnTrueForValidToken() {
        String token = jwtUtils.generateToken("admin", "ADMIN");
        assertThat(jwtUtils.isValid(token)).isTrue();
    }

    @Test
    void isValid_ShouldReturnFalseForGarbageToken() {
        assertThat(jwtUtils.isValid("not.a.valid.token")).isFalse();
    }

    @Test
    void isValid_ShouldReturnFalseForExpiredToken() {
        JwtUtils shortLived = new JwtUtils(
                "changeMeInProductionThisKeyMustBeAtLeast256BitsLong!!",
                0L
        );
        String token = shortLived.generateToken("admin", "ADMIN");
        assertThat(shortLived.isValid(token)).isFalse();
    }
}
