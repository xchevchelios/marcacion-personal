package com.epesa.demo.dto;

import com.epesa.demo.model.enums.Rol;
import com.epesa.demo.model.enums.TipoContrato;
import jakarta.validation.constraints.*;

public record EmpleadoRequestDto(
        @NotBlank String nombreCompleto,
        @NotBlank @Email String correo,
        @NotBlank String documentoIdentidad,
        @NotNull Rol rol,
        @NotNull TipoContrato tipoContrato,
        @Size(min = 8, message = "debe tener al menos 8 caracteres") String password,
        Boolean activo) {}
