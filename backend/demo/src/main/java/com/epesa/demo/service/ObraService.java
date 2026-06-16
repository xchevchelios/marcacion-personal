package com.epesa.demo.service;

import com.epesa.demo.dto.ObraRequestDto;
import com.epesa.demo.dto.ObraResponseDto;
import com.epesa.demo.dto.ValidacionEspacialResult;
import com.epesa.demo.model.Obra;
import com.epesa.demo.repository.ObraRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObraService {

    private final ObraRepository obraRepository;
    private final GeocercaService geocercaService;

    public ObraResponseDto crearObra(ObraRequestDto request) {
        Polygon poligono = geocercaService.crearPoligono(request.getVertices());
        
        Obra nuevaObra = Obra.builder()
                .nombre(request.getNombre())
                .areaGeocerca(poligono)
                .build();
                
        nuevaObra = obraRepository.save(nuevaObra); // Guardamos y capturamos la entidad con el UUID generado
        
        return new ObraResponseDto(nuevaObra.getId(), nuevaObra.getNombre());
    }

    public ValidacionEspacialResult validarUbicacion(UUID obraId, Double lat, Double lng) {
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));
        
        boolean dentro = geocercaService.puntoDentroDePoligono(lat, lng, obra.getAreaGeocerca());
        return new ValidacionEspacialResult(dentro, dentro ? "Punto dentro de la geocerca" : "Punto fuera del área permitida");
    }
}