package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asistencias_consolidadas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId; // Relación 1:1 lógica con el Inbox

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private UUID obraId;

    @Column(nullable = false)
    private LocalDateTime fechaHoraReal;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(nullable = false)
    private LocalDateTime fechaProcesamiento;

    @Column(nullable = false)
    private UUID empleadoId;

    @Column(nullable = false)
    private boolean requiereRevision; // Flag de Soft Fail

    @Column(length = 255)
    private String motivoRevision; // Explicación del flag para RRHH
}