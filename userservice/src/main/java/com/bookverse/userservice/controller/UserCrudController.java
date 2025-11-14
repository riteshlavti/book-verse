package com.bookverse.userservice.controller;

import com.bookverse.userservice.dto.user.UserRequestDto;
import com.bookverse.userservice.dto.user.UserResponseDto;
import com.bookverse.userservice.service.UserCrudService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserCrudController {

    @Autowired
    UserCrudService userCrudService;

    @PostMapping("")
    public UserRequestDto createUser(@Valid @RequestBody UserRequestDto userRequestDto){
       return userCrudService.createUser(userRequestDto);
    }

    @GetMapping("/{username}")
    public UserResponseDto getUser(@PathVariable String username,
                                   @RequestHeader ("X-User-Id") String requesterUsername) {
        return userCrudService.getUser(username, requesterUsername);
    }

    @PutMapping("/{username}")
    public UserResponseDto updateUser(@PathVariable String username,
            @RequestHeader ("X-User-Id") String requesterUsername, @Valid @RequestBody UserRequestDto userRequestDto) {
        return userCrudService.updateUser(username, userRequestDto, requesterUsername);
    }

    @DeleteMapping("/{username}")
    public String deleteUser(@PathVariable String username) {
        return userCrudService.deleteUser(username);
    }
}
