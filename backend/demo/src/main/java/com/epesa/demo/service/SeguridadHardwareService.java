package com.epesa.demo.service;

import com.epesa.demo.dto.RegistroDispositivoDto;
import com.epesa.demo.model.Dispositivo;
import com.epesa.demo.repository.DispositivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SeguridadHardwareService {

    private final DispositivoRepository dispositivoRepository;

    // Usado por el Admin web para registrar nuevos teléfonos/tablets
    public Dispositivo enrolarDispositivo(RegistroDispositivoDto dto) {
        if (dispositivoRepository.findByDeviceId(dto.getDeviceId()).isPresent()) {
            throw new RuntimeException("El dispositivo ya está registrado");
        }

        Dispositivo nuevo = Dispositivo.builder()
                .deviceId(dto.getDeviceId())
                .firmaHardware(dto.getFirmaHardware()) // En un entorno real, esto se hashea o usa PKI
                .activo(true)
                .descripcion(dto.getDescripcion())
                .build();

        return dispositivoRepository.save(nuevo);
    }

    // Usado por el Procesador Asíncrono para validar la procedencia de la marcación
    public boolean validarFirmaZeroTrust(String deviceId, String firmaEnviada) {
        Optional<Dispositivo> dispositivoOpt = dispositivoRepository.findByDeviceId(deviceId);

        if (dispositivoOpt.isEmpty()) {
            return false; // Hardware desconocido = Bloqueo inmediato
        }

        Dispositivo dispositivo = dispositivoOpt.get();
        
        if (!dispositivo.isActivo()) {
            return false; // Hardware dado de baja o robado
        }

        // Validación estricta
        return dispositivo.getFirmaHardware().equals(firmaEnviada);
    }
}