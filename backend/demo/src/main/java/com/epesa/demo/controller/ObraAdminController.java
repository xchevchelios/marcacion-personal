package com.epesa.demo.controller;

import com.epesa.demo.dto.ObraRequestDto;
import com.epesa.demo.dto.ObraResponseDto;
import com.epesa.demo.dto.ObraDetailDto;
import com.epesa.demo.dto.ValidacionEspacialResult;
import com.epesa.demo.service.ObraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/obras")
@RequiredArgsConstructor
public class ObraAdminController {

    private final ObraService obraService;

    // GET /api/v1/admin/obras - Listar todas las obras
    @GetMapping(path = {"", "/"})
    public ResponseEntity<List<ObraResponseDto>> listarObras() {
        return ResponseEntity.ok(obraService.listarObras());
    }

    // GET /api/v1/admin/obras/{id} - Obtener detalles completos (con vértices)
    @GetMapping("/{id}")
    public ResponseEntity<ObraDetailDto> obtenerObraDetalle(@PathVariable UUID id) {
        return ResponseEntity.ok(obraService.obtenerObraDetalle(id));
    }

    @PostMapping
    public ResponseEntity<ObraResponseDto> crearObra(@Valid @RequestBody ObraRequestDto request) {
        return ResponseEntity.ok(obraService.crearObra(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObraResponseDto> actualizarObra(
            @PathVariable UUID id,
            @Valid @RequestBody ObraRequestDto request) {
        return ResponseEntity.ok(obraService.actualizarObra(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarObra(@PathVariable UUID id) {
        obraService.eliminarObra(id);
        return ResponseEntity.noContent().build();
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