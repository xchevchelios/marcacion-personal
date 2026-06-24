package com.epesa.demo.controller;

import com.epesa.demo.model.Empleado;
import com.epesa.demo.service.EmpleadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/empleados")
@RequiredArgsConstructor
public class AdminEmpleadoController {

    private final EmpleadoService empleadoService;

    @PostMapping
    public ResponseEntity<Empleado> crearEmpleado(@RequestBody Empleado empleado) {
        return ResponseEntity.ok(empleadoService.registrarEmpleado(empleado));
    }

    @GetMapping
    public ResponseEntity<List<Empleado>> listarEmpleados() {
        return ResponseEntity.ok(empleadoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Empleado> obtenerEmpleado(@PathVariable UUID id) {
        return ResponseEntity.ok(empleadoService.obtenerPorId(id));
    }
}