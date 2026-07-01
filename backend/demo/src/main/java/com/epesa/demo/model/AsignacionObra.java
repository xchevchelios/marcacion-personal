package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "asignaciones_obra")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AsignacionObra {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID empleadoId;
    @Column(nullable = false, length = 100)
    private String obraId;
    @Column(nullable = false)
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    @Column(nullable = false)
    @Builder.Default
    private LocalTime horaEntrada = LocalTime.of(8, 0);
    @Column(nullable = false)
    @Builder.Default
    private LocalTime horaSalida = LocalTime.of(17, 0);
}
