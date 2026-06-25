package com.epesa.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // 1. Públicas primero
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // 2. Protegidas por roles
                .requestMatchers("/api/v1/obras/**").hasRole("RRHH")
                .requestMatchers("/api/v1/admin/empleados/**").hasRole("RRHH")
                .requestMatchers("/api/v1/asignaciones/**").hasAnyRole("RRHH", "JEFE_OBRA", "RESIDENTE")
                .requestMatchers("/api/v1/admin/dashboard/**").hasAnyRole("RRHH", "JEFE_OBRA", "RESIDENTE")
                
                // 3. SOLO ESTO AL FINAL. NADA MÁS DEBE IR ABAJO.
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}