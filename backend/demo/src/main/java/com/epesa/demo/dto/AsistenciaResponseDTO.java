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
    private UUID empleadoId;
    private String empleadoNombre;
    private String empleadoDocumento;
    private String empleadoTipoContrato;
    private String obraId;
    private String obraNombre;
    
    // Datos técnicos
    private String deviceId;
    private LocalDateTime fechaHoraReal;
    private String tipoMarcacion;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSalida;
    private Double horasTrabajadas;
    
    // Flags de negocio
    private boolean requiereRevision;
    private String motivoRevision;
    private String estadoRevision;
    private String notaResolucion;
    private String resueltoPor;
    private LocalDateTime fechaResolucion;

    // Marcacion por terminal facial
    private String metodoMarcacion;
    private String terminalId;
    private Double confidenceScore;
    private Double precisionMetros;
    private String biometricModel;
    private String padronVersion;
    private String evidenciaBucket;
    private String evidenciaObjectName;
    private String evidenciaContentType;
}
