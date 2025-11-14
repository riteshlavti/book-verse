package com.bookverse.userservice.dto.user;

import com.bookverse.userservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private String username;
    private String emailId;
    private String password;
    private Role role;
}
