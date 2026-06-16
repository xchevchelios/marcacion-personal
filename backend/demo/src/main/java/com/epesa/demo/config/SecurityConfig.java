package com.epesa.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. EL BYPASS: Le decimos a Spring Security que se apague completamente para estas rutas
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            "/h2-console", 
            "/h2-console/**"
        );
    }

    // 2. EL FILTRO ESTÁNDAR: Para el resto de tu aplicación (Tus APIs)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Apagado temporalmente para usar Postman sin problemas
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/sync/**").permitAll() // Tu API de marcación queda abierta
                .anyRequest().authenticated()
            );

        return http.build();
    }
}