package com.epesa.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CoordenadaDto {
    @NotNull private Double lat;
    @NotNull private Double lng;
}