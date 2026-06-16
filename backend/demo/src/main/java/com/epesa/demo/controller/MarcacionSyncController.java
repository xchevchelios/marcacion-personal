package com.epesa.demo.controller;

import com.epesa.demo.dto.MarcacionBatchRequest;
import com.epesa.demo.dto.SyncResponseDto;
import com.epesa.demo.service.MarcacionInboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class MarcacionSyncController {

    private final MarcacionInboxService inboxService;

    @PostMapping("/marcaciones")
    public ResponseEntity<SyncResponseDto> sincronizar(@Valid @RequestBody MarcacionBatchRequest request) {
        List<UUID> procesados = inboxService.procesarLote(request);
        return ResponseEntity.ok(new SyncResponseDto("SUCCESS", procesados));
    }
}