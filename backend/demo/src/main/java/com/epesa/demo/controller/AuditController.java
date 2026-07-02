package com.epesa.demo.controller;

import com.epesa.demo.model.AuditEvent;
import com.epesa.demo.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/auditoria") @RequiredArgsConstructor
public class AuditController {
    private final AuditEventRepository repository;
    @GetMapping
    public Page<AuditEvent> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "50") int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200)));
    }
}
