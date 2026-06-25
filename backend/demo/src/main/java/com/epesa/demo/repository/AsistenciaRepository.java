package com.epesa.demo.repository;

import com.epesa.demo.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia, UUID> {

// Para RRHH
    List<Asistencia> findByRequiereRevision(boolean requiereRevision);
    
    // Para Jefes de Obra (Busca asistencias solo en una lista de sus obras)
    List<Asistencia> findByObraIdIn(List<UUID> obraIds);
    List<Asistencia> findByRequiereRevisionAndObraIdIn(boolean requiereRevision, List<UUID> obraIds);

}