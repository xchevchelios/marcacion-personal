package com.epesa.demo.service;

import com.epesa.demo.dto.*;
import com.epesa.demo.exception.*;
import com.epesa.demo.model.*;
import com.epesa.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class AsignacionObraService {
    private final AsignacionObraRepository repository;
    private final EmpleadoRepository empleadoRepository;
    private final ObraRepository obraRepository;
    private final AuditService auditService;

    @Transactional
    public AsignacionResponseDto asignarEmpleado(AsignacionRequestDto request) {
        String obraId = request.obraId().trim().toUpperCase(Locale.ROOT);
        Empleado empleado = empleadoRepository.findById(request.empleadoId()).orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));
        Obra obra = obraRepository.findById(obraId).orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada: " + obraId));
        if (!empleado.isActivo()) throw new BusinessRuleException("No se puede asignar un empleado inactivo");
        if (!obra.getActiva()) throw new BusinessRuleException("No se puede asignar personal a una obra inactiva");
        if (repository.existsByEmpleadoIdAndObraIdAndFechaFinIsNull(request.empleadoId(), obraId))
            throw new BusinessRuleException("El empleado ya tiene una asignaciÃ³n activa en esta obra");
        LocalTime entrada = request.horaEntrada() == null ? LocalTime.of(8, 0) : request.horaEntrada();
        LocalTime salida = request.horaSalida() == null ? LocalTime.of(17, 0) : request.horaSalida();
        if (!salida.isAfter(entrada)) throw new BusinessRuleException("La hora de salida debe ser posterior a la entrada");
        AsignacionObra entity = AsignacionObra.builder().empleadoId(request.empleadoId()).obraId(obraId)
                .fechaInicio(request.fechaInicio() == null ? LocalDate.now() : request.fechaInicio())
                .fechaFin(request.fechaFin()).horaEntrada(entrada).horaSalida(salida).build();
        entity = repository.save(entity);
        auditService.record("CREATE", "ASIGNACION", entity.getId(), empleado.getNombreCompleto() + " -> " + obraId);
        return toDto(entity, empleado.getNombreCompleto(), obra.getNombre());
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponseDto> listarAsignaciones() {
        List<AsignacionObra> rows = repository.findAll();
        Map<UUID, Empleado> empleados = empleadoRepository.findAllById(rows.stream().map(AsignacionObra::getEmpleadoId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(Empleado::getId, Function.identity()));
        Map<String, Obra> obras = obraRepository.findAllById(rows.stream().map(AsignacionObra::getObraId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(Obra::getCodigoSap, Function.identity()));
        return rows.stream().map(row -> toDto(row,
                Optional.ofNullable(empleados.get(row.getEmpleadoId())).map(Empleado::getNombreCompleto).orElse("Empleado desconocido"),
                Optional.ofNullable(obras.get(row.getObraId())).map(Obra::getNombre).orElse("Obra desconocida"))).toList();
    }

    @Transactional
    public void eliminarAsignacion(UUID id) {
        AsignacionObra entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("AsignaciÃ³n no encontrada"));
        repository.delete(entity); auditService.record("DELETE", "ASIGNACION", id, entity.getEmpleadoId() + " -> " + entity.getObraId());
    }

    @Transactional(readOnly = true)
    public boolean verificarAsignacionActiva(UUID empleadoId, String obraId, LocalDateTime fechaMarcacion) {
        LocalDate fecha = fechaMarcacion.toLocalDate();
        return repository.findByEmpleadoIdAndObraId(empleadoId, obraId).stream().anyMatch(a -> !fecha.isBefore(a.getFechaInicio()) && (a.getFechaFin() == null || !fecha.isAfter(a.getFechaFin())));
    }
    private AsignacionResponseDto toDto(AsignacionObra a, String empleado, String obra) { return new AsignacionResponseDto(a.getId(), a.getEmpleadoId(), a.getObraId(), empleado, obra, a.getFechaInicio(), a.getHoraEntrada(), a.getHoraSalida()); }
}
