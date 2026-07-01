package com.epesa.demo.config;

import com.epesa.demo.model.Empleado;
import com.epesa.demo.model.enums.Rol;
import com.epesa.demo.model.enums.TipoContrato;
import com.epesa.demo.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (empleadoRepository.count() == 0) {
            log.info("Base de datos vacía. Sembrando usuarios de prueba...");

            // Usuario Administrador (RRHH)
            Empleado rrhh = Empleado.builder()
                    .correo("rrhh@epesa.com")
                    .password(passwordEncoder.encode("admin123"))
                    .nombreCompleto("Admin Recursos Humanos")
                    .documentoIdentidad("1111111")
                    .tipoContrato(TipoContrato.INTERNO)
                    .rol(Rol.RRHH)
                    .activo(true)
                    .build();
            empleadoRepository.save(rrhh);

            // Usuario Operativo (Obrero en campo)
            Empleado obrero = Empleado.builder()
                    .correo("obrero@epesa.com")
                    .password(passwordEncoder.encode("obrero123"))
                    .nombreCompleto("Juan Perez Obrero")
                    .documentoIdentidad("2222222")
                    .tipoContrato(TipoContrato.TERCERIZADO)
                    .rol(Rol.OPERATIVO)
                    .activo(true)
                    .build();
            empleadoRepository.save(obrero);

            log.info("Usuarios creados exitosamente.");
        }
    }
}
