package com.epesa.demo.service;

import com.epesa.demo.model.AsignacionObra;
import com.epesa.demo.repository.AsignacionObraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsignacionObraService {

    private final AsignacionObraRepository asignacionRepository;

    /**
     * Registra la asignación de un empleado a una obra determinada.
     * Si no se especifica una fecha de inicio, se asume el día de hoy de forma automática.
     */
    @Transactional
    public AsignacionObra asignarEmpleado(AsignacionObra asignacion) {
        if (asignacion.getFechaInicio() == null) {
            asignacion.setFechaInicio(LocalDate.now());
        }
        return asignacionRepository.save(asignacion);
    }

    /**
     * Verifica si el empleado cuenta con una asignación activa para la obra indicada
     * en el momento exacto en que se realizó la marcación en campo.
     * * Regla de negocio: 
     * - La fecha de marcación debe ser igual o posterior a la fecha de inicio.
     * - Si la fecha de fin es null, la asignación sigue vigente de manera indefinida.
     * - Si tiene fecha de fin, la marcación no debe superar ese límite.
     */
    public boolean verificarAsignacionActiva(UUID empleadoId, UUID obraId, LocalDateTime fechaMarcacion) {
        LocalDate fecha = fechaMarcacion.toLocalDate();
        List<AsignacionObra> asignaciones = asignacionRepository.findByEmpleadoIdAndObraId(empleadoId, obraId);
        
        return asignaciones.stream().anyMatch(asig -> 
            !fecha.isBefore(asig.getFechaInicio()) && 
            (asig.getFechaFin() == null || !fecha.isAfter(asig.getFechaFin()))
        );
    }
}