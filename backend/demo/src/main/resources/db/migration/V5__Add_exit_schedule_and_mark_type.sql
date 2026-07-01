ALTER TABLE asignaciones_obra
    ADD COLUMN hora_salida TIME NOT NULL DEFAULT '17:00:00';

ALTER TABLE asistencias_consolidadas
    ADD COLUMN tipo_marcacion VARCHAR(20) NOT NULL DEFAULT 'ENTRADA';
