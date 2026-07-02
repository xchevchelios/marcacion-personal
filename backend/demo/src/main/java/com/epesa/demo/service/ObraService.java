package com.epesa.demo.service;

import com.epesa.demo.dto.ObraDetailDto;
import com.epesa.demo.dto.ObraRequestDto;
import com.epesa.demo.dto.ObraResponseDto;
import com.epesa.demo.dto.ValidacionEspacialResult;
import com.epesa.demo.model.Obra;
import com.epesa.demo.repository.ObraRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ObraService {
    private final ObraRepository obraRepository;
    private final GeocercaService geocercaService;
    private final AuditService auditService;

    @Transactional
    public ObraResponseDto crearObra(ObraRequestDto request) {
        String codigoSap = normalizarCodigo(request.getCodigoSap());
        if (obraRepository.existsById(codigoSap)) {
            throw new IllegalArgumentException("Ya existe una obra con el código SAP " + codigoSap);
        }
        Obra obra = Obra.builder()
                .codigoSap(codigoSap)
                .nombre(request.getNombre())
                .ubicacion(request.getUbicacion())
                .descripcion(request.getDescripcion())
                .activa(request.getActiva() == null || request.getActiva())
                .areaGeocerca(geocercaService.crearPoligono(request.getVertices()))
                .build();
        obra = obraRepository.save(obra);
        auditService.record("CREATE", "OBRA", obra.getCodigoSap(), obra.getNombre());
        return convertir(obra);
    }

    @Transactional(readOnly = true)
    public List<ObraResponseDto> listarObras() {
        return obraRepository.findAll().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public ObraDetailDto obtenerObraDetalle(String codigoSap) {
        Obra obra = buscar(codigoSap);
        return new ObraDetailDto(obra.getCodigoSap(), obra.getNombre(), obra.getUbicacion(),
                obra.getDescripcion(), obra.getActiva(),
                geocercaService.extraerVertices(obra.getAreaGeocerca()));
    }

    @Transactional
    public ObraResponseDto actualizarObra(String codigoSap, ObraRequestDto request) {
        Obra obra = buscar(codigoSap);
        String codigoSolicitado = normalizarCodigo(request.getCodigoSap());
        if (!obra.getCodigoSap().equals(codigoSolicitado)) {
            throw new IllegalArgumentException("El código SAP no se puede modificar");
        }
        Polygon poligono = geocercaService.crearPoligono(request.getVertices());
        obra.setNombre(request.getNombre());
        obra.setUbicacion(request.getUbicacion());
        obra.setDescripcion(request.getDescripcion());
        obra.setActiva(request.getActiva() == null || request.getActiva());
        obra.setAreaGeocerca(poligono);
        obra = obraRepository.save(obra);
        auditService.record("UPDATE", "OBRA", obra.getCodigoSap(), obra.getNombre());
        return convertir(obra);
    }

    @Transactional
    public void eliminarObra(String codigoSap) {
        Obra obra = buscar(codigoSap);
        obra.setActiva(false);
        obraRepository.save(obra);
        auditService.record("DEACTIVATE", "OBRA", obra.getCodigoSap(), obra.getNombre());
    }

    @Transactional(readOnly = true)
    public ValidacionEspacialResult validarUbicacion(String codigoSap, Double lat, Double lng) {
        Obra obra = buscar(codigoSap);
        boolean dentro = geocercaService.puntoDentroDePoligono(lat, lng, obra.getAreaGeocerca());
        return new ValidacionEspacialResult(dentro,
                dentro ? "Punto dentro de la geocerca" : "Punto fuera del área permitida");
    }

    private Obra buscar(String codigoSap) {
        return obraRepository.findById(normalizarCodigo(codigoSap))
                .orElseThrow(() -> new IllegalArgumentException("Obra no encontrada: " + codigoSap));
    }

    private String normalizarCodigo(String codigoSap) {
        return codigoSap.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private ObraResponseDto convertir(Obra obra) {
        return new ObraResponseDto(obra.getCodigoSap(), obra.getNombre(), obra.getUbicacion(),
                obra.getDescripcion(), obra.getActiva());
    }
}
