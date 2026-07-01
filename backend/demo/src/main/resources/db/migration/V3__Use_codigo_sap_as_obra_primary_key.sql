-- El código SAP pasa a ser la identidad estable y visible de una obra.
-- Los UUID existentes se conservan como códigos LEGACY para no perder relaciones.
ALTER TABLE obras ADD COLUMN codigo_sap VARCHAR(100);

UPDATE obras
SET codigo_sap = 'LEGACY-' || UPPER(REPLACE(id::text, '-', ''))
WHERE codigo_sap IS NULL;

ALTER TABLE asignaciones_obra
    ALTER COLUMN obra_id TYPE VARCHAR(100)
    USING 'LEGACY-' || UPPER(REPLACE(obra_id::text, '-', ''));

ALTER TABLE asistencias_consolidadas
    ALTER COLUMN obra_id TYPE VARCHAR(100)
    USING 'LEGACY-' || UPPER(REPLACE(obra_id::text, '-', ''));

ALTER TABLE obras DROP CONSTRAINT obras_pkey;
ALTER TABLE obras DROP COLUMN id;
ALTER TABLE obras ALTER COLUMN codigo_sap SET NOT NULL;
ALTER TABLE obras ADD CONSTRAINT obras_pkey PRIMARY KEY (codigo_sap);
