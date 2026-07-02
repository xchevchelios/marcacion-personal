package com.epesa.demo.repository;

import com.epesa.demo.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia, UUID> {

// Para RRHH
    List<Asistencia> findByRequiereRevision(boolean requiereRevision);
    
    // Para Jefes de Obra (Busca asistencias solo en una lista de sus obras)
    List<Asistencia> findByObraIdIn(List<String> obraIds);
    List<Asistencia> findByRequiereRevisionAndObraIdIn(boolean requiereRevision, List<String> obraIds);
    boolean existsByEmpleadoIdAndObraIdAndTipoMarcacionAndFechaHoraRealBetween(
            UUID empleadoId, String obraId, String tipoMarcacion, LocalDateTime desde, LocalDateTime hasta);
    List<Asistencia> findByEstadoRevisionNotOrderByFechaResolucionDesc(String estadoRevision);
    long countByFechaHoraRealBetween(LocalDateTime desde, LocalDateTime hasta);
    long countByRequiereRevisionTrue();
    long countByRequiereRevisionTrueAndTipoMarcacionAndFechaHoraRealBetween(String tipo, LocalDateTime desde, LocalDateTime hasta);

}
