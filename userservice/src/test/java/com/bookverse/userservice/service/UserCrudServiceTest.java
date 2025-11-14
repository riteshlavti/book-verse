package com.bookverse.userservice.service;

import com.bookverse.userservice.dto.user.UserRequestDto;
import com.bookverse.userservice.dto.user.UserResponseDto;
import com.bookverse.userservice.exception.serviceLevel.UserServiceException;
import com.bookverse.userservice.mapper.UserMapper;
import com.bookverse.userservice.model.Role;
import com.bookverse.userservice.model.User;
import com.bookverse.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCrudServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCrudService userCrudService;

    private User testUser;
    private UserRequestDto userRequestDto;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .emailId("test@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        userRequestDto = UserRequestDto.builder()
                .username("testuser")
                .emailId("test@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        userResponseDto = UserResponseDto.builder()
                .username("testuser")
                .emailId("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userMapper.toEntity(userRequestDto)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserRequestDto result = userCrudService.createUser(userRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmailId());

        verify(passwordEncoder, times(1)).encode("password123");
        verify(userMapper, times(1)).toEntity(userRequestDto);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateUser_Failure() {
        // Arrange
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userMapper.toEntity(userRequestDto)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        UserServiceException exception = assertThrows(
                UserServiceException.class,
                () -> userCrudService.createUser(userRequestDto)
        );

        assertTrue(exception.getMessage().contains("Failed to add User"));
        assertTrue(exception.getMessage().contains("testuser"));

        verify(passwordEncoder, times(1)).encode("password123");
        verify(userMapper, times(1)).toEntity(userRequestDto);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testGetUser_Success() {
        // Arrange
        String username = "testuser";
        String requesterUsername = "testuser";

        when(userRepository.findByUsername(username)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(userResponseDto);

        // Act
        UserResponseDto result = userCrudService.getUser(username, requesterUsername);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmailId());

        verify(userRepository, times(1)).findByUsername(username);
        verify(userMapper, times(1)).toDto(testUser);
    }

    @Test
    void testGetUser_AccessDenied() {
        // Arrange
        String username = "testuser";
        String requesterUsername = "otheruser";

        // Act & Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> userCrudService.getUser(username, requesterUsername)
        );

        assertEquals("Access denied: You can only access your own user details.", exception.getMessage());

        verify(userRepository, never()).findByUsername(any());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void testGetUser_UserNotFound() {
        // Arrange
        String username = "testuser";
        String requesterUsername = "testuser";

        when(userRepository.findByUsername(username)).thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        UserServiceException exception = assertThrows(
                UserServiceException.class,
                () -> userCrudService.getUser(username, requesterUsername)
        );

        assertTrue(exception.getMessage().contains("User not found with username"));

        verify(userRepository, times(1)).findByUsername(username);
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void testGetUser_UserIsNull() {
        // Arrange
        String username = "testuser";
        String requesterUsername = "testuser";

        when(userRepository.findByUsername(username)).thenReturn(null);

        // Act & Assert
        UserServiceException exception = assertThrows(
                UserServiceException.class,
                () -> userCrudService.getUser(username, requesterUsername)
        );

        assertEquals("User with username testuser not found.", exception.getMessage());

        verify(userRepository, times(1)).findByUsername(username);
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void testUpdateUser_Success() {
        // Arrange
        String username = "testuser";
        String requesterUsername = "testuser";

        UserRequestDto updateDto = UserRequestDto.builder()
                .username("testuser")
                .emailId("updated@example.com")
                .password("newPassword123")
                .role(Role.ADMIN)
                .build();

        User updatedUser = User.builder()
                .id(1L)
                .username("testuser")
                .emailId("updated@example.com")
                .password("newEncodedPassword")
                .role(Role.ADMIN)
                .build();

        UserResponseDto updatedResponseDto = UserResponseDto.builder()
                .username("testuser")
                .emailId("updated@example.com")
                .password("newEncodedPassword")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findById(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(updatedResponseDto);

        // Act
        UserResponseDto result = userCrudService.updateUser(username, updateDto, requesterUsername);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("updated@example.com", result.getEmailId());
        assertEquals(Role.ADMIN, result.getRole());

        verify(userRepository, times(1)).findById(username);
        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    @Test
    void testUpdateUser_AccessDenied() {
        // Arrange
        String username = "testuser";
        String requesterUsername = "otheruser";

        UserRequestDto updateDto = UserRequestDto.builder()
                .username("testuser")
                .emailId("updated@example.com")
                .password("newPassword123")
                .role(Role.USER)
                .build();

        // Act & Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> userCrudService.updateUser(username, updateDto, requesterUsername)
        );

        assertEquals("Access denied: You can only update your own user details.", exception.getMessage());

        verify(userRepository, never()).findById(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void testUpdateUser_UserNotFound() {
        // Arrange
        String username = "nonexistent";
        String requesterUsername = "nonexistent";

        UserRequestDto updateDto = UserRequestDto.builder()
                .username("nonexistent")
                .emailId("updated@example.com")
                .password("newPassword123")
                .role(Role.USER)
                .build();

        when(userRepository.findById(username)).thenReturn(Optional.empty());

        // Act & Assert
        UserServiceException exception = assertThrows(
                UserServiceException.class,
                () -> userCrudService.updateUser(username, updateDto, requesterUsername)
        );

        assertTrue(exception.getMessage().contains("User not found with username"));

        verify(userRepository, times(1)).findById(username);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void testDeleteUser_Success() {
        // Arrange
        String username = "testuser";

        when(userRepository.findById(username)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        // Act
        String result = userCrudService.deleteUser(username);

        // Assert
        assertEquals("User deleted successfully", result);

        verify(userRepository, times(1)).findById(username);
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void testDeleteUser_UserNotFound() {
        // Arrange
        String username = "nonexistent";

        when(userRepository.findById(username)).thenReturn(Optional.empty());

        // Act & Assert
        UserServiceException exception = assertThrows(
                UserServiceException.class,
                () -> userCrudService.deleteUser(username)
        );

        assertTrue(exception.getMessage().contains("User not found with username"));

        verify(userRepository, times(1)).findById(username);
        verify(userRepository, never()).delete(any());
    }
}

