package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ObraResponseDto {
    private String codigoSap;
    private String nombre;
    private String ubicacion;
    private String descripcion;
    private Boolean activa;
}
