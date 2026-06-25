package com.epesa.demo.controller;

import com.epesa.demo.dto.AsistenciaResponseDTO;
import com.epesa.demo.model.Asistencia;
import com.epesa.demo.model.Empleado;
import com.epesa.demo.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    // 1. Ver todas las asistencias consolidadas (Filtradas por el rol del JWT)
    @GetMapping("/asistencias")
    public ResponseEntity<List<AsistenciaResponseDTO>> obtenerDashboardGeneral(@AuthenticationPrincipal Empleado admin) {
        return ResponseEntity.ok(dashboardService.obtenerAsistencias(admin));
    }

    @GetMapping("/excepciones")
    public ResponseEntity<List<AsistenciaResponseDTO>> obtenerExcepciones(@AuthenticationPrincipal Empleado admin) {
        return ResponseEntity.ok(dashboardService.obtenerExcepciones(admin));
    }

    @PatchMapping("/excepciones/{id}/resolver")
    public ResponseEntity<AsistenciaResponseDTO> resolverExcepcion(
            @PathVariable UUID id,
            @RequestParam boolean aprobar,
            @RequestParam(required = false) String nota) {
        return ResponseEntity.ok(dashboardService.resolverExcepcion(id, aprobar, nota));
    }
}