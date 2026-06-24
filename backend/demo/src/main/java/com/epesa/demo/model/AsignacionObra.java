package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "asignaciones_obra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionObra {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID empleadoId;

    @Column(nullable = false)
    private UUID obraId;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column
    private LocalDate fechaFin; // Null significa asignación indefinida
}