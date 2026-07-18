package com.epesa.demo.dto;

import java.util.UUID;

public record TerminalFacialMarcacionResponse(
        boolean aceptada,
        boolean requiereRevision,
        String mensaje,
        UUID eventId,
        UUID empleadoId,
        String obraId,
        String tipoMarcacion,
        Double confidenceScore
) {}
