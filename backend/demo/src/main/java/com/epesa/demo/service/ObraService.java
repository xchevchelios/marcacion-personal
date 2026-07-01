package com.epesa.demo.service;

import com.epesa.demo.dto.ObraRequestDto;
import com.epesa.demo.dto.ObraResponseDto;
import com.epesa.demo.dto.ObraDetailDto;
import com.epesa.demo.dto.ValidacionEspacialResult;
import com.epesa.demo.model.Obra;
import com.epesa.demo.repository.ObraRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ObraService {

    private final ObraRepository obraRepository;
    private final GeocercaService geocercaService;

    public ObraResponseDto crearObra(ObraRequestDto request) {
        Polygon poligono = geocercaService.crearPoligono(request.getVertices());
        
        Obra nuevaObra = Obra.builder()
                .nombre(request.getNombre())
                .ubicacion(request.getUbicacion())
                .descripcion(request.getDescripcion())
                .activa(request.getActiva() != null ? request.getActiva() : true)
                .areaGeocerca(poligono)
                .build();
                
        nuevaObra = obraRepository.save(nuevaObra);
        
        return new ObraResponseDto(
                nuevaObra.getId(),
                nuevaObra.getNombre(),
                nuevaObra.getUbicacion(),
                nuevaObra.getDescripcion(),
                nuevaObra.getActiva()
        );
    }

    public List<ObraResponseDto> listarObras() {
        return obraRepository.findAll().stream()
                .map(obra -> new ObraResponseDto(
                        obra.getId(),
                        obra.getNombre(),
                        obra.getUbicacion(),
                        obra.getDescripcion(),
                        obra.getActiva()
                ))
                .collect(Collectors.toList());
    }

    public ObraDetailDto obtenerObraDetalle(UUID id) {
        Obra obra = obraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        return new ObraDetailDto(
                obra.getId(),
                obra.getNombre(),
                obra.getUbicacion(),
                obra.getDescripcion(),
                obra.getActiva(),
                geocercaService.extraerVertices(obra.getAreaGeocerca())
        );
    }

    public ObraResponseDto actualizarObra(UUID id, ObraRequestDto request) {
        Obra obra = obraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        Polygon poligono = geocercaService.crearPoligono(request.getVertices());
        obra.setNombre(request.getNombre());
        obra.setUbicacion(request.getUbicacion());
        obra.setDescripcion(request.getDescripcion());
        obra.setActiva(request.getActiva() != null ? request.getActiva() : true);
        obra.setAreaGeocerca(poligono);

        obra = obraRepository.save(obra);
        return new ObraResponseDto(
                obra.getId(),
                obra.getNombre(),
                obra.getUbicacion(),
                obra.getDescripcion(),
                obra.getActiva()
        );
    }

    public void eliminarObra(UUID id) {
        obraRepository.deleteById(id);
    }

    public ValidacionEspacialResult validarUbicacion(UUID obraId, Double lat, Double lng) {
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));
        
        boolean dentro = geocercaService.puntoDentroDePoligono(lat, lng, obra.getAreaGeocerca());
        return new ValidacionEspacialResult(dentro, dentro ? "Punto dentro de la geocerca" : "Punto fuera del área permitida");
    }

    public boolean verificarAsignacionActiva(UUID empleadoId, UUID obraId, LocalDateTime timestampDispositivo) {
        return false;
    }
}