package com.c15tour.backend.security;

import com.c15tour.backend.entity.User;
import com.c15tour.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtils.isValid(token)) {
                String username = jwtUtils.getUsername(token);
                String role = jwtUtils.getRole(token);

                if (isTokenStillValidForUser(username, token)) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenStillValidForUser(String email, String token) {
        Optional<User> dbUser = userRepository.findByEmail(email);
        if (dbUser.isEmpty()) {
            return true;
        }
        Long passwordChangedAt = dbUser.get().getPasswordChangedAt();
        if (passwordChangedAt == null) {
            return true;
        }
        long issuedAt = jwtUtils.getIssuedAtEpochSecond(token);
        return issuedAt >= passwordChangedAt;
    }
}
