package com.epesa.demo.service;

import com.epesa.demo.dto.TerminalFacialMarcacionRequest;
import com.epesa.demo.dto.TerminalFacialMarcacionResponse;
import com.epesa.demo.model.AsignacionObra;
import com.epesa.demo.model.Asistencia;
import com.epesa.demo.model.Empleado;
import com.epesa.demo.model.Obra;
import com.epesa.demo.model.enums.EstadoAprobacion;
import com.epesa.demo.repository.AsignacionObraRepository;
import com.epesa.demo.repository.AsistenciaRepository;
import com.epesa.demo.repository.EmpleadoRepository;
import com.epesa.demo.repository.ObraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminalFacialMarcacionService {
    private static final double SCORE_MINIMO_CONFIABLE = 0.80;
    private static final double GPS_PRECISION_MAXIMA_METROS = 80.0;
    private static final int TOLERANCIA_MINUTOS = 15;
    private static final int DUPLICADO_RECIENTE_MINUTOS = 5;

    private final EmpleadoRepository empleadoRepository;
    private final AsignacionObraRepository asignacionRepository;
    private final ObraRepository obraRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final GeocercaService geocercaService;
    private final MinioStorageService storageService;

    @Transactional
    public TerminalFacialMarcacionResponse registrar(TerminalFacialMarcacionRequest request,
                                                     MultipartFile evidencia) {
        UUID eventId = request.eventId() == null ? UUID.randomUUID() : request.eventId();
        LocalDateTime fechaEvento = request.timestampDispositivo() == null
                ? LocalDateTime.now()
                : request.timestampDispositivo();

        if (asistenciaRepository.existsByEventId(eventId)) {
            return new TerminalFacialMarcacionResponse(true, false,
                    "Evento facial ya registrado", eventId, null, null, null, request.confidenceScore());
        }

        Optional<Empleado> empleadoOpt = resolverEmpleado(request.empleadoReconocidoRef());
        if (empleadoOpt.isEmpty()) {
            return rechazado("No se encontro el empleado reconocido por la terminal",
                    eventId, request.confidenceScore());
        }

        Empleado empleado = empleadoOpt.get();
        if (!empleado.isActivo()) {
            return rechazado("El empleado reconocido esta inactivo", eventId, empleado.getId(),
                    request.confidenceScore());
        }
        if (empleado.getEstadoAprobacion() != EstadoAprobacion.APROBADO) {
            return rechazado("El empleado reconocido no esta aprobado para marcacion",
                    eventId, empleado.getId(), request.confidenceScore());
        }
        if (request.latitud() == null || request.longitud() == null) {
            return rechazado("La terminal no envio una ubicacion valida",
                    eventId, empleado.getId(), request.confidenceScore());
        }

        List<AsignacionObra> asignaciones = asignacionesVigentes(empleado.getId(), fechaEvento.toLocalDate());
        if (asignaciones.isEmpty()) {
            return rechazado("El empleado reconocido no tiene asignaciones vigentes",
                    eventId, empleado.getId(), request.confidenceScore());
        }

        Obra obra = buscarObraPorGeocerca(asignaciones, request.latitud(), request.longitud())
                .orElseGet(() -> primeraObraDisponible(asignaciones).orElse(null));
        if (obra == null) {
            return rechazado("Las asignaciones vigentes no apuntan a obras existentes",
                    eventId, empleado.getId(), request.confidenceScore());
        }

        AsignacionObra asignacion = asignaciones.stream()
                .filter(a -> a.getObraId().equals(obra.getCodigoSap()))
                .findFirst()
                .orElse(asignaciones.getFirst());

        Optional<Asistencia> ultima = ultimaAsistenciaDelDia(empleado.getId(), obra.getCodigoSap(), fechaEvento);
        if (ultima.isPresent()
                && Duration.between(ultima.get().getFechaHoraReal(), fechaEvento).abs()
                        .toMinutes() < DUPLICADO_RECIENTE_MINUTOS) {
            return new TerminalFacialMarcacionResponse(false, false,
                    "Ya existe una marcacion reciente para este empleado", eventId,
                    empleado.getId(), obra.getCodigoSap(), ultima.get().getTipoMarcacion(),
                    request.confidenceScore());
        }

        String tipoMarcacion = ultima.map(Asistencia::getTipoMarcacion)
                .filter("ENTRADA"::equals)
                .map(tipo -> "SALIDA")
                .orElse("ENTRADA");

        List<String> motivos = new ArrayList<>();
        boolean dentroGeocerca = geocercaService.puntoDentroDePoligono(
                request.latitud(), request.longitud(), obra.getAreaGeocerca());
        if (!dentroGeocerca) {
            motivos.add("Ubicacion fuera de geocerca para las obras asignadas.");
        }
        if (request.confidenceScore() == null) {
            motivos.add("La terminal no informo score de reconocimiento facial.");
        } else if (request.confidenceScore() < SCORE_MINIMO_CONFIABLE) {
            motivos.add("Score facial por debajo del minimo confiable.");
        }
        if (request.precisionMetros() != null && request.precisionMetros() > GPS_PRECISION_MAXIMA_METROS) {
            motivos.add("Precision GPS insuficiente.");
        }
        evaluarHorario(tipoMarcacion, asignacion, fechaEvento, motivos);

        String evidenciaObjectName = null;
        String evidenciaBucket = null;
        String evidenciaContentType = null;
        if (evidencia != null && !evidencia.isEmpty()) {
            evidenciaObjectName = storageService.subirEvidencia(eventId, evidencia);
            evidenciaBucket = storageService.obtenerBucketName();
            evidenciaContentType = evidencia.getContentType();
        }

        boolean requiereRevision = !motivos.isEmpty();
        asistenciaRepository.saveAndFlush(Asistencia.builder()
                .eventId(eventId)
                .deviceId(request.terminalId() == null || request.terminalId().isBlank()
                        ? "TERMINAL-NO-INFORMADA"
                        : request.terminalId().trim())
                .obraId(obra.getCodigoSap())
                .empleadoId(empleado.getId())
                .tipoMarcacion(tipoMarcacion)
                .fechaHoraReal(fechaEvento)
                .latitud(request.latitud())
                .longitud(request.longitud())
                .fechaProcesamiento(LocalDateTime.now())
                .requiereRevision(requiereRevision)
                .estadoRevision(requiereRevision ? "PENDIENTE" : "SIN_REVISION")
                .motivoRevision(requiereRevision ? String.join(" ", motivos) : null)
                .metodoMarcacion("FACIAL_TERMINAL")
                .terminalId(request.terminalId())
                .confidenceScore(request.confidenceScore())
                .precisionMetros(request.precisionMetros())
                .biometricModel(blankToNull(request.biometricModel()))
                .padronVersion(blankToNull(request.padronVersion()))
                .evidenciaBucket(evidenciaBucket)
                .evidenciaObjectName(evidenciaObjectName)
                .evidenciaContentType(evidenciaContentType)
                .build());

        String mensaje = requiereRevision
                ? "Asistencia facial registrada y enviada a revision"
                : "Asistencia facial registrada correctamente";
        return new TerminalFacialMarcacionResponse(true, requiereRevision, mensaje,
                eventId, empleado.getId(), obra.getCodigoSap(), tipoMarcacion, request.confidenceScore());
    }

    private Optional<Empleado> resolverEmpleado(String referencia) {
        if (referencia == null || referencia.isBlank()) return Optional.empty();
        String normalizada = referencia.trim();
        try {
            UUID id = UUID.fromString(normalizada);
            Optional<Empleado> porId = empleadoRepository.findById(id);
            if (porId.isPresent()) return porId;
        } catch (IllegalArgumentException ignored) {
            // La referencia simulada puede ser documento o correo.
        }
        Optional<Empleado> porDocumento = empleadoRepository.findByDocumentoIdentidad(normalizada);
        if (porDocumento.isPresent()) return porDocumento;
        if (normalizada.contains("@")) {
            return empleadoRepository.findByCorreo(normalizada.toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private List<AsignacionObra> asignacionesVigentes(UUID empleadoId, LocalDate fecha) {
        return asignacionRepository.findByEmpleadoId(empleadoId).stream()
                .filter(a -> !fecha.isBefore(a.getFechaInicio()))
                .filter(a -> a.getFechaFin() == null || !fecha.isAfter(a.getFechaFin()))
                .sorted(Comparator.comparing(AsignacionObra::getFechaInicio).reversed())
                .toList();
    }

    private Optional<Obra> buscarObraPorGeocerca(List<AsignacionObra> asignaciones, Double lat, Double lng) {
        return asignaciones.stream()
                .map(a -> obraRepository.findById(a.getObraId()))
                .flatMap(Optional::stream)
                .filter(Obra::getActiva)
                .filter(obra -> geocercaService.puntoDentroDePoligono(lat, lng, obra.getAreaGeocerca()))
                .findFirst();
    }

    private Optional<Obra> primeraObraDisponible(List<AsignacionObra> asignaciones) {
        return asignaciones.stream()
                .map(a -> obraRepository.findById(a.getObraId()))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<Asistencia> ultimaAsistenciaDelDia(UUID empleadoId, String obraId, LocalDateTime fechaEvento) {
        LocalDateTime desde = fechaEvento.toLocalDate().atStartOfDay();
        LocalDateTime hasta = desde.plusDays(1);
        return asistenciaRepository
                .findTopByEmpleadoIdAndObraIdAndFechaHoraRealBetweenOrderByFechaHoraRealDesc(
                        empleadoId, obraId, desde, hasta);
    }

    private void evaluarHorario(String tipoMarcacion, AsignacionObra asignacion,
                                LocalDateTime fechaEvento, List<String> motivos) {
        LocalDateTime entrada = fechaEvento.toLocalDate().atTime(asignacion.getHoraEntrada());
        LocalDateTime salida = fechaEvento.toLocalDate().atTime(asignacion.getHoraSalida());
        if ("ENTRADA".equals(tipoMarcacion) && fechaEvento.isBefore(entrada)) {
            motivos.add("Marcacion antes del horario de entrada.");
            return;
        }
        if ("ENTRADA".equals(tipoMarcacion) && fechaEvento.isAfter(entrada.plusMinutes(TOLERANCIA_MINUTOS))) {
            motivos.add("Llegada tardia: supero los 15 minutos de tolerancia.");
        }
        if ("SALIDA".equals(tipoMarcacion) && fechaEvento.isAfter(salida.plusMinutes(TOLERANCIA_MINUTOS))) {
            motivos.add("Salida tardia: supero los 15 minutos de tolerancia.");
        }
    }

    private TerminalFacialMarcacionResponse rechazado(String mensaje, UUID eventId, Double score) {
        return new TerminalFacialMarcacionResponse(false, false, mensaje, eventId,
                null, null, null, score);
    }

    private TerminalFacialMarcacionResponse rechazado(String mensaje, UUID eventId, UUID empleadoId, Double score) {
        return new TerminalFacialMarcacionResponse(false, false, mensaje, eventId,
                empleadoId, null, null, score);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
