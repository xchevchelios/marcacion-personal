package com.epesa.demo.controller;

import com.epesa.demo.dto.EmpleadoRequestDto;
import com.epesa.demo.dto.EmpleadoResponseDto;
import com.epesa.demo.service.EmpleadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/admin/empleados") @RequiredArgsConstructor
public class AdminEmpleadoController {
    private final EmpleadoService service;
    @PostMapping public ResponseEntity<EmpleadoResponseDto> crear(@Valid @RequestBody EmpleadoRequestDto request) { return ResponseEntity.ok(service.crear(request)); }
    @GetMapping public List<EmpleadoResponseDto> listar() { return service.listarTodos(); }
    @GetMapping("/{id}") public EmpleadoResponseDto obtener(@PathVariable UUID id) { return service.obtener(id); }
    @PutMapping("/{id}") public EmpleadoResponseDto actualizar(@PathVariable UUID id, @Valid @RequestBody EmpleadoRequestDto request) { return service.actualizar(id, request); }
    @PatchMapping("/{id}/estado") public EmpleadoResponseDto cambiarEstado(@PathVariable UUID id, @RequestParam boolean activo) { return service.cambiarEstado(id, activo); }
    @PatchMapping("/{id}/aprobacion") public EmpleadoResponseDto aprobar(@PathVariable UUID id, @RequestParam boolean aprobar) { return service.resolverAprobacion(id, aprobar); }
}
