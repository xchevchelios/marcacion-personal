package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidacionEspacialResult {
    private boolean esValido;
    private String mensaje;
}