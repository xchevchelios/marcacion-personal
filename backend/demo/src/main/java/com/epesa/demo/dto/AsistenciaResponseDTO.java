package com.epesa.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AsistenciaResponseDTO {
    private UUID id;
    private UUID eventId;
    
    // Datos resueltos para la vista web
    private String empleadoNombre;
    private String empleadoDocumento;
    private String empleadoTipoContrato;
    private String obraNombre;
    
    // Datos técnicos
    private String deviceId;
    private LocalDateTime fechaHoraReal;
    
    // Flags de negocio
    private boolean requiereRevision;
    private String motivoRevision;
}