package com.epesa.demo.service;

import com.epesa.demo.dto.MarcacionBatchRequest;
import com.epesa.demo.dto.MarcacionEventoDto;
import com.epesa.demo.model.MarcacionInbox;
import com.epesa.demo.repository.MarcacionInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarcacionInboxService {

    private final MarcacionInboxRepository inboxRepository;

    @Transactional
    public List<UUID> procesarLote(MarcacionBatchRequest request) {
        List<UUID> procesados = new ArrayList<>();

        for (MarcacionEventoDto dto : request.getMarcaciones()) {
            if (!inboxRepository.existsById(dto.getEventId())) {
                inboxRepository.save(MarcacionInbox.builder()
                        .eventId(dto.getEventId())
                        .deviceId(dto.getDeviceId())
                        .timestampDispositivo(dto.getTimestampDispositivo())
                        .payloadRaw(dto.getPayloadRaw())
                        .estado(MarcacionInbox.EstadoEvento.PENDING)
                        .fechaRecepcion(LocalDateTime.now())
                        .build());
            }
            procesados.add(dto.getEventId()); // Se añade a la respuesta sea nuevo o duplicado
        }
        return procesados;
    }
    public List<MarcacionInbox> obtenerTodas() {
        return inboxRepository.findAll();
    }
}