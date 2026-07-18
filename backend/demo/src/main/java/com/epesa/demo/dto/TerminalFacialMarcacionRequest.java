package com.epesa.demo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TerminalFacialMarcacionRequest(
        UUID eventId,
        String terminalId,
        LocalDateTime timestampDispositivo,
        String empleadoReconocidoRef,
        Double confidenceScore,
        String biometricModel,
        String padronVersion,
        Double latitud,
        Double longitud,
        Double precisionMetros,
        String metodoMarcacion,
        Boolean offlineCapable
) {}
