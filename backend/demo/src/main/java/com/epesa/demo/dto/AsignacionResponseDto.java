package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AsignacionResponseDto {
    private UUID id;
    private UUID empleadoId;
    private String obraId;
    private String empleadoNombre;
    private String obraNombre;
    private LocalDate fechaInicio;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
}
