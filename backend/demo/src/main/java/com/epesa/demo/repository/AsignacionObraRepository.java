package com.epesa.demo.repository;

import com.epesa.demo.model.AsignacionObra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface AsignacionObraRepository extends JpaRepository<AsignacionObra, UUID> {
    List<AsignacionObra> findByEmpleadoIdAndObraId(UUID empleadoId, String obraId);
    List<AsignacionObra> findByEmpleadoId(UUID empleadoId);
}
