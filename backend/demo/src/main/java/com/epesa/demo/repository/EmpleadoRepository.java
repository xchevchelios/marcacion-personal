package com.epesa.demo.repository;

import com.epesa.demo.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmpleadoRepository extends JpaRepository<Empleado, UUID> {
    Optional<Empleado> findByCorreo(String correo);
}