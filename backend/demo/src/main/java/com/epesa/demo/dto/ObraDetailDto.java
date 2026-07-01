package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ObraDetailDto {
    private String codigoSap;
    private String nombre;
    private String ubicacion;
    private String descripcion;
    private Boolean activa;
    private List<CoordenadaDto> vertices;
}
