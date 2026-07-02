package com.epesa.demo.dto;

import com.epesa.demo.model.enums.*;
import java.util.UUID;

public record EmpleadoResponseDto(UUID id, String nombreCompleto, String correo,
        String documentoIdentidad, Rol rol, TipoContrato tipoContrato,
        EstadoAprobacion estadoAprobacion, boolean activo) {}
