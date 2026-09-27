package com.rikkeibank.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.identityservice.dto.*;
import com.rikkeibank.identityservice.entity.Role;
import com.rikkeibank.identityservice.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login should return 200 with tokens")
    void testLoginEndpoint() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("john_doe")
                .password("secret123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token")
                .role("CUSTOMER")
                .username("john_doe")
                .userId(1L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test_access_token"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 201 on success")
    void testRegisterEndpoint() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("new_customer")
                .password("password123")
                .role(Role.CUSTOMER)
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("reg_access_token")
                .refreshToken("reg_refresh_token")
                .role("CUSTOMER")
                .username("new_customer")
                .userId(2L)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("reg_access_token"));
    }

    @Test
    @DisplayName("POST /api/auth/revoke/{userId} by ADMIN should return 200")
    void testRevokeUserByAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/revoke/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/revoke/{userId} without ADMIN role should return 403")
    void testRevokeUserForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/revoke/1")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }
}
