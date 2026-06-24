package com.epesa.demo.controller;

import com.epesa.demo.model.AsignacionObra;
import com.epesa.demo.service.AsignacionObraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionObraService asignacionObraService;

    @PostMapping
    public ResponseEntity<AsignacionObra> crearAsignacion(@RequestBody AsignacionObra asignacion) {
        return ResponseEntity.ok(asignacionObraService.asignarEmpleado(asignacion));
    }
}