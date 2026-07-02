package com.epesa.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

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
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/auditoria/**").hasRole("SOPORTE")
                .requestMatchers("/api/v1/dispositivos/enrolar").hasAnyRole("SOPORTE", "RRHH", "ADMIN")
                .requestMatchers("/api/v1/admin/empleados", "/api/v1/admin/empleados/", "/api/v1/admin/empleados/**").hasAnyRole("SOPORTE", "RRHH", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/asignaciones", "/api/v1/admin/asignaciones/", "/api/v1/admin/asignaciones/**").hasAnyRole("SOPORTE", "RRHH", "ADMIN", "JEFE_OBRA", "RESIDENTE")
                .requestMatchers("/api/v1/admin/asignaciones", "/api/v1/admin/asignaciones/", "/api/v1/admin/asignaciones/**").hasAnyRole("SOPORTE", "RRHH", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/dashboard/excepciones/**").hasAnyRole("SOPORTE", "RRHH", "ADMIN")
                .requestMatchers("/api/v1/admin/dashboard/**").hasAnyRole("SOPORTE", "RRHH", "ADMIN", "JEFE_OBRA", "RESIDENTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/obras", "/api/v1/admin/obras/", "/api/v1/admin/obras/**").hasAnyRole("SOPORTE", "RRHH", "ADMIN", "JEFE_OBRA", "RESIDENTE")
                .requestMatchers("/api/v1/admin/obras", "/api/v1/admin/obras/", "/api/v1/admin/obras/**").hasAnyRole("SOPORTE", "RRHH", "ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Autenticación requerida"))
                .accessDeniedHandler((request, response, exception) ->
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Permisos insuficientes"))
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
