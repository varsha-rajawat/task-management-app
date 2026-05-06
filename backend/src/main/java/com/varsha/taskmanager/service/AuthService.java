package com.varsha.taskmanager.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varsha.taskmanager.dto.AuthResponse;
import com.varsha.taskmanager.dto.LoginRequest;
import com.varsha.taskmanager.dto.RegisterRequest;
import com.varsha.taskmanager.exception.AppException;
import com.varsha.taskmanager.exception.DuplicateResourceException;
import com.varsha.taskmanager.exception.ResourceNotFoundException;
import com.varsha.taskmanager.model.RefreshToken;
import com.varsha.taskmanager.model.User;
import com.varsha.taskmanager.repository.RefreshTokenRepository;
import com.varsha.taskmanager.repository.UserRepository;
import com.varsha.taskmanager.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    //Register a new user
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with email " + request.getEmail() + " already exists.");
        }

        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword())) //Bcrypt hashing
            .build();

        user = userRepository.save(user);
        refreshTokenRepository.deleteByUser(user); // Invalidate any existing tokens (shouldn't be any for new user, but just in case)
        return buildAuthResponse(user);
    }

    //Login
    @Transactional
    public AuthResponse login(LoginRequest request) {
        //AuthenticationManager will call UserDetailsServiceImpl to load user and check password
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
            .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED));
        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken); 
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(User user) {
        refreshTokenRepository.deleteByUser(user);
    }


    // ── Private helpers ────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user); // Invalidate old tokens on new login
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiryMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    

    
}
