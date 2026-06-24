package com.epesa.demo.service;

import com.epesa.demo.model.Empleado;
import com.epesa.demo.repository.EmpleadoRepository;
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