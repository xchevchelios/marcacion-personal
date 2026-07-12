package com.epesa.demo.service;

import com.epesa.demo.dto.AuthResponse;
import com.epesa.demo.dto.LoginRequest;
import com.epesa.demo.model.Empleado;
import com.epesa.demo.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final EmpleadoRepository empleadoRepository;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getCorreo(), request.getPassword()));
        Empleado empleado = empleadoRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new IllegalStateException("Empleado autenticado no encontrado"));
        return AuthResponse.builder()
                .token(jwtService.generateToken(empleado))
                .empleadoId(empleado.getDocumentoIdentidad())
                .nombreCompleto(empleado.getNombreCompleto())
                .rol(empleado.getRol())
                .tipoContrato(empleado.getTipoContrato())
                .estadoAprobacion(empleado.getEstadoAprobacion())
                .build();
    }
}
