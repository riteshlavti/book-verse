package com.bookverse.userservice.controller;

import com.bookverse.userservice.dto.auth.LoginRequestDto;
import com.bookverse.userservice.dto.auth.LoginResponseDto;
import com.bookverse.userservice.dto.auth.SignUpDto;
import com.bookverse.userservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public SignUpDto registerUser(@Valid @RequestBody SignUpDto signUpDto) {
        return authService.registerUser(signUpDto);
    }

    @PostMapping("/login")
    public LoginResponseDto loginUser(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        return authService.loginUser(loginRequestDto);
    }

    @PostMapping("/logout")
    public String logoutUser(String token) {
        return authService.logoutUser(token);
    }

    @GetMapping("/me")
    public String getCurrentUser() {
        return authService.getCurrentUser();
    }

}
