package com.epesa.demo.service;

import com.epesa.demo.dto.EmpleadoRequestDto;
import com.epesa.demo.dto.EmpleadoResponseDto;
import com.epesa.demo.dto.RegistroObreroRequest;
import com.epesa.demo.exception.BusinessRuleException;
import com.epesa.demo.exception.ResourceNotFoundException;
import com.epesa.demo.model.Empleado;
import com.epesa.demo.model.enums.EstadoAprobacion;
import com.epesa.demo.model.enums.Rol;
import com.epesa.demo.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class EmpleadoService {
    private final EmpleadoRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public EmpleadoResponseDto crear(EmpleadoRequestDto request) {
        validarGestionSoporte(request.rol(), null);
        validarUnicidad(request.correo(), request.documentoIdentidad(), null);
        if (request.password() == null || request.password().isBlank())
            throw new BusinessRuleException("La contraseÃ±a es obligatoria");
        Empleado empleado = Empleado.builder().nombreCompleto(request.nombreCompleto().trim())
                .correo(request.correo().trim().toLowerCase(Locale.ROOT))
                .documentoIdentidad(request.documentoIdentidad().trim()).rol(request.rol())
                .tipoContrato(request.tipoContrato()).password(passwordEncoder.encode(request.password()))
                .activo(request.activo() == null || request.activo())
                .estadoAprobacion(request.rol() == Rol.OPERATIVO ? EstadoAprobacion.PENDIENTE : EstadoAprobacion.APROBADO)
                .build();
        empleado = repository.save(empleado);
        auditService.record("CREATE", "EMPLEADO", empleado.getId(), empleado.getCorreo());
        return toDto(empleado);
    }

    @Transactional
    public EmpleadoResponseDto actualizar(UUID id, EmpleadoRequestDto request) {
        Empleado empleado = obtenerEntidad(id);
        validarGestionSoporte(request.rol(), empleado);
        validarUnicidad(request.correo(), request.documentoIdentidad(), id);
        empleado.setNombreCompleto(request.nombreCompleto().trim());
        empleado.setCorreo(request.correo().trim().toLowerCase(Locale.ROOT));
        empleado.setDocumentoIdentidad(request.documentoIdentidad().trim());
        empleado.setRol(request.rol()); empleado.setTipoContrato(request.tipoContrato());
        if (request.password() != null && !request.password().isBlank()) empleado.setPassword(passwordEncoder.encode(request.password()));
        if (request.activo() != null) empleado.setActivo(request.activo());
        auditService.record("UPDATE", "EMPLEADO", id, empleado.getCorreo());
        return toDto(repository.save(empleado));
    }

    @Transactional
    public EmpleadoResponseDto cambiarEstado(UUID id, boolean activo) {
        Empleado empleado = obtenerEntidad(id); empleado.setActivo(activo);
        validarGestionSoporte(empleado.getRol(), empleado);
        auditService.record(activo ? "ACTIVATE" : "DEACTIVATE", "EMPLEADO", id, empleado.getCorreo());
        return toDto(repository.save(empleado));
    }

    @Transactional
    public EmpleadoResponseDto resolverAprobacion(UUID id, boolean aprobar) {
        Empleado empleado = obtenerEntidad(id);
        empleado.setEstadoAprobacion(aprobar ? EstadoAprobacion.APROBADO : EstadoAprobacion.RECHAZADO);
        auditService.record(aprobar ? "APPROVE" : "REJECT", "EMPLEADO", id, "Acceso mÃ³vil");
        return toDto(repository.save(empleado));
    }

    @Transactional(readOnly = true) public List<EmpleadoResponseDto> listarTodos() { return repository.findAll().stream().map(this::toDto).toList(); }
    @Transactional(readOnly = true) public EmpleadoResponseDto obtener(UUID id) { return toDto(obtenerEntidad(id)); }
    public Empleado obtenerEntidad(UUID id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado")); }

    @Transactional
    public Empleado registrarObrero(RegistroObreroRequest request) {
        Empleado empleado = Empleado.builder().nombreCompleto(request.nombreCompleto()).correo(request.correo())
                .documentoIdentidad(request.documentoIdentidad()).tipoContrato(request.tipoContrato())
                .rol(Rol.OPERATIVO).password(request.password()).build();
        validarUnicidad(empleado.getCorreo(), empleado.getDocumentoIdentidad(), null);
        empleado.setPassword(passwordEncoder.encode(empleado.getPassword())); empleado.setActivo(true);
        empleado.setEstadoAprobacion(EstadoAprobacion.PENDIENTE);
        return repository.save(empleado);
    }

    private void validarUnicidad(String correo, String documento, UUID id) {
        boolean correoExiste = id == null ? repository.existsByCorreoIgnoreCase(correo) : repository.existsByCorreoIgnoreCaseAndIdNot(correo, id);
        boolean documentoExiste = id == null ? repository.existsByDocumentoIdentidad(documento) : repository.existsByDocumentoIdentidadAndIdNot(documento, id);
        if (correoExiste) throw new BusinessRuleException("El correo ya estÃ¡ registrado");
        if (documentoExiste) throw new BusinessRuleException("El documento ya estÃ¡ registrado");
    }
    private void validarGestionSoporte(Rol rolSolicitado, Empleado existente) {
        boolean involucraSoporte = rolSolicitado == Rol.SOPORTE || (existente != null && existente.getRol() == Rol.SOPORTE);
        if (!involucraSoporte) return;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean actorSoporte = auth != null && auth.getPrincipal() instanceof Empleado actor && actor.getRol() == Rol.SOPORTE;
        boolean primerSoporte = !repository.existsByRol(Rol.SOPORTE);
        if (!actorSoporte && !primerSoporte) throw new BusinessRuleException("Solo SOPORTE puede administrar cuentas SOPORTE");
    }
    private EmpleadoResponseDto toDto(Empleado e) { return new EmpleadoResponseDto(e.getId(), e.getNombreCompleto(), e.getCorreo(), e.getDocumentoIdentidad(), e.getRol(), e.getTipoContrato(), e.getEstadoAprobacion(), e.isActivo()); }
}
