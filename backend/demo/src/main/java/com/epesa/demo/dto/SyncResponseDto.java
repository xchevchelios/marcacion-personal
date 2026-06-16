package com.epesa.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class SyncResponseDto {
    private String status;
    private List<UUID> eventosSincronizados;
}