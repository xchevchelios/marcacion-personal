package com.epesa.demo.dto;

import com.epesa.demo.model.enums.Rol;
import com.epesa.demo.model.enums.TipoContrato;
import com.epesa.demo.model.enums.EstadoAprobacion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String nombreCompleto;
    private Rol rol;
    private TipoContrato tipoContrato;
    private EstadoAprobacion estadoAprobacion;
}
