package com.epesa.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "dispositivos_confianza")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispositivo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Identificador único que envía Flutter (ej: OBRA-SUR-01)
    @Column(unique = true, nullable = false)
    private String deviceId;

    // Puede ser un IMEI, MAC, o clave pública criptográfica
    @Column(nullable = false)
    private String firmaHardware; 

    @Column(nullable = false)
    private boolean activo;

    private String descripcion;
}
