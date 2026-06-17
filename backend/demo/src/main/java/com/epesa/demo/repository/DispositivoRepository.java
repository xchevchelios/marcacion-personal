package com.epesa.demo.repository;

import com.epesa.demo.model.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID> {
    Optional<Dispositivo> findByDeviceId(String deviceId);
}