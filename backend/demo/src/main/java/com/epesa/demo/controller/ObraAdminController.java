package com.epesa.demo.controller;

import com.epesa.demo.dto.ObraRequestDto;
import com.epesa.demo.dto.ObraResponseDto;
import com.epesa.demo.dto.ValidacionEspacialResult;
import com.epesa.demo.service.ObraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/obras")
@RequiredArgsConstructor
public class ObraAdminController {

    private final ObraService obraService;

    // Usado por React para crear el polígono
    @PostMapping
    public ResponseEntity<ObraResponseDto> crearObra(@Valid @RequestBody ObraRequestDto request) {
        return ResponseEntity.ok(obraService.crearObra(request));
    }

    // Endpoint de prueba para validar un punto GPS contra la obra
    @GetMapping("/{id}/validar-punto")
    public ResponseEntity<ValidacionEspacialResult> validarUbicacion(
            @PathVariable UUID id,
            @RequestParam Double lat,
            @RequestParam Double lng) {
        return ResponseEntity.ok(obraService.validarUbicacion(id, lat, lng));
    }
}