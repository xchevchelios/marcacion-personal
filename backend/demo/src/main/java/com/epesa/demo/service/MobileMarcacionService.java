package com.epesa.demo.service;

import com.epesa.demo.dto.MobileMarcacionRequest;
import com.epesa.demo.dto.MobileMarcacionResponse;
import com.epesa.demo.dto.MobileObraDto;
import com.epesa.demo.model.AsignacionObra;
import com.epesa.demo.model.Empleado;
import com.epesa.demo.model.MarcacionInbox;
import com.epesa.demo.model.Obra;
import com.epesa.demo.model.enums.EstadoAprobacion;
import com.epesa.demo.repository.AsignacionObraRepository;
import com.epesa.demo.repository.AsistenciaRepository;
import com.epesa.demo.repository.MarcacionInboxRepository;
import com.epesa.demo.repository.ObraRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MobileMarcacionService {
    private static final int TOLERANCIA_MINUTOS = 15;

    private final AsignacionObraRepository asignacionRepository;
    private final ObraRepository obraRepository;
    private final MarcacionInboxRepository inboxRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final GeocercaService geocercaService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<MobileObraDto> misObras(Empleado empleado) {
        validarAprobacion(empleado);
        LocalDate hoy = LocalDate.now();
        return asignacionRepository.findByEmpleadoId(empleado.getId()).stream()
                .filter(a -> !hoy.isBefore(a.getFechaInicio()))
                .filter(a -> a.getFechaFin() == null || !hoy.isAfter(a.getFechaFin()))
                .map(a -> obraRepository.findById(a.getObraId())
                        .filter(Obra::getActiva)
                        .map(o -> new MobileObraDto(o.getCodigoSap(), o.getNombre(),
                                o.getUbicacion(), a.getHoraEntrada(), a.getHoraSalida())))
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional
    public MobileMarcacionResponse marcar(Empleado empleado, MobileMarcacionRequest request) {
        validarAprobacion(empleado);
        String codigoSap = request.codigoSap().trim().toUpperCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now();

        AsignacionObra asignacion = asignacionRepository
                .findByEmpleadoIdAndObraId(empleado.getId(), codigoSap).stream()
                .filter(a -> !ahora.toLocalDate().isBefore(a.getFechaInicio()))
                .filter(a -> a.getFechaFin() == null || !ahora.toLocalDate().isAfter(a.getFechaFin()))
                .findFirst()
                .orElse(null);

        if (asignacion == null) {
            return new MobileMarcacionResponse(false, false,
                    "No tienes una asignación activa para esta obra", null);
        }

        Obra obra = obraRepository.findById(codigoSap).orElseThrow();
        if (!geocercaService.puntoDentroDePoligono(request.latitud(), request.longitud(), obra.getAreaGeocerca())) {
            return new MobileMarcacionResponse(false, false,
                    "Estás fuera de la geocerca. Acércate más a la zona de la obra", null);
        }

        String tipoMarcacion = resolverTipoMarcacion(request.tipoMarcacion(), empleado.getId(), codigoSap, ahora.toLocalDate());
        LocalDateTime horarioEntrada = ahora.toLocalDate().atTime(asignacion.getHoraEntrada());
        LocalDateTime horarioSalida = ahora.toLocalDate().atTime(asignacion.getHoraSalida());

        if ("ENTRADA".equals(tipoMarcacion) && ahora.isBefore(horarioEntrada)) {
            return new MobileMarcacionResponse(false, false,
                    "Aún no es el horario de entrada. Podrás marcar a las " + asignacion.getHoraEntrada(), null);
        }

        boolean tardia = "ENTRADA".equals(tipoMarcacion)
                ? ahora.isAfter(horarioEntrada.plusMinutes(TOLERANCIA_MINUTOS))
                : ahora.isAfter(horarioSalida.plusMinutes(TOLERANCIA_MINUTOS));

        String motivoRevision = null;
        if (tardia) {
            motivoRevision = "ENTRADA".equals(tipoMarcacion)
                    ? "Llegada tardía: superó los 15 minutos de tolerancia."
                    : "Salida tardía: superó los 15 minutos de tolerancia.";
        }

        UUID eventId = UUID.randomUUID();
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("empleadoId", empleado.getId());
            payload.put("obraId", codigoSap);
            payload.put("lat", request.latitud());
            payload.put("lng", request.longitud());
            payload.put("dispositivo", request.dispositivo());
            payload.put("tipoMarcacion", tipoMarcacion);
            payload.put("marcacionTardia", tardia);
            payload.put("motivoRevision", motivoRevision);

            inboxRepository.save(MarcacionInbox.builder()
                    .eventId(eventId)
                    .deviceId(request.dispositivo() == null ? "NO-INFORMADO" : request.dispositivo())
                    .timestampDispositivo(ahora)
                    .payloadRaw(objectMapper.writeValueAsString(payload))
                    .estado(MarcacionInbox.EstadoEvento.PENDING)
                    .fechaRecepcion(ahora)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo registrar la marcación", exception);
        }

        String mensaje = tardia
                ? ("ENTRADA".equals(tipoMarcacion)
                    ? "Llegada tardía aceptada y enviada a revisión de RRHH"
                    : "Salida tardía aceptada y enviada a revisión de RRHH")
                : ("ENTRADA".equals(tipoMarcacion) ? "Entrada registrada correctamente" : "Salida registrada correctamente");
        return new MobileMarcacionResponse(true, tardia, mensaje, eventId);
    }

    private String resolverTipoMarcacion(String tipoSolicitado, UUID empleadoId, String obraId, LocalDate fecha) {
        if (tipoSolicitado != null && !tipoSolicitado.isBlank()) {
            String normalizado = tipoSolicitado.trim().toUpperCase(Locale.ROOT);
            if ("ENTRADA".equals(normalizado) || "SALIDA".equals(normalizado)) {
                return normalizado;
            }
        }

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();

        boolean yaTieneEntradaProcesada = asistenciaRepository
                .existsByEmpleadoIdAndObraIdAndTipoMarcacionAndFechaHoraRealBetween(
                        empleadoId, obraId, "ENTRADA", inicioDia, finDia);
        if (yaTieneEntradaProcesada) {
            return "SALIDA";
        }

        boolean entradaPendiente = inboxRepository.findByTimestampDispositivoBetween(inicioDia, finDia).stream()
                .anyMatch(m -> {
                    try {
                        var json = objectMapper.readTree(m.getPayloadRaw());
                        return empleadoId.toString().equals(json.path("empleadoId").asText())
                                && obraId.equals(json.path("obraId").asText())
                                && "ENTRADA".equals(json.path("tipoMarcacion").asText("ENTRADA"));
                    } catch (Exception ignored) {
                        return false;
                    }
                });
        return entradaPendiente ? "SALIDA" : "ENTRADA";
    }

    private void validarAprobacion(Empleado empleado) {
        if (empleado.getEstadoAprobacion() != EstadoAprobacion.APROBADO) {
            throw new IllegalStateException("Tu acceso móvil está pendiente de aprobación por RRHH");
        }
    }
}
