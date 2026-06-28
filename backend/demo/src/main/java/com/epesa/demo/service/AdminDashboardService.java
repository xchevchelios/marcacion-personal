package com.epesa.demo.service;

import com.epesa.demo.dto.AsistenciaResponseDTO;
import com.epesa.demo.model.AsignacionObra;
import com.epesa.demo.model.Asistencia;
import com.epesa.demo.model.Empleado;
import com.epesa.demo.model.Obra;
import com.epesa.demo.model.enums.Rol;
import com.epesa.demo.repository.AsignacionObraRepository;
import com.epesa.demo.repository.AsistenciaRepository;
import com.epesa.demo.repository.EmpleadoRepository;
import com.epesa.demo.repository.ObraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AsistenciaRepository asistenciaRepository;
    private final AsignacionObraRepository asignacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ObraRepository obraRepository;

    // Métodos actualizados para devolver DTOs
    public List<AsistenciaResponseDTO> obtenerAsistencias(Empleado administrador) {
        List<Asistencia> asistencias;
        if (administrador.getRol() == Rol.RRHH) {
            asistencias = asistenciaRepository.findAll();
        } else {
            List<UUID> misObras = obtenerObrasDelAdministrador(administrador.getId());
            asistencias = asistenciaRepository.findByObraIdIn(misObras);
        }
        return asistencias.stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    public List<AsistenciaResponseDTO> obtenerExcepciones(Empleado administrador) {
        List<Asistencia> excepciones;
        if (administrador.getRol() == Rol.RRHH) {
            excepciones = asistenciaRepository.findByRequiereRevision(true);
        } else {
            List<UUID> misObras = obtenerObrasDelAdministrador(administrador.getId());
            excepciones = asistenciaRepository.findByRequiereRevisionAndObraIdIn(true, misObras);
        }
        return excepciones.stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    public AsistenciaResponseDTO resolverExcepcion(UUID asistenciaId, boolean aprobar, String notaResolucion) {
        Asistencia asistencia = asistenciaRepository.findById(asistenciaId)
                .orElseThrow(() -> new RuntimeException("Asistencia no encontrada"));

        if (aprobar) {
            asistencia.setRequiereRevision(false);
            asistencia.setMotivoRevision("Aprobado manualmente: " + notaResolucion);
        } else {
            asistencia.setMotivoRevision("Rechazado manualmente: " + notaResolucion);
        }
        
        return convertirADTO(asistenciaRepository.save(asistencia));
    }

    @SuppressWarnings("null")
    private List<UUID> obtenerObrasDelAdministrador(UUID empleadoId) {
        return asignacionRepository.findByEmpleadoId(empleadoId).stream()
                .map(AsignacionObra::getObraId)
                .collect(Collectors.toList());
    }

    // El motor de conversión
    private AsistenciaResponseDTO convertirADTO(Asistencia asistencia) {
        Empleado empleado = empleadoRepository.findById(asistencia.getEmpleadoId()).orElse(null);
        Obra obra = obraRepository.findById(asistencia.getObraId()).orElse(null);

        return AsistenciaResponseDTO.builder()
                .id(asistencia.getId())
                .eventId(asistencia.getEventId())
                .empleadoNombre(empleado != null ? empleado.getNombreCompleto() : "Desconocido")
                .empleadoDocumento(empleado != null ? empleado.getDocumentoIdentidad() : "N/A")
                .empleadoTipoContrato(empleado != null ? empleado.getTipoContrato().name() : "N/A")
                .obraNombre(obra != null ? obra.getNombre() : "Obra Desconocida")
                .deviceId(asistencia.getDeviceId())
                .fechaHoraReal(asistencia.getFechaHoraReal())
                .requiereRevision(asistencia.isRequiereRevision())
                .motivoRevision(asistencia.getMotivoRevision())
                .build();
    }
}