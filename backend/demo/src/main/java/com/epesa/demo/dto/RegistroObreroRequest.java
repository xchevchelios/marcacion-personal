package com.epesa.demo.dto;

import com.epesa.demo.model.enums.TipoContrato;
import jakarta.validation.constraints.*;

public record RegistroObreroRequest(
        @NotBlank String nombreCompleto,
        @NotBlank @Email String correo,
        @NotBlank String documentoIdentidad,
        @NotNull TipoContrato tipoContrato,
        @NotBlank @Size(min = 8) String password
) {}
