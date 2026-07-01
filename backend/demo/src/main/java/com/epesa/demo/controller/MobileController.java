package com.epesa.demo.controller;

import com.epesa.demo.dto.*;
import com.epesa.demo.model.Empleado;
import com.epesa.demo.service.MobileMarcacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
public class MobileController {
    private final MobileMarcacionService mobileService;

    @GetMapping("/mis-obras")
    public ResponseEntity<List<MobileObraDto>> misObras(@AuthenticationPrincipal Empleado empleado) {
        return ResponseEntity.ok(mobileService.misObras(empleado));
    }

    @PostMapping("/marcaciones")
    public ResponseEntity<MobileMarcacionResponse> marcar(@AuthenticationPrincipal Empleado empleado,
                                                          @Valid @RequestBody MobileMarcacionRequest request) {
        return ResponseEntity.ok(mobileService.marcar(empleado, request));
    }
}
