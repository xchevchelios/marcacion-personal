package com.epesa.demo.controller;

import com.epesa.demo.dto.AsignacionResponseDto;
import com.epesa.demo.dto.AsignacionRequestDto;
import com.epesa.demo.service.AsignacionObraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionObraService asignacionObraService;

    @GetMapping(path = {"", "/"})
    public ResponseEntity<List<AsignacionResponseDto>> listarAsignaciones() {
        return ResponseEntity.ok(asignacionObraService.listarAsignaciones());
    }

    @PostMapping
    public ResponseEntity<AsignacionResponseDto> crearAsignacion(@Valid @RequestBody AsignacionRequestDto asignacion) {
        return ResponseEntity.ok(asignacionObraService.asignarEmpleado(asignacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAsignacion(@PathVariable UUID id) {
        asignacionObraService.eliminarAsignacion(id);
        return ResponseEntity.noContent().build();
    }
}
