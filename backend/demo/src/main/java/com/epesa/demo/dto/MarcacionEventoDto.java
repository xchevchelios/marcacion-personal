package com.epesa.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MarcacionEventoDto {
    @NotNull private UUID eventId;
    @NotNull private String deviceId;
    @NotNull private LocalDateTime timestampDispositivo;
    @NotNull private String payloadRaw;
}