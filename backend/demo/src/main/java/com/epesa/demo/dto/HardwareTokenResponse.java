package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HardwareTokenResponse {
    private boolean esConfiable;
    private String mensaje;
}