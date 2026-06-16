package repository;

import model.MarcacionInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface MarcacionInboxRepository extends JpaRepository<MarcacionInbox, UUID> {
    // JpaRepository ya incluye por defecto la verificación de existencia por ID,
    // lo cual usaremos para la idempotencia (.existsById)
}