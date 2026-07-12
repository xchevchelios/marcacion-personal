[CmdletBinding()]
param(
    [string]$MigrationRoot,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if (-not $MigrationRoot) {
    $MigrationRoot = $PSScriptRoot
}

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Assert-Command {
    param([string]$CommandName)
    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "No se encontró '$CommandName'. Instale la herramienta requerida y vuelva a ejecutar."
    }
}

function Confirm-Danger {
    param([string]$Message)
    if ($Force) {
        return
    }
    $answer = Read-Host "$Message Escriba SI para continuar"
    if ($answer -ne "SI") {
        throw "Importación cancelada por seguridad."
    }
}

function Get-EnvValue {
    param(
        [string]$Path,
        [string]$Name,
        [string]$DefaultValue
    )
    if (-not (Test-Path $Path)) {
        return $DefaultValue
    }
    $line = Get-Content -LiteralPath $Path | Where-Object { $_ -match "^\s*$([regex]::Escape($Name))\s*=" } | Select-Object -Last 1
    if (-not $line) {
        return $DefaultValue
    }
    return ($line -replace "^\s*$([regex]::Escape($Name))\s*=\s*", "").Trim()
}

function Ensure-DockerVolume {
    param([string]$Name)
    $exists = docker volume ls --format "{{.Name}}" | Where-Object { $_ -eq $Name }
    if (-not $exists) {
        docker volume create $Name | Out-Null
    }
}

function Test-VolumeHasData {
    param([string]$Name)
    $count = docker run --rm -v "${Name}:/data:ro" alpine sh -c "find /data -mindepth 1 -maxdepth 1 | wc -l"
    return ([int]$count.Trim()) -gt 0
}

Assert-Command docker

Push-Location $MigrationRoot
try {
    if (-not (Test-Path ".env")) {
        throw "Falta el archivo .env. Copie .env.example como .env y complete los secretos en la computadora destino."
    }

    docker version | Out-Null
    docker compose version | Out-Null

    $envPath = Join-Path $MigrationRoot ".env"
    $postgresUser = Get-EnvValue -Path $envPath -Name "POSTGRES_USER" -DefaultValue "postgres"
    $postgresPassword = Get-EnvValue -Path $envPath -Name "POSTGRES_PASSWORD" -DefaultValue ""
    $postgresVolume = Get-EnvValue -Path $envPath -Name "PGDATA_VOLUME" -DefaultValue "pg_data_directo"
    $minioVolume = Get-EnvValue -Path $envPath -Name "MINIO_DATA_VOLUME" -DefaultValue "minio_data_directo"

    $pgDump = Get-ChildItem -LiteralPath (Join-Path $MigrationRoot "backups/postgres") -Filter "*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $pgDump) {
        throw "No se encontró respaldo SQL en backups/postgres."
    }

    $minioBackup = Get-ChildItem -LiteralPath (Join-Path $MigrationRoot "backups/volumes") -Filter "minio_data-*.tar.gz" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

    Write-Step "Creando volúmenes Docker si no existen"
    Ensure-DockerVolume -Name $postgresVolume
    Ensure-DockerVolume -Name $minioVolume

    if (Test-VolumeHasData -Name $postgresVolume) {
        Confirm-Danger "El volumen PostgreSQL '$postgresVolume' ya contiene datos. Restaurar encima puede fallar o mezclar datos."
    }
    if ($minioBackup -and (Test-VolumeHasData -Name $minioVolume)) {
        Confirm-Danger "El volumen MinIO '$minioVolume' ya contiene datos. La restauración agregará/sobrescribirá archivos dentro del volumen."
    }

    Write-Step "Levantando primero PostgreSQL"
    docker compose up -d postgres

    Write-Step "Esperando disponibilidad de PostgreSQL"
    $ready = $false
    for ($i = 1; $i -le 60; $i++) {
        docker compose exec -T postgres pg_isready -U $postgresUser | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) {
        throw "PostgreSQL no estuvo disponible dentro del tiempo esperado."
    }

    Write-Step "Restaurando respaldo lógico de PostgreSQL"
    Get-Content -LiteralPath $pgDump.FullName -Raw | docker compose exec -T postgres psql -U $postgresUser

    if ($postgresPassword) {
        Write-Step "Alineando contraseña PostgreSQL con .env"
        $escapedPostgresPassword = $postgresPassword.Replace("'", "''")
        docker compose exec -T postgres psql -U $postgresUser -d postgres -c "ALTER ROLE ""$postgresUser"" WITH PASSWORD '$escapedPostgresPassword';"
    }

    if ($minioBackup) {
        Write-Step "Restaurando volumen de MinIO"
        docker run --rm -v "${minioVolume}:/data" -v "$($minioBackup.DirectoryName):/backup:ro" alpine sh -c "cd /data && tar -xzf /backup/$($minioBackup.Name)"
    }
    else {
        Write-Host "No se encontró backup de MinIO. Se continuará sin restaurar ese volumen."
    }

    Write-Step "Levantando el resto del stack"
    docker compose up -d --build

    Write-Step "Estado final de contenedores"
    docker compose ps
}
finally {
    Pop-Location
}
