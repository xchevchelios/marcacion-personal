package com.epesa.demo.controller;

import com.epesa.demo.dto.AuthResponse;
import com.epesa.demo.dto.LoginRequest;
import com.epesa.demo.model.Empleado;
import com.epesa.demo.repository.EmpleadoRepository;
import com.epesa.demo.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final EmpleadoRepository empleadoRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // 1. Verifica las credenciales. Si fallan, Spring lanza una excepción automáticamente (403/401)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword())
        );

        // 2. Si pasa, buscamos al usuario en la BD para generar su token y armar la respuesta
        Empleado empleado = empleadoRepository.findByCorreo(request.getCorreo())
                .orElseThrow();

        String jwtToken = jwtService.generateToken(empleado);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwtToken)
                .nombreCompleto(empleado.getNombreCompleto())
                .rol(empleado.getRol())
                .tipoContrato(empleado.getTipoContrato())
                .build());
    }
}