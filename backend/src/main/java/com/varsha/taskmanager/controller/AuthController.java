package com.varsha.taskmanager.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.varsha.taskmanager.dto.AuthResponse;
import com.varsha.taskmanager.dto.LoginRequest;
import com.varsha.taskmanager.dto.RegisterRequest;
import com.varsha.taskmanager.model.User;
import com.varsha.taskmanager.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, logout")
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details.")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
 
     @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange refresh token for new access token")
    public AuthResponse refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return authService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate all refresh tokens")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        // @AuthenticationPrincipal injects the currently authenticated user
        // from the SecurityContext (set by JwtFilter)
        authService.logout(user);
        return ResponseEntity.noContent().build();   // 204
    }


}
