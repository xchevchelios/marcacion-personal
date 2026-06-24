package com.epesa.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
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
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // 1. Endpoints Públicos
                .requestMatchers("/api/v1/auth/**").permitAll() 
                
                // 2. Acceso Exclusivo de Recursos Humanos (Control Total)
                .requestMatchers("/api/v1/obras/**").hasRole("RRHH")
                .requestMatchers("/api/v1/admin/empleados/**").hasRole("RRHH")
                
                // 3. Acceso Compartido (RRHH, Jefes de Obra y Residentes)
                .requestMatchers("/api/v1/asignaciones/**").hasAnyRole("RRHH", "JEFE_OBRA", "RESIDENTE")
                
                // 4. Todo lo demás requiere autenticación general (como la sincronización del Inbox)
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}