package com.epesa.demo.repository;

import com.epesa.demo.model.MarcacionInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MarcacionInboxRepository extends JpaRepository<MarcacionInbox, UUID> {
    List<MarcacionInbox> findByEstado(MarcacionInbox.EstadoEvento estado);
    List<MarcacionInbox> findByTimestampDispositivoBetween(LocalDateTime desde, LocalDateTime hasta);
}
