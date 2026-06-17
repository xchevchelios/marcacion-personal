package com.epesa.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistroDispositivoDto {
    @NotBlank private String deviceId;
    @NotBlank private String firmaHardware;
    private String descripcion;
}