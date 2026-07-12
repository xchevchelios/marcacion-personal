[CmdletBinding()]
param(
    [string]$ProjectRoot,
    [switch]$OverwriteSources
)

$ErrorActionPreference = "Stop"

if (-not $ProjectRoot) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
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

function Confirm-OverwritePath {
    param([string]$Path)
    if ((Test-Path $Path) -and -not $OverwriteSources) {
        $answer = Read-Host "La ruta '$Path' ya existe. Escriba SI para sobrescribirla"
        if ($answer -ne "SI") {
            throw "Exportación cancelada para evitar sobrescribir '$Path'."
        }
    }
}

function Copy-DirectoryClean {
    param(
        [string]$Source,
        [string]$Destination,
        [string[]]$Exclude = @()
    )

    Confirm-OverwritePath -Path $Destination
    if (Test-Path $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }
    New-Item -ItemType Directory -Path $Destination -Force | Out-Null

    $sourcePath = (Resolve-Path $Source).Path
    $destinationPath = (Resolve-Path $Destination).Path
    $items = Get-ChildItem -LiteralPath $sourcePath -Force
    foreach ($item in $items) {
        if ($Exclude -contains $item.Name) {
            continue
        }
        Copy-Item -LiteralPath $item.FullName -Destination $destinationPath -Recurse -Force
    }
}

Assert-Command docker

$migrationRoot = $PSScriptRoot
$backupRoot = Join-Path $migrationRoot "backups"
$postgresBackupDir = Join-Path $backupRoot "postgres"
$volumeBackupDir = Join-Path $backupRoot "volumes"

New-Item -ItemType Directory -Path $postgresBackupDir -Force | Out-Null
New-Item -ItemType Directory -Path $volumeBackupDir -Force | Out-Null

Write-Step "Copiando fuentes necesarias para reconstruir backend y frontend"
Copy-DirectoryClean -Source (Join-Path $ProjectRoot "backend") -Destination (Join-Path $migrationRoot "backend") -Exclude @(".git", ".idea", ".vscode", "target")
foreach ($backendGeneratedPath in @("backend/demo/.git", "backend/demo/.idea", "backend/demo/.vscode", "backend/demo/target")) {
    $fullPath = Join-Path $migrationRoot $backendGeneratedPath
    if (Test-Path $fullPath) {
        Remove-Item -LiteralPath $fullPath -Recurse -Force
    }
}
Copy-DirectoryClean -Source (Join-Path $ProjectRoot "frontend-svelte") -Destination (Join-Path $migrationRoot "frontend-svelte") -Exclude @(".git", ".idea", ".vscode", "node_modules", "dist")
if (Test-Path (Join-Path $ProjectRoot "simuladores")) {
    Copy-DirectoryClean -Source (Join-Path $ProjectRoot "simuladores") -Destination (Join-Path $migrationRoot "simuladores") -Exclude @(".git", ".idea", ".vscode", ".venv", "venv", "__pycache__")
}

Write-Step "Validando servicios del compose de migración"
Push-Location $migrationRoot
try {
    docker compose --env-file .env.example config --services | Out-Null
}
finally {
    Pop-Location
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$pgDumpPath = Join-Path $postgresBackupDir "postgres-$timestamp.dumpall.sql"
$minioTarPath = Join-Path $volumeBackupDir "minio_data-$timestamp.tar.gz"

Write-Step "Generando respaldo lógico de PostgreSQL con pg_dumpall"
Push-Location $ProjectRoot
try {
    $postgresRunning = docker compose ps --status running --services | Where-Object { $_ -eq "postgres" }
    if (-not $postgresRunning) {
        Write-Host "PostgreSQL no está corriendo. Se levantará solo el servicio postgres para poder generar el dump."
        docker compose up -d postgres | Out-Null
    }

    $postgresUser = "postgres"
    $envFile = Join-Path $ProjectRoot ".env"
    if (Test-Path $envFile) {
        $envLines = Get-Content -LiteralPath $envFile | Where-Object { $_ -match "^\s*POSTGRES_USER\s*=" }
        if ($envLines.Count -gt 0) {
            $postgresUser = ($envLines[-1] -replace "^\s*POSTGRES_USER\s*=\s*", "").Trim()
        }
    }

    $dump = docker compose exec -T postgres pg_dumpall -U $postgresUser
    Set-Content -LiteralPath $pgDumpPath -Value $dump -Encoding UTF8
}
finally {
    Pop-Location
}

Write-Step "Respaldando volumen persistente de MinIO"
docker run --rm -v minio_data_directo:/data:ro -v "${volumeBackupDir}:/backup" alpine sh -c "cd /data && tar -czf /backup/$(Split-Path $minioTarPath -Leaf) ."

Write-Step "Exportación completada"
Write-Host "Paquete: $migrationRoot"
Write-Host "Dump PostgreSQL: $pgDumpPath"
Write-Host "Backup MinIO: $minioTarPath"
Write-Host "No se exportó .env real. Cree docker-migration\\.env desde .env.example en destino."
