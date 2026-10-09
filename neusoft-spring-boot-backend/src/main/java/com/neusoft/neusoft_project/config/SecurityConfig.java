package com.neusoft.neusoft_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .csrf(csrf -> csrf.disable()) // Disable CSRF
                .authorizeHttpRequests(auth -> auth
                        // --- 1. PUBLIC ENDPOINTS (Login, Register, Chat) ---
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/chat/**").permitAll()

                        // --- 2. ADMIN DASHBOARD ---
                        // Currently allowing all for testing.
                        // When ready for security, change to: .hasRole("ADMIN")
                        .requestMatchers("/admin/**").permitAll()

                        // --- 3. DOCTOR DASHBOARD ---
                        // When ready, change to: .hasRole("DOCTOR")
                        .requestMatchers("/appointments/**").permitAll()
                        .requestMatchers("/reports/**").permitAll()

                        // --- 4. DEFAULT (Keep everything else open for development) ---
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow your Vue Frontend
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}