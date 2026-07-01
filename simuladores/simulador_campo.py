"""Simulador configurable del flujo RRHH -> app móvil -> marcación."""

import json
import time
from datetime import datetime
import requests

# ============================ CONFIGURACIÓN ============================
BASE_URL = "http://localhost:8080/api/v1"
CORREO_RRHH, PASSWORD_RRHH = "rrhh@epesa.com", "admin123"
CORREO_OBRERO, PASSWORD_OBRERO = "obrero@epesa.com", "obrero123"

CODIGO_SAP_OBRA = "OBRA-2026-001"
NOMBRE_OBRA = "Obra SDS"
UBICACION_OBRA = "Campus FPUNA"
DESCRIPCION_OBRA = "Obra utilizada por el simulador"
HORA_ENTRADA = "08:00"  # Cambia este valor para probar horario/tolerancia de entrada.
HORA_SALIDA = "17:00"   # Cambia este valor para probar salida tardía.
TIPO_MARCACION = "ENTRADA"  # "ENTRADA", "SALIDA" o None para que el backend lo infiera.
VERTICES_GEOCERCA = [
    {"lat": -25.3320, "lng": -57.5220},
    {"lat": -25.3320, "lng": -57.5180},
    {"lat": -25.3360, "lng": -57.5180},
    {"lat": -25.3360, "lng": -57.5220},
]
LATITUD_MARCACION, LONGITUD_MARCACION = -25.3340, -57.5200
METADATO_DISPOSITIVO = "Android simulado | modelo Pixel | id TEL-SIM-01"
ESPERA_PROCESAMIENTO_SEGUNDOS = 12
TIMEOUT_HTTP_SEGUNDOS = 15
# ======================================================================


def paso(texto):
    print(f"\n{'-' * 64}\n{texto}\n{'-' * 64}")


def api(metodo, ruta, token=None, cuerpo=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    respuesta = requests.request(metodo, BASE_URL + ruta, headers=headers,
                                  json=cuerpo, timeout=TIMEOUT_HTTP_SEGUNDOS)
    if respuesta.status_code != 200:
        raise RuntimeError(f"{metodo} {ruta}: {respuesta.status_code} {respuesta.text}")
    return respuesta.json() if respuesta.content else None


def login(correo, password):
    return api("POST", "/auth/login", cuerpo={"correo": correo, "password": password})


def main():
    paso("1. RRHH prepara la obra y la asignación")
    sesion_rrhh = login(CORREO_RRHH, PASSWORD_RRHH)
    token_rrhh = sesion_rrhh["token"]
    empleados = api("GET", "/admin/empleados", token_rrhh)
    obrero = next((e for e in empleados if e["correo"] == CORREO_OBRERO), None)
    if not obrero:
        raise RuntimeError(f"No existe el obrero {CORREO_OBRERO}")

    # El simulador aprueba al obrero si sigue pendiente. Para probar el estado
    # pendiente, comenta estas tres líneas y resuélvelo desde el frontend RRHH.
    if obrero.get("estadoAprobacion") == "PENDIENTE":
        api("PATCH", f"/admin/empleados/{obrero['id']}/aprobacion?aprobar=true", token_rrhh)

    obra = {
        "codigoSap": CODIGO_SAP_OBRA, "nombre": NOMBRE_OBRA,
        "ubicacion": UBICACION_OBRA, "descripcion": DESCRIPCION_OBRA,
        "activa": True, "vertices": VERTICES_GEOCERCA,
    }
    obras = api("GET", "/admin/obras", token_rrhh)
    if any(o["codigoSap"] == CODIGO_SAP_OBRA for o in obras):
        api("PUT", f"/admin/obras/{CODIGO_SAP_OBRA}", token_rrhh, obra)
    else:
        api("POST", "/admin/obras", token_rrhh, obra)

    asignaciones = api("GET", "/admin/asignaciones", token_rrhh)
    existe = any(a["empleadoId"] == obrero["id"] and a["obraId"] == CODIGO_SAP_OBRA
                 for a in asignaciones)
    if not existe:
        api("POST", "/admin/asignaciones", token_rrhh, {
            "empleadoId": obrero["id"], "obraId": CODIGO_SAP_OBRA,
            "fechaInicio": datetime.now().strftime("%Y-%m-%d"),
            "horaEntrada": HORA_ENTRADA,
            "horaSalida": HORA_SALIDA,
        })

    paso("2. El obrero inicia sesión y consulta sus obras")
    sesion_obrero = login(CORREO_OBRERO, PASSWORD_OBRERO)
    print("Estado de aprobación:", sesion_obrero["estadoAprobacion"])
    token_obrero = sesion_obrero["token"]
    mis_obras = api("GET", "/mobile/mis-obras", token_obrero)
    print(json.dumps(mis_obras, indent=2, ensure_ascii=False))

    paso("3. El obrero selecciona la obra e intenta marcar")
    resultado = api("POST", "/mobile/marcaciones", token_obrero, {
        "codigoSap": CODIGO_SAP_OBRA,
        "latitud": LATITUD_MARCACION,
        "longitud": LONGITUD_MARCACION,
        "dispositivo": METADATO_DISPOSITIVO,
        "tipoMarcacion": TIPO_MARCACION,
    })
    print(resultado["mensaje"])
    if not resultado["aceptada"]:
        return

    paso("4. RRHH consulta el resultado procesado")
    time.sleep(ESPERA_PROCESAMIENTO_SEGUNDOS)
    asistencias = api("GET", "/admin/dashboard/asistencias", token_rrhh)
    evento = next((a for a in asistencias if str(a.get("eventId")) == resultado["eventId"]), None)
    print(json.dumps(evento, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    try:
        main()
    except (requests.RequestException, RuntimeError, StopIteration) as error:
        print("ERROR:", error)
        raise SystemExit(1)
