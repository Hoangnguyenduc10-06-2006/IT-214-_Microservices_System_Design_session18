package com.rikkeibank.identityservice.service;

import com.rikkeibank.identityservice.dto.*;
import com.rikkeibank.identityservice.entity.Role;
import com.rikkeibank.identityservice.entity.User;
import com.rikkeibank.identityservice.repository.UserRepository;
import com.rikkeibank.identityservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service handling registration, login, token refresh, and revocation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Register a new user. Only ADMIN should call this in production.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .customerId(request.getCustomerId())
                .staffId(request.getStaffId())
                .enabled(true)
                .build();

        user = userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getRole().name(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getUsername(), user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    /**
     * Authenticate user and return JWT tokens.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        // Check if user was revoked by ADMIN
        if (jwtUtil.isUserRevoked(user.getUsername())) {
            throw new RuntimeException("Access has been revoked. Contact administrator.");
        }

        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getRole().name(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getUsername(), user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    /**
     * Refresh access token using a valid refresh token.
     * Enables seamless long-lived sessions for CUSTOMER users.
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
            throw new RuntimeException("Token is not a refresh token");
        }

        String username = jwtUtil.getUsername(refreshToken);

        if (jwtUtil.isUserRevoked(username)) {
            throw new RuntimeException("Access has been revoked. Contact administrator.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getRole().name(), user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // reuse existing refresh token
                .role(user.getRole().name())
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    /**
     * ADMIN: Force logout a specific user by revoking all their tokens.
     */
    public void revokeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        jwtUtil.revokeAllTokensForUser(user.getUsername());
    }

    /**
     * Validate a token and return its claims info.
     */
    public boolean validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            return false;
        }
        String username = jwtUtil.getUsername(token);
        return !jwtUtil.isUserRevoked(username);
    }
}
