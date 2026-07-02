package com.epesa.demo.dto;

public record DashboardSummaryDto(long empleadosActivos, long obrasActivas,
        long marcacionesHoy, long excepcionesPendientes,
        long llegadasTardiasHoy, long salidasTardiasHoy) {}
