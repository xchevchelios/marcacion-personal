package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ObraDetailDto {
    private UUID id;
    private String nombre;
    private String ubicacion;
    private String descripcion;
    private Boolean activa;
    private List<CoordenadaDto> vertices;
}
