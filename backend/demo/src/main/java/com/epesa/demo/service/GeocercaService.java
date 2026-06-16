package com.epesa.demo.service;

import com.epesa.demo.dto.CoordenadaDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GeocercaService {
    
    // SRID 4326 para coordenadas terrestres reales
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public Polygon crearPoligono(List<CoordenadaDto> vertices) {
        Coordinate[] coords = new Coordinate[vertices.size() + 1];
        
        for (int i = 0; i < vertices.size(); i++) {
            // Importante: JTS usa el formato (X, Y) -> (Longitud, Latitud)
            coords[i] = new Coordinate(vertices.get(i).getLng(), vertices.get(i).getLat()); 
        }
        
        // PostGIS requiere que el polígono esté cerrado (el último punto = el primer punto)
        coords[vertices.size()] = coords[0];
        
        return geometryFactory.createPolygon(coords);
    }

    public boolean puntoDentroDePoligono(Double lat, Double lng, Polygon poligono) {
        Point puntoMarcacion = geometryFactory.createPoint(new Coordinate(lng, lat));
        return poligono.contains(puntoMarcacion);
    }
}