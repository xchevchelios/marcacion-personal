package com.epesa.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AsignacionRequestDto(@NotNull UUID empleadoId, @NotBlank String obraId,
        LocalDate fechaInicio, LocalDate fechaFin, LocalTime horaEntrada, LocalTime horaSalida) {}
