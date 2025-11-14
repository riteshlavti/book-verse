package com.bookverse.userservice.mapper;

import com.bookverse.userservice.dto.auth.SignUpDto;
import com.bookverse.userservice.dto.user.UserRequestDto;
import com.bookverse.userservice.dto.user.UserResponseDto;
import com.bookverse.userservice.model.Role;
import com.bookverse.userservice.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequestDto userRequestDto){
        return User.builder()
                .username(userRequestDto.getUsername())
                .emailId(userRequestDto.getEmailId())
                .password(userRequestDto.getPassword())
                .role(userRequestDto.getRole())
                .build();
    }

    public UserResponseDto toDto(User user){
        return UserResponseDto.builder()
                .username(user.getUsername())
                .emailId(user.getEmailId())
                .password(user.getPassword())
                .role(user.getRole())
                .build();
    }

    public User toEntityFromSignUp(SignUpDto signUpDto){
        return User.builder()
                .username(signUpDto.getUsername())
                .emailId(signUpDto.getEmailId())
                .password(signUpDto.getPassword())
                .role(Role.USER) // Default role level for new sign-ups
                .build();
    }
}
