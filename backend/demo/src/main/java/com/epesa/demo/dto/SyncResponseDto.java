package com.epesa.demo.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class SyncResponseDto {
    private String status;
    private boolean aceptada;
    private boolean online;
    private String mensaje;
    private List<UUID> eventosSincronizados;

    public SyncResponseDto(String status, List<UUID> eventosSincronizados) {
        this.status = status;
        this.aceptada = true;
        this.online = true;
        this.mensaje = "Marcaciones sincronizadas correctamente";
        this.eventosSincronizados = eventosSincronizados;
    }
}
