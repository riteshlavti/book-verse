package com.bookverse.userservice.service;

import com.bookverse.userservice.util.JwtUtil;
import com.bookverse.userservice.dto.auth.LoginRequestDto;
import com.bookverse.userservice.dto.auth.LoginResponseDto;
import com.bookverse.userservice.dto.auth.SignUpDto;
import com.bookverse.userservice.exception.serviceLevel.UserServiceException;
import com.bookverse.userservice.mapper.UserMapper;
import com.bookverse.userservice.model.User;
import com.bookverse.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    public SignUpDto registerUser(SignUpDto signUpDto) {

        User user = userMapper.toEntityFromSignUp(signUpDto);
        try{
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
        } catch (Exception e) {
            throw new UserServiceException("Signup Failed" ,e);
        }

        return signUpDto;
    }

    public LoginResponseDto loginUser(LoginRequestDto loginRequestDto) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequestDto.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDto.getUsername(), loginRequestDto.getPassword())
        );

        return new LoginResponseDto(jwtUtil.generateToken(userDetails));
    }

    public String logoutUser(String token) {
        // For stateless JWT, logout is handled client-side by deleting the token.
        return "Logout successful";
    }

    public String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
}
