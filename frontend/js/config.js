// ============================================================
// EPESA — Configuración global
// ============================================================

const API_BASE_URL = 'http://localhost:8080/api/v1';

// Roles reconocidos por el sistema
const ROLES = {
  RRHH:          'RRHH',
  ADMIN:         'ADMIN',
  JEFE_OBRA:     'JEFE_OBRA',
  RESIDENTE:     'RESIDENTE',
};

// Opciones disponibles para el selector de rol en el formulario de empleados
const ROL_OPTIONS = [
  { value: 'RRHH',      label: 'RRHH' },
  { value: 'ADMIN',     label: 'Admin' },
  { value: 'JEFE_OBRA', label: 'Jefe de Obra' },
  { value: 'RESIDENTE', label: 'Residente' },
];

// Opciones de tipo de contrato
const CONTRATO_OPTIONS = [
  { value: 'PERMANENTE',  label: 'Permanente' },
  { value: 'TEMPORAL',    label: 'Temporal' },
  { value: 'CONTRATADO',  label: 'Contratado' },
  { value: 'PASANTE',     label: 'Pasante' },
];
