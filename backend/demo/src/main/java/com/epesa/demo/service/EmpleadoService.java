package com.epesa.demo.service;

import com.epesa.demo.model.Empleado;
import com.epesa.demo.repository.EmpleadoRepository;
import com.epesa.demo.dto.RegistroObreroRequest;
import com.epesa.demo.model.enums.EstadoAprobacion;
import com.epesa.demo.model.enums.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Empleado registrarEmpleado(Empleado empleado) {
        // Encriptar la contraseña con BCrypt antes de guardar
        empleado.setPassword(passwordEncoder.encode(empleado.getPassword()));
        empleado.setActivo(true);
        empleado.setEstadoAprobacion(empleado.getRol() == Rol.OPERATIVO
                ? EstadoAprobacion.PENDIENTE : EstadoAprobacion.APROBADO);
        return empleadoRepository.save(empleado);
    }

    @Transactional
    public Empleado registrarObrero(RegistroObreroRequest request) {
        Empleado empleado = Empleado.builder()
                .nombreCompleto(request.nombreCompleto())
                .correo(request.correo())
                .documentoIdentidad(request.documentoIdentidad())
                .tipoContrato(request.tipoContrato())
                .rol(Rol.OPERATIVO)
                .password(request.password())
                .build();
        return registrarEmpleado(empleado);
    }

    @Transactional
    public Empleado resolverAprobacion(UUID id, boolean aprobar) {
        Empleado empleado = obtenerPorId(id);
        empleado.setEstadoAprobacion(aprobar ? EstadoAprobacion.APROBADO : EstadoAprobacion.RECHAZADO);
        return empleadoRepository.save(empleado);
    }

    public List<Empleado> listarTodos() {
        return empleadoRepository.findAll();
    }

    public Empleado obtenerPorId(UUID id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
    }
}
