package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "obras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Obra {
    @Id
    @Column(name = "codigo_sap", nullable = false, length = 100)
    private String codigoSap;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String ubicacion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    // Columna espacial (SRID 4326 es el estándar GPS WGS84)
    @Column(columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon areaGeocerca; 
}
