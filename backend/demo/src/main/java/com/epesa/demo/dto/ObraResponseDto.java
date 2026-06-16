package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ObraResponseDto {
    private UUID id;
    private String nombre;
}