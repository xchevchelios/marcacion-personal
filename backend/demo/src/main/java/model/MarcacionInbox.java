package model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marcaciones_inbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcacionInbox {
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "timestamp_dispositivo", nullable = false)
    private LocalDateTime timestampDispositivo;

    @Column(name = "payload_raw", columnDefinition = "TEXT", nullable = false)
    private String payloadRaw; //JSON original del dispositivo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEvento estado; //PENDIENTE, PROCESADO, ERROR

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion; //FECHA Y HORA EN QUE EL SERVICIO RECIBIÓ EL EVENTO


    @Column(name = "mensaje_error")
    private String mensajeError;

    public enum EstadoEvento {
        PENDING, PROCESSED, ERROR
    }
}

