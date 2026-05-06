package com.varsha.taskmanager.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varsha.taskmanager.dto.UserResponse;
import com.varsha.taskmanager.model.User;
import com.varsha.taskmanager.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public UserResponse getCurrentUser(@AuthenticationPrincipal User user) {
        return UserResponse.fromEntity(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")    // Only ADMIN can list all users
    @Operation(summary = "List all users (ADMIN only)")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }
}
