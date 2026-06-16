package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marcaciones_inbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcacionInbox {
    @Id
    @Column(updatable = false, nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private LocalDateTime timestampDispositivo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payloadRaw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEvento estado;

    @Column(nullable = false)
    private LocalDateTime fechaRecepcion;

    public enum EstadoEvento { PENDING, PROCESSED, ERROR }
}