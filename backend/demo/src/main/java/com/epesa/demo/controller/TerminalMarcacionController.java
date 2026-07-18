package com.epesa.demo.controller;

import com.epesa.demo.dto.TerminalFacialMarcacionRequest;
import com.epesa.demo.dto.TerminalFacialMarcacionResponse;
import com.epesa.demo.service.TerminalFacialMarcacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/terminales")
@RequiredArgsConstructor
public class TerminalMarcacionController {

    private final TerminalFacialMarcacionService service;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/marcaciones/facial", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TerminalFacialMarcacionResponse> registrarJson(
            @RequestBody TerminalFacialMarcacionRequest request) {
        return ResponseEntity.ok(service.registrar(request, null));
    }

    @PostMapping(value = "/marcaciones/facial", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TerminalFacialMarcacionResponse> registrarMultipart(
            @RequestParam("metadata") String metadata,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        TerminalFacialMarcacionRequest request;
        try {
            request = objectMapper.readValue(metadata, TerminalFacialMarcacionRequest.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Metadata de marcacion facial invalida", exception);
        }
        return ResponseEntity.ok(service.registrar(request, file));
    }
}
