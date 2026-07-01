package com.epesa.demo.controller;

import com.epesa.demo.dto.ObraDetailDto;
import com.epesa.demo.dto.ObraRequestDto;
import com.epesa.demo.dto.ObraResponseDto;
import com.epesa.demo.dto.ValidacionEspacialResult;
import com.epesa.demo.service.ObraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/obras")
@RequiredArgsConstructor
public class ObraAdminController {
    private final ObraService obraService;

    @GetMapping(path = {"", "/"})
    public ResponseEntity<List<ObraResponseDto>> listarObras() {
        return ResponseEntity.ok(obraService.listarObras());
    }

    @GetMapping("/{codigoSap}")
    public ResponseEntity<ObraDetailDto> obtenerObraDetalle(@PathVariable String codigoSap) {
        return ResponseEntity.ok(obraService.obtenerObraDetalle(codigoSap));
    }

    @PostMapping
    public ResponseEntity<ObraResponseDto> crearObra(@Valid @RequestBody ObraRequestDto request) {
        return ResponseEntity.ok(obraService.crearObra(request));
    }

    @PutMapping("/{codigoSap}")
    public ResponseEntity<ObraResponseDto> actualizarObra(@PathVariable String codigoSap,
                                                           @Valid @RequestBody ObraRequestDto request) {
        return ResponseEntity.ok(obraService.actualizarObra(codigoSap, request));
    }

    @DeleteMapping("/{codigoSap}")
    public ResponseEntity<Void> eliminarObra(@PathVariable String codigoSap) {
        obraService.eliminarObra(codigoSap);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{codigoSap}/validar-punto")
    public ResponseEntity<ValidacionEspacialResult> validarUbicacion(@PathVariable String codigoSap,
                                                                     @RequestParam Double lat,
                                                                     @RequestParam Double lng) {
        return ResponseEntity.ok(obraService.validarUbicacion(codigoSap, lat, lng));
    }
}
