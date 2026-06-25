import requests
import json
import time
import uuid
from datetime import datetime

BASE_URL = "http://localhost:8080/api/v1"

def print_step(title):
    print(f"\n{'-'*50}\n▶ {title}\n{'-'*50}")

def main():
    print_step("1. Inicio de Sesión como RRHH")
    # Simulamos el panel web haciendo login
    res_login = requests.post(f"{BASE_URL}/auth/login", json={
        "correo": "rrhh@epesa.com",
        "password": "admin123"
    })
    
    if res_login.status_code != 200:
        print("❌ Fallo el login. Revisa que el backend esté corriendo y el DatabaseSeeder haya funcionado.")
        return

    token_rrhh = res_login.json()["token"]
    headers_rrhh = {"Authorization": f"Bearer {token_rrhh}", "Content-Type": "application/json"}
    print("✅ Login exitoso. Token RRHH obtenido.")

    print_step("2. Obtener el ID del Obrero (Creado por el Seeder)")
    res_empleados = requests.get(f"{BASE_URL}/admin/empleados", headers=headers_rrhh)
    
    if res_empleados.status_code != 200:
        print(f"❌ Error en el servidor (Código {res_empleados.status_code}): {res_empleados.text}")
        return
        
    empleados = res_empleados.json()
    obrero_id = next(emp["id"] for emp in empleados if emp["correo"] == "obrero@epesa.com")
    print(f"✅ ID de Juan Perez (Obrero): {obrero_id}")

    print_step("3. Crear la Geocerca (Obra Campus FPUNA)")
    obra_payload = {
        "nombre": "Obra Campus Principal FPUNA",
        "vertices": [
            {"lat": -25.3320, "lng": -57.5220},
            {"lat": -25.3320, "lng": -57.5180},
            {"lat": -25.3360, "lng": -57.5180},
            {"lat": -25.3360, "lng": -57.5220}
        ]
    }
    res_obra = requests.post(f"{BASE_URL}/obras", json=obra_payload, headers=headers_rrhh)
    obra_id = res_obra.json()["id"]
    print(f"✅ Obra creada con ID: {obra_id}")

    print_step("4. Asignar el Obrero a la Obra")
    asignacion_payload = {
        "empleadoId": obrero_id,
        "obraId": obra_id,
        "fechaInicio": datetime.now().strftime("%Y-%m-%d")
    }
    res_asignacion = requests.post(f"{BASE_URL}/asignaciones", json=asignacion_payload, headers=headers_rrhh)
    print("✅ Obrero asignado a la obra exitosamente.")

    print_step("5. Enrolar Dispositivo Móvil (Zero Trust)")
    dispositivo_payload = {
        "deviceId": "TEL-SIMULADOR-01",
        "firmaHardware": "firma-criptografica-x1y2",
        "descripcion": "Celular de Juan Perez"
    }
    requests.post(f"{BASE_URL}/dispositivos/enrolar", json=dispositivo_payload, headers=headers_rrhh)
    print("✅ Dispositivo enrolado y autorizado en la base de datos.")

    print_step("6. SIMULACIÓN DE APP MÓVIL: Sincronizando Marcación Offline")
    # El móvil de Juan envía la marcación. Cae justo en el centro del polígono (-25.3340, -57.5200)
    
    # El payloadRaw es un JSON convertido a String, tal como lo definimos en la arquitectura
    payload_raw = json.dumps({
        "empleadoId": obrero_id,
        "obraId": obra_id,
        "lat": -25.3340,
        "lng": -57.5200,
        "firma_hardware": "firma-criptografica-x1y2"
    })

    sync_payload = {
        "marcaciones": [
            {
                "eventId": str(uuid.uuid4()),
                "deviceId": "TEL-SIMULADOR-01",
                "timestampDispositivo": datetime.now().isoformat(),
                "payloadRaw": payload_raw
            }
        ]
    }
    
    # Usamos el token de RRHH por simplicidad, pero en producción esto usaría el token del Obrero
    res_sync = requests.post(f"{BASE_URL}/sync/marcaciones", json=sync_payload, headers=headers_rrhh)
    print("✅ Marcación enviada al Inbox (Estado: PENDING).")

    print_step("7. Esperando al Orquestador Asíncrono...")
    print("⏳ Esperando 12 segundos para que el @Scheduled de Spring Boot procese el Inbox...")
    time.sleep(12)

    print_step("8. Verificando Resultado en la Tabla de Asistencias")
    # RRHH revisa el dashboard para ver si la marcación pasó los filtros
    res_asistencias = requests.get(f"{BASE_URL}/asistencias", headers=headers_rrhh)
    
    asistencias = res_asistencias.json()
    if len(asistencias) > 0:
        asistencia = asistencias[-1]
        print("🎉 ¡ÉXITO! La marcación fue procesada y validada.")
        print(f"   - Empleado ID: {asistencia.get('empleadoId')}")
        print(f"   - Obra ID: {asistencia.get('obraId')}")
        print(f"   - Requiere Revisión (Flag Soft Fail): {asistencia.get('requiereRevision')}")
        if asistencia.get('requiereRevision'):
            print(f"   - Motivo Revisión: {asistencia.get('motivoRevision')}")
    else:
        print("❌ No se encontraron asistencias procesadas. Revisa los logs de Spring Boot para ver qué filtro la rechazó.")

if __name__ == "__main__":
    main()