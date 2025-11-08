package com.bookverse.userservice.service;

import com.bookverse.userservice.dto.auth.LoginRequestDto;
import com.bookverse.userservice.dto.auth.LoginResponseDto;
import com.bookverse.userservice.dto.auth.SignUpDto;
import com.bookverse.userservice.exception.serviceLevel.UserServiceException;
import com.bookverse.userservice.mapper.UserMapper;
import com.bookverse.userservice.model.Role;
import com.bookverse.userservice.model.User;
import com.bookverse.userservice.repository.UserRepository;
import com.bookverse.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private SignUpDto signUpDto;
    private LoginRequestDto loginRequestDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .emailId("test@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        signUpDto = new SignUpDto();
        signUpDto.setUsername("testuser");
        signUpDto.setEmailId("test@example.com");
        signUpDto.setPassword("password123");

        loginRequestDto = new LoginRequestDto();
        loginRequestDto.setUsername("testuser");
        loginRequestDto.setPassword("password123");
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        when(userMapper.toEntityFromSignUp(signUpDto)).thenReturn(testUser);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        SignUpDto result = authService.registerUser(signUpDto);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmailId());

        verify(userMapper, times(1)).toEntityFromSignUp(signUpDto);
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_Failure() {
        // Arrange
        when(userMapper.toEntityFromSignUp(signUpDto)).thenReturn(testUser);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(UserServiceException.class, () -> authService.registerUser(signUpDto));

        verify(userMapper, times(1)).toEntityFromSignUp(signUpDto);
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testLoginUser_Success() {
        // Arrange
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("encodedPassword")
                .roles("USER")
                .build();

        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token-123");

        // Act
        LoginResponseDto result = authService.loginUser(loginRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals("jwt-token-123", result.getToken());

        verify(customUserDetailsService, times(1)).loadUserByUsername("testuser");
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, times(1)).generateToken(userDetails);
    }

    @Test
    void testLoginUser_InvalidCredentials() {
        // Arrange
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("encodedPassword")
                .roles("USER")
                .build();

        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.loginUser(loginRequestDto));

        verify(customUserDetailsService, times(1)).loadUserByUsername("testuser");
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void testLogoutUser() {
        // Act
        String result = authService.logoutUser("jwt-token-123");

        // Assert
        assertEquals("Logout successful", result);
    }

    @Test
    void testGetCurrentUser_Authenticated() {
        // Arrange
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("encodedPassword")
                .roles("USER")
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.setContext(securityContext);

        // Act
        String result = authService.getCurrentUser();

        // Assert
        assertEquals("testuser", result);

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUser_NotAuthenticated() {
        // Arrange
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // Act
        String result = authService.getCurrentUser();

        // Assert
        assertNull(result);

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUser_NotUserDetails() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("someString"); // Not UserDetails
        SecurityContextHolder.setContext(securityContext);

        // Act
        String result = authService.getCurrentUser();

        // Assert
        assertNull(result);

        // Cleanup
        SecurityContextHolder.clearContext();
    }
}

