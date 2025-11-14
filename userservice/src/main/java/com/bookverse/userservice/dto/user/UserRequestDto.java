package com.bookverse.userservice.dto.user;

import com.bookverse.userservice.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Email ID cannot be blank")
    private String emailId;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    @NotNull(message = "Role level cannot be blank")
    private Role role;
}
