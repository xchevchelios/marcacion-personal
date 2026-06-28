package com.epesa.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.epesa.demo.model.Asistencia;
import com.epesa.demo.model.MarcacionInbox;
import com.epesa.demo.repository.AsistenciaRepository;
import com.epesa.demo.repository.MarcacionInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcesadorEventosAsincrono {

    private final MarcacionInboxRepository inboxRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final SeguridadHardwareService hardwareService;
    private final ObraService obraService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AsignacionObraService asignacionObraService;

    // Se ejecuta automáticamente cada 10 segundos
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void procesarInbox() {
        // Buscar todas las marcaciones pendientes creadas por el Módulo 1
        List<MarcacionInbox> pendientes = inboxRepository.findAll().stream()
                .filter(m -> m.getEstado() == MarcacionInbox.EstadoEvento.PENDING)
                .toList();

        if (pendientes.isEmpty()) return;

        log.info("Procesador: Encontradas {} marcaciones pendientes.", pendientes.size());

        for (MarcacionInbox marcacion : pendientes) {
            try {
                // 1. Parsear el payload dinámico enviado por el cliente móvil
                JsonNode json = objectMapper.readTree(marcacion.getPayloadRaw());
                String firma = json.get("firma_hardware").asText();
                UUID obraId = UUID.fromString(json.get("obraId").asText());
                UUID empleadoId = UUID.fromString(json.get("empleadoId").asText()); // NUEVO
                Double lat = json.get("lat").asDouble();
                Double lng = json.get("lng").asDouble();

                hardwareService.validarFirmaZeroTrust(marcacion.getDeviceId(), firma);
                if (!hardwareService.validarFirmaZeroTrust(marcacion.getDeviceId(), firma)) {
                    marcacion.setEstado(MarcacionInbox.EstadoEvento.ERROR);
                    inboxRepository.save(marcacion);
                    continue;
                }

                obraService.validarUbicacion(obraId, lat, lng).isEsValido();
                if (!obraService.validarUbicacion(obraId, lat, lng).isEsValido()) {
                    marcacion.setEstado(MarcacionInbox.EstadoEvento.ERROR);
                    inboxRepository.save(marcacion);
                    continue;
                }
                //4. filtro de recursos humanos
                boolean requiereRevision = false;
                String motivo = null;

                // ¡AQUÍ ESTÁ LA CLAVE! Llama a asignacionObraService, NO a obraService
                boolean tieneAsignacionActiva = asignacionObraService.verificarAsignacionActiva(empleadoId, obraId, marcacion.getTimestampDispositivo());

                if (!tieneAsignacionActiva) {
                    requiereRevision = true;
                    motivo = "Empleado sin asignación activa en la obra para la fecha indicada.";
                    log.info("Marcación del empleado {} guardada con FLAG de revisión.", empleadoId);
                }


                // 5. Consolidación: Guardar en la tabla final limpia de asistencias
                asistenciaRepository.save(Asistencia.builder()
                        .eventId(marcacion.getEventId())
                        .deviceId(marcacion.getDeviceId())
                        .obraId(obraId)
                        .empleadoId(empleadoId) // Guardamos quién marcó
                        .fechaHoraReal(marcacion.getTimestampDispositivo())
                        .latitud(lat)
                        .longitud(lng)
                        .fechaProcesamiento(java.time.LocalDateTime.now())
                        .requiereRevision(requiereRevision) // Guardamos el estado del flag
                        .motivoRevision(motivo)
                        .build());

                // 6. Éxito: Actualizar estado del Inbox
                marcacion.setEstado(MarcacionInbox.EstadoEvento.PROCESSED);
                inboxRepository.save(marcacion);

            } catch (Exception e) {
                log.error("Error crítico procesando evento {}: {}", marcacion.getEventId(), e.getMessage());
                marcacion.setEstado(MarcacionInbox.EstadoEvento.ERROR);
                inboxRepository.save(marcacion);
            }
        }
    }
}