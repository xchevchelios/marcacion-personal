package com.epesa.demo.service;

import com.epesa.demo.model.AuditEvent;
import com.epesa.demo.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service @RequiredArgsConstructor
public class AuditService {
    private final AuditEventRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, Object entityId, String details) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null && auth.isAuthenticated() ? auth.getName() : "system";
        repository.save(AuditEvent.builder().actor(actor).action(action).entityType(entityType)
                .entityId(String.valueOf(entityId)).details(details).createdAt(Instant.now()).build());
    }
}
