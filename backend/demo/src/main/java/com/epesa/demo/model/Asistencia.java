package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asistencias_consolidadas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asistencia {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true) private UUID eventId;
    @Column(nullable = false) private String deviceId;
    @Column(nullable = false) private String obraId;
    @Column(nullable = false) private LocalDateTime fechaHoraReal;
    @Column(nullable = false) private Double latitud;
    @Column(nullable = false) private Double longitud;
    @Column(nullable = false) private LocalDateTime fechaProcesamiento;
    @Column(nullable = false) private UUID empleadoId;
    @Column(nullable = false, length = 20) @Builder.Default private String tipoMarcacion = "ENTRADA";
    @Column(nullable = false) private boolean requiereRevision;
    @Column(length = 255) private String motivoRevision;
    @Column(nullable = false, length = 20) @Builder.Default private String estadoRevision = "SIN_REVISION";
    @Column(length = 1000) private String notaResolucion;
    private String resueltoPor;
    private LocalDateTime fechaResolucion;
    @Column(length = 40) @Builder.Default private String metodoMarcacion = "MOVIL";
    @Column(length = 120) private String terminalId;
    private Double confidenceScore;
    private Double precisionMetros;
    @Column(length = 80) private String biometricModel;
    @Column(length = 120) private String padronVersion;
    @Column(length = 255) private String evidenciaBucket;
    @Column(length = 500) private String evidenciaObjectName;
    @Column(length = 120) private String evidenciaContentType;
}
