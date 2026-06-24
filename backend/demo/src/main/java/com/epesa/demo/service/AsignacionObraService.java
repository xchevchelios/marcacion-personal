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

    @Transactional
    public AsignacionObra asignarEmpleado(AsignacionObra asignacion) {
        if (asignacion.getFechaInicio() == null) {
            asignacion.setFechaInicio(LocalDate.now());
        }
        return asignacionRepository.save(asignacion);
    }

    // El orquestador consumirá este método para evaluar el flag de revisión
    public boolean verificarAsignacionActiva(UUID empleadoId, UUID obraId, LocalDateTime fechaMarcacion) {
        LocalDate fecha = fechaMarcacion.toLocalDate();
        List<AsignacionObra> asignaciones = asignacionRepository.findByEmpleadoIdAndObraId(empleadoId, obraId);
        
        return asignaciones.stream().anyMatch(asig -> 
            !fecha.isBefore(asig.getFechaInicio()) && 
            (asig.getFechaFin() == null || !fecha.isAfter(asig.getFechaFin()))
        );
    }
}