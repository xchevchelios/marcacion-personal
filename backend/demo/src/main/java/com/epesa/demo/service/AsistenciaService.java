package com.epesa.demo.service;

import com.epesa.demo.model.Asistencia;
import com.epesa.demo.repository.AsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AsistenciaService {
    private final AsistenciaRepository asistenciaRepository;

    @Transactional(readOnly = true)
    public List<Asistencia> obtenerTodas() {
        return asistenciaRepository.findAll();
    }
}
