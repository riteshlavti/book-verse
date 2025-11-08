package com.bookverse.userservice.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignUpDto {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Email ID cannot be blank")
    private String emailId;

    private String password;
}
