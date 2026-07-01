package com.epesa.demo.service;

import com.epesa.demo.model.Asistencia;
import com.epesa.demo.model.MarcacionInbox;
import com.epesa.demo.repository.AsistenciaRepository;
import com.epesa.demo.repository.MarcacionInboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.processing.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ProcesadorEventosAsincrono {

    private final MarcacionInboxRepository inboxRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final ObraService obraService;
    private final ObjectMapper objectMapper;
    private final AsignacionObraService asignacionObraService;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void procesarInbox() {
        List<MarcacionInbox> pendientes = inboxRepository.findByEstado(MarcacionInbox.EstadoEvento.PENDING);

        if (pendientes.isEmpty()) return;

        log.info("Procesador: encontradas {} marcaciones pendientes.", pendientes.size());

        for (MarcacionInbox marcacion : pendientes) {
            try {
                JsonNode json = objectMapper.readTree(marcacion.getPayloadRaw());
                String obraId = json.get("obraId").asText().trim().toUpperCase(Locale.ROOT);
                UUID empleadoId = UUID.fromString(json.get("empleadoId").asText());
                Double lat = json.get("lat").asDouble();
                Double lng = json.get("lng").asDouble();
                String tipoMarcacion = normalizarTipoMarcacion(json.path("tipoMarcacion").asText("ENTRADA"));

                boolean ubicacionValida = obraService.validarUbicacion(obraId, lat, lng).isEsValido();
                if (!ubicacionValida) {
                    marcacion.setEstado(MarcacionInbox.EstadoEvento.ERROR);
                    inboxRepository.save(marcacion);
                    continue;
                }

                boolean requiereRevision = json.path("marcacionTardia").asBoolean(false);
                String motivo = leerMotivo(json, tipoMarcacion, requiereRevision);

                boolean tieneAsignacionActiva = asignacionObraService.verificarAsignacionActiva(
                        empleadoId, obraId, marcacion.getTimestampDispositivo());

                if (!tieneAsignacionActiva) {
                    requiereRevision = true;
                    motivo = "Empleado sin asignación activa en la obra para la fecha indicada.";
                    log.info("Marcación del empleado {} guardada con revisión.", empleadoId);
                }

                asistenciaRepository.save(Asistencia.builder()
                        .eventId(marcacion.getEventId())
                        .deviceId(marcacion.getDeviceId())
                        .obraId(obraId)
                        .empleadoId(empleadoId)
                        .tipoMarcacion(tipoMarcacion)
                        .fechaHoraReal(marcacion.getTimestampDispositivo())
                        .latitud(lat)
                        .longitud(lng)
                        .fechaProcesamiento(java.time.LocalDateTime.now())
                        .requiereRevision(requiereRevision)
                        .motivoRevision(motivo)
                        .build());

                marcacion.setEstado(MarcacionInbox.EstadoEvento.PROCESSED);
                inboxRepository.save(marcacion);

            } catch (Exception e) {
                log.error("Error crítico procesando evento {}: {}", marcacion.getEventId(), e.getMessage());
                marcacion.setEstado(MarcacionInbox.EstadoEvento.ERROR);
                inboxRepository.save(marcacion);
            }
        }
    }

    private String normalizarTipoMarcacion(String tipoMarcacion) {
        String normalizado = tipoMarcacion == null ? "ENTRADA" : tipoMarcacion.trim().toUpperCase(Locale.ROOT);
        return "SALIDA".equals(normalizado) ? "SALIDA" : "ENTRADA";
    }

    private String leerMotivo(JsonNode json, String tipoMarcacion, boolean requiereRevision) {
        JsonNode motivoNode = json.path("motivoRevision");
        if (!motivoNode.isMissingNode() && !motivoNode.isNull() && !motivoNode.asText().isBlank()) {
            return motivoNode.asText();
        }
        if (!requiereRevision) {
            return null;
        }
        return "SALIDA".equals(tipoMarcacion)
                ? "Salida tardía: superó los 15 minutos de tolerancia."
                : "Llegada tardía: superó los 15 minutos de tolerancia.";
    }
}
