package com.epesa.demo.repository;

import com.epesa.demo.model.MarcacionInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MarcacionInboxRepository extends JpaRepository<MarcacionInbox, UUID> {}