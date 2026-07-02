package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events", indexes = @Index(name = "idx_audit_created_at", columnList = "created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String actor;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String entityType;
    @Column(nullable = false) private String entityId;
    @Column(length = 2000) private String details;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
}
