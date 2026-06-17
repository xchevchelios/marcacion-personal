package com.epesa.demo.controller;

import com.epesa.demo.dto.ArchivoSubidoResponseDto;
import com.epesa.demo.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidencias")
@RequiredArgsConstructor
public class EvidenciaController {

    private final MinioStorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoSubidoResponseDto> subirArchivo(
            @RequestParam("eventId") UUID eventId,
            @RequestParam("file") MultipartFile file) {

        String nombreArchivoSaved = storageService.subirEvidencia(eventId, file);
        
        // Simulación de URL estática de acceso basada en el esquema de MinIO
        String urlAcceso = "/api/v1/evidencias/download/" + nombreArchivoSaved;

        return ResponseEntity.ok(new ArchivoSubidoResponseDto(
                eventId,
                nombreArchivoSaved,
                storageService.obtenerBucketName(),
                urlAcceso
        ));
    }
}