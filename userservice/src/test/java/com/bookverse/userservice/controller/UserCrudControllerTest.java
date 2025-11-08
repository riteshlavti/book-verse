package com.bookverse.userservice.controller;

import com.bookverse.userservice.config.SecurityConfig;
import com.bookverse.userservice.dto.user.UserRequestDto;
import com.bookverse.userservice.dto.user.UserResponseDto;
import com.bookverse.userservice.exception.serviceLevel.UserServiceException;
import com.bookverse.userservice.model.Role;
import com.bookverse.userservice.service.UserCrudService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(UserCrudController.class)
class UserCrudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCrudService userCrudService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_Success() throws Exception {
        // Arrange
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .username("newuser")
                .emailId("newuser@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        when(userCrudService.createUser(any(UserRequestDto.class))).thenReturn(userRequestDto);

        // Act & Assert
        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.emailId").value("newuser@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userCrudService, times(1)).createUser(any(UserRequestDto.class));
    }

    @Test
    @WithMockUser
    void testGetUser_Success() throws Exception {
        // Arrange
        String username = "testuser";
        String requesterUsername = "testuser";

        UserResponseDto userResponseDto = UserResponseDto.builder()
                .username(username)
                .emailId("testuser@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userCrudService.getUser(username, requesterUsername)).thenReturn(userResponseDto);

        // Act & Assert
        mockMvc.perform(get("/api/user/{username}", username)
                        .header("X-User-Id", requesterUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.emailId").value("testuser@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userCrudService, times(1)).getUser(username, requesterUsername);
    }

    @Test
    @WithMockUser
    void testGetUser_DifferentRequester() throws Exception {
        // Arrange
        String username = "testuser";
        String requesterUsername = "otheruser";

        when(userCrudService.getUser(username, requesterUsername))
                .thenThrow(new AccessDeniedException("Access denied: You can only access your own user details."));

        // Act & Assert
        mockMvc.perform(get("/api/user/{username}", username)
                        .header("X-User-Id", requesterUsername))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: You can only access your own user details."));

        verify(userCrudService, times(1)).getUser(username, requesterUsername);
    }

    @Test
    @WithMockUser
    void testUpdateUser_Success() throws Exception {
        // Arrange
        String username = "testuser";
        String requesterUsername = "testuser";

        UserRequestDto userRequestDto = UserRequestDto.builder()
                .username(username)
                .emailId("updated@example.com")
                .password("newpassword123")
                .role(Role.USER)
                .build();

        UserResponseDto userResponseDto = UserResponseDto.builder()
                .username(username)
                .emailId("updated@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userCrudService.updateUser(eq(username), any(UserRequestDto.class), eq(requesterUsername)))
                .thenReturn(userResponseDto);

        // Act & Assert
        mockMvc.perform(put("/api/user/{username}", username)
                        .header("X-User-Id", requesterUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.emailId").value("updated@example.com"));

        verify(userCrudService, times(1)).updateUser(eq(username), any(UserRequestDto.class), eq(requesterUsername));
    }

    @Test
    @WithMockUser
    void testUpdateUser_DifferentRequester() throws Exception {
        // Arrange
        String username = "testuser";
        String requesterUsername = "otheruser";

        UserRequestDto userRequestDto = UserRequestDto.builder()
                .username(username)
                .emailId("updated@example.com")
                .password("newpassword123")
                .role(Role.USER)
                .build();

        when(userCrudService.updateUser(eq(username), any(UserRequestDto.class), eq(requesterUsername)))
                .thenThrow(new RuntimeException("Access denied: You can only update your own user details."));

        // Act & Assert
        mockMvc.perform(put("/api/user/{username}", username)
                        .header("X-User-Id", requesterUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().is5xxServerError());

        verify(userCrudService, times(1)).updateUser(eq(username), any(UserRequestDto.class), eq(requesterUsername));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteUser_Success() throws Exception {
        // Arrange
        String username = "testuser";
        when(userCrudService.deleteUser(username)).thenReturn("User deleted successfully");

        // Act & Assert
        mockMvc.perform(delete("/api/user/{username}", username))
                .andExpect(status().isOk())
                .andExpect(content().string("User deleted successfully"));

        verify(userCrudService, times(1)).deleteUser(username);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteUser_UserNotFound() throws Exception {
        // Arrange
        String username = "nonexistent";
        when(userCrudService.deleteUser(username))
                .thenThrow(new RuntimeException("User not found with username: " + username));

        // Act & Assert
        mockMvc.perform(delete("/api/user/{username}", username))
                .andExpect(status().is5xxServerError());

        verify(userCrudService, times(1)).deleteUser(username);
    }
}

