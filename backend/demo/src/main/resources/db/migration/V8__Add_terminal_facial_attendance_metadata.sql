ALTER TABLE asistencias_consolidadas ADD COLUMN metodo_marcacion VARCHAR(40) NOT NULL DEFAULT 'MOVIL';
ALTER TABLE asistencias_consolidadas ADD COLUMN terminal_id VARCHAR(120);
ALTER TABLE asistencias_consolidadas ADD COLUMN confidence_score DOUBLE PRECISION;
ALTER TABLE asistencias_consolidadas ADD COLUMN precision_metros DOUBLE PRECISION;
ALTER TABLE asistencias_consolidadas ADD COLUMN biometric_model VARCHAR(80);
ALTER TABLE asistencias_consolidadas ADD COLUMN padron_version VARCHAR(120);
ALTER TABLE asistencias_consolidadas ADD COLUMN evidencia_bucket VARCHAR(255);
ALTER TABLE asistencias_consolidadas ADD COLUMN evidencia_object_name VARCHAR(500);
ALTER TABLE asistencias_consolidadas ADD COLUMN evidencia_content_type VARCHAR(120);

CREATE INDEX idx_asistencia_terminal ON asistencias_consolidadas(terminal_id, fecha_hora_real DESC);
CREATE INDEX idx_asistencia_metodo ON asistencias_consolidadas(metodo_marcacion, fecha_hora_real DESC);
