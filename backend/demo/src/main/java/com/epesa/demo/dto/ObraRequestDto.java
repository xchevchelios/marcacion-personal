package com.epesa.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class ObraRequestDto {
    @NotBlank private String nombre;
    private String ubicacion;
    private String descripcion;
    private Boolean activa;
    
    // Un polígono requiere al menos 3 puntos
    @Valid @Size(min = 3) private List<CoordenadaDto> vertices; 
}