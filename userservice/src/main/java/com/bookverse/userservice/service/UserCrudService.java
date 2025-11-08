package com.bookverse.userservice.service;

import com.bookverse.userservice.dto.user.UserRequestDto;
import com.bookverse.userservice.dto.user.UserResponseDto;
import com.bookverse.userservice.exception.serviceLevel.UserServiceException;
import com.bookverse.userservice.mapper.UserMapper;
import com.bookverse.userservice.model.User;
import com.bookverse.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserCrudService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserRequestDto createUser( UserRequestDto userRequestDto) {
        try{
            userRequestDto.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
            User user = userMapper.toEntity(userRequestDto);
            userRepository.save(user);
        } catch (Exception e) {
            throw new UserServiceException("Failed to add User"+ userRequestDto.getUsername(), e);
        }
        return userRequestDto;
    }

    public UserResponseDto getUser(String username, String requesterUsername) {
        if (!username.equals(requesterUsername)) {
            throw new AccessDeniedException("Access denied: You can only access your own user details.");
        }

        User user;
        try{
            user = userRepository.findByUsername(username);
        } catch (Exception e) {
            throw new UserServiceException("User not found with username: " + username + ". "+e.getMessage(), e);
        }
        if (user == null) {
            throw new UserServiceException("User with username " + username + " not found.");
        }
        return userMapper.toDto(user);
    }

    public UserResponseDto updateUser(String username, UserRequestDto userRequestDto, String requesterUsername) {
        if(!username.equals(requesterUsername)) {
            throw new AccessDeniedException("Access denied: You can only update your own user details.");
        }

        User existingUser = userRepository.findById(username).orElseThrow(() ->
                new UserServiceException("User not found with username: " + username));
        existingUser.setEmailId(userRequestDto.getEmailId());
        existingUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        existingUser.setRole(userRequestDto.getRole());
        User updatedUser = userRepository.save(existingUser);

        return userMapper.toDto(updatedUser);
    }

    public String deleteUser(String username) {
        User existingUser = userRepository.findById(username).orElseThrow(() ->
                new UserServiceException("User not found with username: " + username));
        userRepository.delete(existingUser);
        return "User deleted successfully";
    }

}
