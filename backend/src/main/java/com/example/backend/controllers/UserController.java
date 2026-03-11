package com.example.backend.controllers;

import com.example.backend.models.UserDto;
import com.example.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(Authentication currentUser) {
        return ResponseEntity.ok(userService.getAllUsersExceptSelf(currentUser));
    }
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyProfile(Authentication currentUser) {
        return ResponseEntity.ok(userService.getMyProfile(currentUser));
    }
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(
            @RequestParam String keyword,
            Authentication currentUser) {
        return ResponseEntity.ok(userService.searchUsers(keyword, currentUser));
    }
}