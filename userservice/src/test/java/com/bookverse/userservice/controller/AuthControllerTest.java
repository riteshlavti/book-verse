package com.bookverse.userservice.controller;

import com.bookverse.userservice.config.SecurityConfig;
import com.bookverse.userservice.dto.auth.LoginRequestDto;
import com.bookverse.userservice.dto.auth.LoginResponseDto;
import com.bookverse.userservice.dto.auth.SignUpDto;
import com.bookverse.userservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @MockitoBean
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void testRegisterUser_Success() throws Exception {
        // Arrange
        SignUpDto signUpDto = new SignUpDto();
        signUpDto.setUsername("testuser");
        signUpDto.setEmailId("test@example.com");
        signUpDto.setPassword("password123");

        when(authService.registerUser(any(SignUpDto.class))).thenReturn(signUpDto);

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.emailId").value("test@example.com"));

        verify(authService, times(1)).registerUser(any(SignUpDto.class));
    }

    @Test
    @WithMockUser
    void testLoginUser_Success() throws Exception {
        // Arrange
        LoginRequestDto loginRequestDto = new LoginRequestDto();
        loginRequestDto.setUsername("testuser");
        loginRequestDto.setPassword("password123");

        LoginResponseDto loginResponseDto = new LoginResponseDto("jwt-token-123");

        when(authService.loginUser(any(LoginRequestDto.class))).thenReturn(loginResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"));

        verify(authService, times(1)).loginUser(any(LoginRequestDto.class));
    }

    @Test
    @WithMockUser
    void testLogoutUser_Success() throws Exception {
        // Arrange
        String token = "jwt-token-123";
        when(authService.logoutUser(token)).thenReturn("Logout successful");

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string("Logout successful"));

        verify(authService, times(1)).logoutUser(token);
    }

    @Test
    @WithMockUser
    void testGetCurrentUser_Success() throws Exception {
        // Arrange
        String username = "testuser";
        when(authService.getCurrentUser()).thenReturn(username);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(content().string(username));

        verify(authService, times(1)).getCurrentUser();
    }

    @Test
    @WithMockUser
    void testGetCurrentUser_NoAuthentication() throws Exception {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());

        verify(authService, times(1)).getCurrentUser();
    }
}

