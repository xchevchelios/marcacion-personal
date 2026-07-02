ALTER TABLE asistencias_consolidadas ADD COLUMN estado_revision VARCHAR(20) NOT NULL DEFAULT 'SIN_REVISION';
ALTER TABLE asistencias_consolidadas ADD COLUMN nota_resolucion VARCHAR(1000);
ALTER TABLE asistencias_consolidadas ADD COLUMN resuelto_por VARCHAR(255);
ALTER TABLE asistencias_consolidadas ADD COLUMN fecha_resolucion TIMESTAMP;
UPDATE asistencias_consolidadas SET estado_revision = 'PENDIENTE' WHERE requiere_revision = TRUE;
CREATE INDEX idx_asistencia_revision ON asistencias_consolidadas(estado_revision, fecha_resolucion DESC);
