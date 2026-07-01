package com.epesa.demo.service;

import com.epesa.demo.dto.AsignacionResponseDto;
import com.epesa.demo.model.AsignacionObra;
import com.epesa.demo.repository.AsignacionObraRepository;
import com.epesa.demo.repository.EmpleadoRepository;
import com.epesa.demo.repository.ObraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionObraService {

    private final AsignacionObraRepository asignacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ObraRepository obraRepository;

    /**
     * Registra la asignación de un empleado a una obra determinada.
     * Si no se especifica una fecha de inicio, se asume el día de hoy de forma automática.
     */
    @Transactional
    public AsignacionObra asignarEmpleado(AsignacionObra asignacion) {
        String codigoSap = asignacion.getObraId().trim().toUpperCase(java.util.Locale.ROOT);
        if (!empleadoRepository.existsById(asignacion.getEmpleadoId())) {
            throw new IllegalArgumentException("Empleado no encontrado");
        }
        if (!obraRepository.existsById(codigoSap)) {
            throw new IllegalArgumentException("Obra no encontrada: " + codigoSap);
        }
        asignacion.setObraId(codigoSap);
        if (asignacion.getFechaInicio() == null) {
            asignacion.setFechaInicio(LocalDate.now());
        }
        if (asignacion.getHoraEntrada() == null) {
            asignacion.setHoraEntrada(java.time.LocalTime.of(8, 0));
        }
        if (asignacion.getHoraSalida() == null) {
            asignacion.setHoraSalida(java.time.LocalTime.of(17, 0));
        }
        return asignacionRepository.save(asignacion);
    }

    public List<AsignacionResponseDto> listarAsignaciones() {
        return asignacionRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public void eliminarAsignacion(UUID id) {
        asignacionRepository.deleteById(id);
    }

    private AsignacionResponseDto convertToDto(AsignacionObra asignacion) {
        String empleadoNombre = empleadoRepository.findById(asignacion.getEmpleadoId())
                .map(e -> e.getNombreCompleto())
                .orElse("Empleado desconocido");
        String obraNombre = obraRepository.findById(asignacion.getObraId())
                .map(o -> o.getNombre())
                .orElse("Obra desconocida");

        return new AsignacionResponseDto(
                asignacion.getId(),
                asignacion.getEmpleadoId(),
                asignacion.getObraId(),
                empleadoNombre,
                obraNombre,
                asignacion.getFechaInicio(),
                asignacion.getHoraEntrada(),
                asignacion.getHoraSalida()
        );
    }

    /**
     * Verifica si el empleado cuenta con una asignación activa para la obra indicada
     * en el momento exacto en que se realizó la marcación en campo.
     * * Regla de negocio: 
     * - La fecha de marcación debe ser igual o posterior a la fecha de inicio.
     * - Si la fecha de fin es null, la asignación sigue vigente de manera indefinida.
     * - Si tiene fecha de fin, la marcación no debe superar ese límite.
     */
    public boolean verificarAsignacionActiva(UUID empleadoId, String obraId, LocalDateTime fechaMarcacion) {
        LocalDate fecha = fechaMarcacion.toLocalDate();
        List<AsignacionObra> asignaciones = asignacionRepository.findByEmpleadoIdAndObraId(empleadoId, obraId);
        
        return asignaciones.stream().anyMatch(asig -> 
            !fecha.isBefore(asig.getFechaInicio()) && 
            (asig.getFechaFin() == null || !fecha.isAfter(asig.getFechaFin()))
        );
    }
}
