package com.epesa.demo.service;

import com.epesa.demo.dto.*;
import com.epesa.demo.exception.*;
import com.epesa.demo.model.*;
import com.epesa.demo.model.enums.Rol;
import com.epesa.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class AdminDashboardService {
    private final AsistenciaRepository asistenciaRepository;
    private final AsignacionObraRepository asignacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ObraRepository obraRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AsistenciaResponseDTO> obtenerAsistencias(Empleado admin) { return convertAll(filtrar(admin, asistenciaRepository.findAll())); }
    @Transactional(readOnly = true)
    public List<AsistenciaResponseDTO> obtenerExcepciones(Empleado admin) { return convertAll(filtrar(admin, asistenciaRepository.findByRequiereRevision(true))); }
    @Transactional(readOnly = true)
    public List<AsistenciaResponseDTO> obtenerHistorialExcepciones(Empleado admin) { return convertAll(filtrar(admin, asistenciaRepository.findByEstadoRevisionNotOrderByFechaResolucionDesc("SIN_REVISION"))); }

    @Transactional(readOnly = true)
    public DashboardSummaryDto obtenerResumen() {
        LocalDateTime desde = LocalDate.now().atStartOfDay(), hasta = desde.plusDays(1);
        return new DashboardSummaryDto(empleadoRepository.countByActivoTrue(), obraRepository.countByActivaTrue(),
                asistenciaRepository.countByFechaHoraRealBetween(desde, hasta), asistenciaRepository.countByRequiereRevisionTrue(),
                asistenciaRepository.countByRequiereRevisionTrueAndTipoMarcacionAndFechaHoraRealBetween("ENTRADA", desde, hasta),
                asistenciaRepository.countByRequiereRevisionTrueAndTipoMarcacionAndFechaHoraRealBetween("SALIDA", desde, hasta));
    }

    @Transactional
    public AsistenciaResponseDTO resolverExcepcion(UUID id, boolean aprobar, String nota) {
        if (nota == null || nota.isBlank()) throw new BusinessRuleException("La nota de resoluciÃ³n es obligatoria");
        Asistencia asistencia = asistenciaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asistencia no encontrada"));
        if (!asistencia.isRequiereRevision()) throw new BusinessRuleException("La excepciÃ³n ya fue resuelta");
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        asistencia.setRequiereRevision(false);
        asistencia.setEstadoRevision(aprobar ? "APROBADA" : "RECHAZADA");
        asistencia.setNotaResolucion(nota.trim()); asistencia.setResueltoPor(actor); asistencia.setFechaResolucion(LocalDateTime.now());
        auditService.record(aprobar ? "APPROVE" : "REJECT", "EXCEPCION", id, nota.trim());
        return convertAll(List.of(asistenciaRepository.save(asistencia))).getFirst();
    }

    private List<Asistencia> filtrar(Empleado admin, List<Asistencia> source) {
        if (EnumSet.of(Rol.SOPORTE, Rol.ADMIN, Rol.RRHH).contains(admin.getRol())) return source;
        Set<String> obras = asignacionRepository.findByEmpleadoId(admin.getId()).stream().map(AsignacionObra::getObraId).collect(Collectors.toSet());
        return source.stream().filter(a -> obras.contains(a.getObraId())).toList();
    }
    private List<AsistenciaResponseDTO> convertAll(List<Asistencia> rows) {
        Map<UUID, Empleado> empleados = empleadoRepository.findAllById(rows.stream().map(Asistencia::getEmpleadoId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(Empleado::getId, Function.identity()));
        Map<String, Obra> obras = obraRepository.findAllById(rows.stream().map(Asistencia::getObraId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(Obra::getCodigoSap, Function.identity()));
        return rows.stream().map(a -> toDto(a, empleados.get(a.getEmpleadoId()), obras.get(a.getObraId()))).toList();
    }
    private AsistenciaResponseDTO toDto(Asistencia a, Empleado e, Obra o) {
        return AsistenciaResponseDTO.builder().id(a.getId()).eventId(a.getEventId()).empleadoId(a.getEmpleadoId())
                .empleadoNombre(e == null ? "Desconocido" : e.getNombreCompleto()).empleadoDocumento(e == null ? "N/A" : e.getDocumentoIdentidad())
                .empleadoTipoContrato(e == null ? "N/A" : e.getTipoContrato().name()).obraId(a.getObraId()).obraNombre(o == null ? "Obra desconocida" : o.getNombre())
                .deviceId(a.getDeviceId()).fechaHoraReal(a.getFechaHoraReal()).tipoMarcacion(a.getTipoMarcacion())
                .horaEntrada("ENTRADA".equals(a.getTipoMarcacion()) ? a.getFechaHoraReal() : null).horaSalida("SALIDA".equals(a.getTipoMarcacion()) ? a.getFechaHoraReal() : null)
                .requiereRevision(a.isRequiereRevision()).motivoRevision(a.getMotivoRevision()).estadoRevision(a.getEstadoRevision())
                .notaResolucion(a.getNotaResolucion()).resueltoPor(a.getResueltoPor()).fechaResolucion(a.getFechaResolucion())
                .metodoMarcacion(a.getMetodoMarcacion()).terminalId(a.getTerminalId()).confidenceScore(a.getConfidenceScore())
                .precisionMetros(a.getPrecisionMetros()).biometricModel(a.getBiometricModel()).padronVersion(a.getPadronVersion())
                .evidenciaBucket(a.getEvidenciaBucket()).evidenciaObjectName(a.getEvidenciaObjectName())
                .evidenciaContentType(a.getEvidenciaContentType()).build();
    }
}
