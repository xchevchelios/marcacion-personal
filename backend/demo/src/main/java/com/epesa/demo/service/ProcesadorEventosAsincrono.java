package com.epesa.demo.service;

import com.epesa.demo.model.Asistencia;
import com.epesa.demo.model.MarcacionInbox;
import com.epesa.demo.repository.AsistenciaRepository;
import com.epesa.demo.repository.EmpleadoRepository;
import com.epesa.demo.repository.MarcacionInboxRepository;
import com.epesa.demo.repository.ObraRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
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
    private final EmpleadoRepository empleadoRepository;
    private final ObraRepository obraRepository;
    private final GeocercaService geocercaService;
    private final ObjectMapper objectMapper;
    private final AsignacionObraService asignacionObraService;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelay = 10000)
    public void procesarInbox() {
        List<MarcacionInbox> pendientes = inboxRepository.findByEstado(MarcacionInbox.EstadoEvento.PENDING);
        if (pendientes.isEmpty()) return;

        log.info("Procesador: encontradas {} marcaciones pendientes.", pendientes.size());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        for (MarcacionInbox marcacion : pendientes) {
            try {
                transactionTemplate.executeWithoutResult(status -> procesarMarcacion(marcacion));
            } catch (Exception e) {
                log.error("Error critico procesando evento {}: {}", marcacion.getEventId(), e.getMessage());
                transactionTemplate.executeWithoutResult(status -> marcarError(marcacion));
            }
        }
    }

    private void procesarMarcacion(MarcacionInbox marcacion) {
        if (asistenciaRepository.existsByEventId(marcacion.getEventId())) {
            marcacion.setEstado(MarcacionInbox.EstadoEvento.PROCESSED);
            inboxRepository.saveAndFlush(marcacion);
            return;
        }

        try {
            JsonNode json = objectMapper.readTree(marcacion.getPayloadRaw());
            String obraId = leerTexto(json, "obraId", "codigoSap").toUpperCase(Locale.ROOT);
            UUID empleadoId = resolverEmpleadoId(leerTexto(json, "empleadoId", "documentoIdentidad"));
            Double lat = leerDouble(json, "lat", "latitud");
            Double lng = leerDouble(json, "lng", "longitud");
            String tipoMarcacion = normalizarTipoMarcacion(json.path("tipoMarcacion").asText("ENTRADA"));

            boolean ubicacionValida = obraRepository.findById(obraId)
                    .map(obra -> geocercaService.puntoDentroDePoligono(lat, lng, obra.getAreaGeocerca()))
                    .orElse(false);
            if (!ubicacionValida) {
                marcarError(marcacion);
                return;
            }

            boolean requiereRevision = json.path("marcacionTardia").asBoolean(false);
            String motivo = leerMotivo(json, tipoMarcacion, requiereRevision);

            boolean tieneAsignacionActiva = asignacionObraService.verificarAsignacionActiva(
                    empleadoId, obraId, marcacion.getTimestampDispositivo());

            if (!tieneAsignacionActiva) {
                requiereRevision = true;
                motivo = "Empleado sin asignacion activa en la obra para la fecha indicada.";
                log.info("Marcacion del empleado {} guardada con revision.", empleadoId);
            }

            asistenciaRepository.saveAndFlush(Asistencia.builder()
                    .eventId(marcacion.getEventId())
                    .deviceId(marcacion.getDeviceId())
                    .obraId(obraId)
                    .empleadoId(empleadoId)
                    .tipoMarcacion(tipoMarcacion)
                    .fechaHoraReal(marcacion.getTimestampDispositivo())
                    .latitud(lat)
                    .longitud(lng)
                    .fechaProcesamiento(LocalDateTime.now())
                    .requiereRevision(requiereRevision)
                    .estadoRevision(requiereRevision ? "PENDIENTE" : "SIN_REVISION")
                    .motivoRevision(motivo)
                    .build());

            marcacion.setEstado(MarcacionInbox.EstadoEvento.PROCESSED);
            inboxRepository.saveAndFlush(marcacion);
        } catch (Exception e) {
            log.error("Evento {} invalido: {}", marcacion.getEventId(), e.getMessage());
            marcarError(marcacion);
        }
    }

    private void marcarError(MarcacionInbox marcacion) {
        marcacion.setEstado(MarcacionInbox.EstadoEvento.ERROR);
        inboxRepository.saveAndFlush(marcacion);
    }

    private String leerTexto(JsonNode json, String... nombres) {
        for (String nombre : nombres) {
            JsonNode node = json.path(nombre);
            if (!node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) {
                return node.asText().trim();
            }
        }
        throw new IllegalArgumentException("Falta campo requerido: " + String.join("/", nombres));
    }

    private Double leerDouble(JsonNode json, String... nombres) {
        String valor = leerTexto(json, nombres);
        return Double.valueOf(valor);
    }

    private UUID resolverEmpleadoId(String empleadoIdRaw) {
        try {
            return UUID.fromString(empleadoIdRaw);
        } catch (IllegalArgumentException ignored) {
            return empleadoRepository.findByDocumentoIdentidad(empleadoIdRaw)
                    .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado: " + empleadoIdRaw))
                    .getId();
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
                ? "Salida tardia: supero los 15 minutos de tolerancia."
                : "Llegada tardia: supero los 15 minutos de tolerancia.";
    }
}
