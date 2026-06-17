package com.epesa.demo.repository;

import com.epesa.demo.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AsistenciaRepository extends JpaRepository<Asistencia, UUID> {}