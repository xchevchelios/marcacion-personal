package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AsignacionResponseDto {
    private UUID id;
    private UUID empleadoId;
    private UUID obraId;
    private String empleadoNombre;
    private String obraNombre;
    private LocalDate fechaInicio;
}
