package com.varsha.taskmanager.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.varsha.taskmanager.security.JwtFilter;
import com.varsha.taskmanager.security.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;

/**
 * Central security configuration.
 *
 * Key decisions:
 *   - STATELESS session: no HttpSession, no cookies. Every request must carry a JWT.
 *   - CSRF disabled: safe for stateless APIs (CSRF attacks rely on session cookies).
 *   - @EnableMethodSecurity: enables @PreAuthorize on controller methods.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF ──────────────────────────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS ──────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Route-level authorization ──────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public routes — no token required
                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // ── Session management ─────────────────────────────
            // STATELESS = Spring never creates an HttpSession.
            // Each request is independently authenticated via JWT.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── Auth provider ──────────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── JWT filter ─────────────────────────────────────
            // Insert our JWT filter BEFORE the default username/password filter.
            // Spring Security runs filters in order — ours runs first and sets
            // the SecurityContext, so the default filter finds auth already set.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCryptPasswordEncoder with strength 10 (default).
     * BCrypt automatically generates and stores a salt — you never manage it.
     * Strength 10 means 2^10 = 1024 hashing rounds. Strong enough, not too slow.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager is needed by AuthService to authenticate login requests
     * (validate email + password). Spring Boot 3 doesn't expose it as a bean by default,
     * so we explicitly declare it here.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS config: allow the React dev server (localhost:3000) to call the API.
     * In production, replace with your actual frontend domain.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
