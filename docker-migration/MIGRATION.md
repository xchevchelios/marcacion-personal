# Migración del stack Docker

Este paquete migra el stack a otra computadora sin copiar directamente el volumen activo de PostgreSQL.

## Arquitectura detectada

Servicios:

- `postgres`: base de datos PostgreSQL con PostGIS, imagen `postgis/postgis:15-3.4`.
- `minio`: almacenamiento de evidencias, imagen `quay.io/minio/minio:latest`.
- `backend`: aplicación Spring Boot construida localmente desde `backend/demo`.
- `frontend`: aplicación Svelte/Nginx construida localmente desde `frontend-svelte`.

Dependencias:

- `backend` depende de `postgres` y `minio` saludables.
- `frontend` depende de `backend` saludable.

Volúmenes Docker:

- `pg_data_directo`: datos de PostgreSQL. Se migra con respaldo lógico, no copiando el volumen.
- `minio_data_directo`: datos de MinIO. Se migra como archivo `tar.gz`.

Bind mounts:

- No hay bind mounts de datos persistentes definidos en el compose actual.

Imágenes construidas localmente:

- `backend`, contexto `./backend/demo`.
- `frontend`, contexto `./frontend-svelte`.

Variables de entorno:

- PostgreSQL: usuario, contraseña y base.
- MinIO: usuario, contraseña, claves y bucket.
- Backend: URL/credenciales de base, MinIO, `JWT_SECRET`, CORS y flags de procesamiento.

Los secretos no se guardan en Git. En destino se debe crear `.env` desde `.env.example`.

## Exportar en la computadora origen

Desde PowerShell:

```powershell
cd C:\Users\Usuario\sistema-marcacion\docker-migration
.\export.ps1
```

El script:

1. Copia las fuentes necesarias para reconstruir backend y frontend.
2. Valida el compose de migración.
3. Genera un respaldo lógico de PostgreSQL con `pg_dumpall`.
4. Respalda el volumen de MinIO como `tar.gz`.

Si PostgreSQL no está corriendo, el script levanta solo el servicio `postgres` para poder exportar. No detiene ni elimina contenedores.

Archivos generados:

- `backups/postgres/postgres-*.dumpall.sql`
- `backups/volumes/minio_data-*.tar.gz`
- Copia de `backend/`
- Copia de `frontend-svelte/`
- Copia de `simuladores/`, si existe

No se copia el `.env` real.

## Llevar a la computadora destino

Copie la carpeta completa `docker-migration` a la nueva computadora.

Requisitos:

- Docker Desktop instalado y funcionando.
- PowerShell.

En la computadora destino:

```powershell
cd ruta\al\docker-migration
Copy-Item .env.example .env
notepad .env
```

Complete los valores reales en `.env`. Use secretos nuevos o los mismos si necesita mantener sesiones/tokens compatibles.

## Importar en la computadora destino

```powershell
cd ruta\al\docker-migration
.\import.ps1
```

El script:

1. Comprueba que Docker y Docker Compose estén disponibles.
2. Verifica que exista `.env`.
3. Crea los volúmenes necesarios.
4. Levanta primero PostgreSQL.
5. Espera a que PostgreSQL esté disponible.
6. Restaura el respaldo lógico.
7. Restaura el volumen de MinIO.
8. Construye y levanta backend y frontend.
9. Muestra el estado final de los contenedores.

Si detecta datos existentes en los volúmenes de destino, pide confirmación explícita antes de continuar.

## Verificación

Después de importar:

```powershell
docker compose ps
```

URLs esperadas:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/actuator/health`
- MinIO Console: `http://localhost:9001`

## Notas importantes

- No elimine el volumen PostgreSQL de origen.
- No copie manualmente `pg_data_directo`; use el SQL generado.
- No suba `.env`, `backups/` ni secretos a Git.
- Si el destino ya tenía datos, haga un respaldo propio antes de ejecutar la importación.
