package com.epesa.demo.controller;

import com.epesa.demo.dto.HardwareTokenResponse;
import com.epesa.demo.dto.RegistroDispositivoDto;
import com.epesa.demo.model.Dispositivo;
import com.epesa.demo.service.SeguridadHardwareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

    private final SeguridadHardwareService hardwareService;

    // 1. Endpoint para registrar un nuevo equipo (Provisionamiento)
    @PostMapping("/enrolar")
    public ResponseEntity<Dispositivo> enrolar(@Valid @RequestBody RegistroDispositivoDto request) {
        return ResponseEntity.ok(hardwareService.enrolarDispositivo(request));
    }

    // 2. Endpoint de prueba para simular la validación Zero Trust
    @GetMapping("/validar")
    public ResponseEntity<HardwareTokenResponse> probarValidacion(
            @RequestParam String deviceId, 
            @RequestParam String firma) {
            
        boolean isValid = hardwareService.validarFirmaZeroTrust(deviceId, firma);
        String mensaje = isValid ? "Hardware Autorizado" : "Acceso Denegado: Hardware no confiable o inactivo";
        
        return ResponseEntity.ok(new HardwareTokenResponse(isValid, mensaje));
    }
}