package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Polygon;
import java.util.UUID;

@Entity
@Table(name = "obras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    // Columna espacial (SRID 4326 es el estándar GPS WGS84)
    @Column(columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon areaGeocerca; 
}