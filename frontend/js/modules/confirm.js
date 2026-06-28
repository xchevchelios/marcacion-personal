// ============================================================
// EPESA — Módulo: Confirmación de eliminación
// ============================================================

let _pendingDelete = null;

function confirmarEliminar(tipo, id, nombre) {
  _pendingDelete = { tipo, id };

  const msgs = {
    empleado:   `¿Eliminás al empleado <strong>${escStr(nombre)}</strong>?`,
    obra:       `¿Eliminás la obra <strong>${escStr(nombre)}</strong>?`,
    asignacion: `¿Retirás a <strong>${escStr(nombre)}</strong> de la obra asignada?`,
  };

  document.getElementById('confirm-body').innerHTML =
    (msgs[tipo] ?? '¿Confirmar eliminación?') +
    '<br><span class="text-muted">Esta acción no se puede deshacer.</span>';

  openModal('modal-confirm');
}

function configurarModalConfirm() {
  document.getElementById('btn-confirm-ok').addEventListener('click', async () => {
    if (!_pendingDelete) return;

    const { tipo, id } = _pendingDelete;
    const endpoints = {
      empleado:   `/admin/empleados/${id}`,
      obra:       `/admin/obras/${id}`,
      asignacion: `/admin/asignaciones/${id}`,
    };
    const recargar = {
      empleado:   cargarEmpleados,
      obra:       cargarObras,
      asignacion: cargarAsignaciones,
    };

    const btn = document.getElementById('btn-confirm-ok');
    btn.disabled = true;

    try {
      const res = await apiFetch(endpoints[tipo], { method: 'DELETE' });
      if (res && (res.ok || res.status === 204)) {
        closeModal('modal-confirm');
        toast('✓ Eliminado correctamente', 'warning');
        await recargar[tipo]?.();
      } else {
        toast('Error al eliminar. Intentá de nuevo.', 'error');
        closeModal('modal-confirm');
      }
    } catch {
      toast('Error de conexión.', 'error');
    } finally {
      btn.disabled   = false;
      _pendingDelete = null;
    }
  });
}
