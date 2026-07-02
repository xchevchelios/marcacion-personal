package com.epesa.demo.repository;

import com.epesa.demo.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import com.epesa.demo.model.enums.Rol;

public interface EmpleadoRepository extends JpaRepository<Empleado, UUID> {
    Optional<Empleado> findByCorreo(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, UUID id);
    boolean existsByDocumentoIdentidad(String documentoIdentidad);
    boolean existsByDocumentoIdentidadAndIdNot(String documentoIdentidad, UUID id);
    long countByActivoTrue();
    boolean existsByRol(Rol rol);
}
