package com.epesa.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MobileMarcacionRequest(
        @NotBlank String codigoSap,
        @NotNull Double latitud,
        @NotNull Double longitud,
        String dispositivo,
        String tipoMarcacion
) {}
