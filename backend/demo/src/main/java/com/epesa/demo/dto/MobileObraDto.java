package com.epesa.demo.dto;

import java.time.LocalTime;

public record MobileObraDto(String codigoSap, String nombre, String ubicacion,
                            LocalTime horaEntrada, LocalTime horaSalida) {}
