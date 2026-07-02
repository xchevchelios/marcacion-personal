package com.epesa.demo.model.enums;

public enum Rol {
    SOPORTE,        // Control total del sistema
    ADMIN,          // AdministraciÃ³n general
    RRHH,           // GestiÃ³n de personal y marcaciones
    JEFE_OBRA,      // Control sobre sus obras asignadas
    RESIDENTE,      // Equivalente a Jefe de Obra en permisos
    OPERATIVO       // Solo puede marcar asistencia
}
