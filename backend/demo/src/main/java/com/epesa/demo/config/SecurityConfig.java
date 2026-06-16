package com.epesa.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Deshabilitamos CSRF usando patrones de ruta
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
                .ignoringRequestMatchers("/api/v1/sync/**")
            )
            // 2. Permitimos iframes (esencial para que la interfaz de H2 funcione)
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            )
            // 3. Autorizamos las rutas
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/v1/sync/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}