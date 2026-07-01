ALTER TABLE empleados
    ADD COLUMN estado_aprobacion VARCHAR(20) NOT NULL DEFAULT 'APROBADO';

ALTER TABLE asignaciones_obra
    ADD COLUMN hora_entrada TIME NOT NULL DEFAULT '08:00:00';
