package com.rikkeibank.identityservice.service;

import com.rikkeibank.identityservice.dto.*;
import com.rikkeibank.identityservice.entity.Role;
import com.rikkeibank.identityservice.entity.User;
import com.rikkeibank.identityservice.repository.UserRepository;
import com.rikkeibank.identityservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded_pass")
                .role(Role.CUSTOMER)
                .customerId(100L)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Register: Should successfully register a new user")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("pass123")
                .role(Role.CUSTOMER)
                .customerId(100L)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtil.generateAccessToken(anyString(), anyString(), any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(anyString(), any())).thenReturn("refresh_token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("CUSTOMER", response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register: Should throw exception when username already exists")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .password("pass123")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login: Should successfully authenticate user with correct credentials")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
        when(jwtUtil.isUserRevoked("testuser")).thenReturn(false);
        when(jwtUtil.generateAccessToken("testuser", "CUSTOMER", 1L)).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken("testuser", 1L)).thenReturn("refresh_token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
    }

    @Test
    @DisplayName("Login: Should fail when password does not match")
    void testLoginInvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", "encoded_pass")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Invalid username or password"));
    }

    @Test
    @DisplayName("Refresh Token: Should issue new tokens when refresh token is valid")
    void testRefreshTokenSuccess() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid_refresh_token");

        when(jwtUtil.validateToken("valid_refresh_token")).thenReturn(true);
        when(jwtUtil.getTokenType("valid_refresh_token")).thenReturn("refresh");
        when(jwtUtil.getUsername("valid_refresh_token")).thenReturn("testuser");
        when(jwtUtil.isUserRevoked("testuser")).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(jwtUtil.generateAccessToken("testuser", "CUSTOMER", 1L)).thenReturn("new_access_token");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("valid_refresh_token", response.getRefreshToken());
    }

    @Test
    @DisplayName("Revoke User: Should call jwtUtil to revoke all tokens")
    void testRevokeUserSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        authService.revokeUser(1L);

        verify(jwtUtil, times(1)).revokeAllTokensForUser("testuser");
    }
}
