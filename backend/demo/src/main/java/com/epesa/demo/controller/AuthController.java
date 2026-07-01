package com.epesa.demo.controller;

import com.epesa.demo.dto.AuthResponse;
import com.epesa.demo.dto.LoginRequest;
import com.epesa.demo.service.AuthService;
import com.epesa.demo.service.EmpleadoService;
import com.epesa.demo.dto.RegistroObreroRequest;
import com.epesa.demo.model.Empleado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmpleadoService empleadoService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    public ResponseEntity<Empleado> registro(@Valid @RequestBody RegistroObreroRequest request) {
        return ResponseEntity.ok(empleadoService.registrarObrero(request));
    }
}
