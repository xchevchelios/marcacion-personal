package com.epesa.demo.dto;

import java.util.UUID;

public record MobileMarcacionResponse(boolean aceptada, boolean requiereRevision,
                                      String mensaje, UUID eventId) {}
