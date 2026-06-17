package com.epesa.demo.controller;

import com.epesa.demo.model.Asistencia;
import com.epesa.demo.repository.AsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaRepository asistenciaRepository;

    @GetMapping
    public ResponseEntity<List<Asistencia>> obtenerAsistenciasValidadas() {
        return ResponseEntity.ok(asistenciaRepository.findAll());
    }
}