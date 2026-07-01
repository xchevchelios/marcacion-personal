package com.epesa.demo.service;

import com.epesa.demo.dto.RegistroDispositivoDto;
import com.epesa.demo.model.Dispositivo;
import com.epesa.demo.repository.DispositivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeguridadHardwareService {
    private final DispositivoRepository dispositivoRepository;

    /** Registra un dispositivo nuevo o actualiza y reactiva uno ya enrolado. */
    @Transactional
    public Dispositivo enrolarDispositivo(RegistroDispositivoDto dto) {
        Dispositivo dispositivo = dispositivoRepository.findByDeviceId(dto.getDeviceId())
                .orElseGet(() -> Dispositivo.builder()
                        .deviceId(dto.getDeviceId())
                        .build());
        dispositivo.setFirmaHardware(dto.getFirmaHardware());
        dispositivo.setDescripcion(dto.getDescripcion());
        dispositivo.setActivo(true);
        return dispositivoRepository.save(dispositivo);
    }

    @Transactional(readOnly = true)
    public boolean validarFirmaZeroTrust(String deviceId, String firmaEnviada) {
        return dispositivoRepository.findByDeviceId(deviceId)
                .filter(Dispositivo::isActivo)
                .map(dispositivo -> dispositivo.getFirmaHardware().equals(firmaEnviada))
                .orElse(false);
    }
}
