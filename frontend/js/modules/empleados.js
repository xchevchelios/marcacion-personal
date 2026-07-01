// ============================================================
// EPESA — Módulo: Empleados  (solo RRHH/Admin)
// ============================================================

let empListCache = [];
let empAsignacionesCache = [];
let empAsistenciasCache = [];
let empleadoSeleccionadoId = null;

async function cargarEmpleados() {
  if (isLoading('empleados')) return;
  setLoadingKey('empleados', true);

  const tbody = document.getElementById('tbody-empleados');
  tbody.innerHTML = loadingRows(6);

  try {
    const res = await apiFetch('/admin/empleados');
    if (!res) return;

    if (!res.ok) {
      tbody.innerHTML = errorRow(6, 'cargarEmpleados()');
      return;
    }

    const data = await res.json();
    empListCache = Array.isArray(data) ? data : (data.content ?? []);
    tbody.innerHTML = '';

    if (!empListCache.length) {
      tbody.innerHTML = emptyRow(6, 'Sin empleados registrados.');
      return;
    }

    empListCache.forEach((emp, i) => {
      const tr = document.createElement('tr');
      tr.className = 'fade-in';
      tr.style.animationDelay = `${i * 25}ms`;
      tr.title = 'Click para ver la ficha del empleado';
      tr.addEventListener('click', () => mostrarDetalleEmpleado(emp.id));

      tr.innerHTML = `
        <td>
          <div class="cell-avatar">
            <span class="avatar-chip">${initials(emp.nombreCompleto)}</span>
            <span class="fw-medium">${escStr(emp.nombreCompleto) || '—'}</span>
          </div>
        </td>
        <td>${escStr(emp.documentoIdentidad) || '—'}</td>
        <td>${badge(emp.rol, 'blue')}</td>
        <td>${badge(emp.estadoAprobacion || 'PENDIENTE', emp.estadoAprobacion === 'APROBADO' ? 'green' : 'yellow')}</td>
        <td>${activeBadge(emp.activo)}</td>
        <td class="td-actions rrhh-only">
          ${emp.rol === 'OPERATIVO' && emp.estadoAprobacion === 'PENDIENTE' ? `
            <button class="btn-action" data-admin-action onclick="event.stopPropagation(); resolverAprobacionEmpleado('${escStr(emp.id)}', true)">Aprobar</button>
            <button class="btn-icon danger" data-admin-action onclick="event.stopPropagation(); resolverAprobacionEmpleado('${escStr(emp.id)}', false)" title="Rechazar">×</button>
          ` : ''}
          <button class="btn-icon" title="Editar" data-admin-action onclick="event.stopPropagation(); abrirModalEditarEmpleado('${escStr(emp.id)}')">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </button>
          <button class="btn-icon danger" title="Eliminar" data-admin-action onclick="event.stopPropagation(); confirmarEliminar('empleado','${escStr(emp.id)}','${escStr(emp.nombreCompleto)}')">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
              <path d="M10 11v6M14 11v6M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
            </svg>
          </button>
        </td>`;
      tbody.appendChild(tr);
    });

  } catch {
    tbody.innerHTML = errorRow(6, 'cargarEmpleados()');
  } finally {
    setLoadingKey('empleados', false);
    applyRBAC(); // re-aplicar tras renderizar
  }
}

// ── Modal Empleado ─────────────────────────────────────────

async function mostrarDetalleEmpleado(id) {
  const emp = empListCache.find(e => String(e.id) === String(id));
  if (!emp) return;

  empleadoSeleccionadoId = String(id);
  const panel = document.getElementById('empleado-detalle-panel');
  document.getElementById('empleado-marcaciones-panel')?.classList.add('hidden');
  document.getElementById('tbody-empleado-marcaciones').innerHTML = '';
  document.getElementById('empleado-detalle-nombre').textContent = emp.nombreCompleto || 'Empleado sin nombre';
  document.getElementById('empleado-detalle-subtitulo').textContent =
    `${emp.activo ? 'Activo' : 'Inactivo'} · aprobación móvil: ${emp.estadoAprobacion || 'PENDIENTE'}`;
  document.getElementById('empleado-detalle-documento').textContent = emp.documentoIdentidad || '—';
  document.getElementById('empleado-detalle-correo').textContent = emp.correo || '—';
  document.getElementById('empleado-detalle-rol').textContent = emp.rol || '—';
  document.getElementById('empleado-detalle-contrato').textContent = emp.tipoContrato || '—';

  const tbody = document.getElementById('tbody-empleado-obras');
  tbody.innerHTML = loadingRows(5);
  panel.classList.remove('hidden');
  panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

  const asignaciones = await _obtenerAsignacionesEmpleado();
  const obras = asignaciones.filter(a => String(a.empleadoId) === String(id));

  if (!obras.length) {
    tbody.innerHTML = emptyRow(5, 'Este empleado todavía no tiene obras asignadas.');
    return;
  }

  tbody.innerHTML = obras.map(a => `
    <tr>
      <td class="fw-medium">${escStr(a.obraId) || '—'}</td>
      <td>${escStr(a.obraNombre) || '—'}</td>
      <td>${fmtDate(a.fechaInicio)}</td>
      <td>${escStr(a.horaEntrada) || '08:00'}</td>
      <td>${escStr(a.horaSalida) || '17:00'}</td>
    </tr>
  `).join('');
}

function cerrarDetalleEmpleado() {
  document.getElementById('empleado-detalle-panel')?.classList.add('hidden');
  document.getElementById('empleado-marcaciones-panel')?.classList.add('hidden');
  empleadoSeleccionadoId = null;
}

async function abrirFichaMarcacionesEmpleado() {
  if (!empleadoSeleccionadoId) return;

  const panel = document.getElementById('empleado-marcaciones-panel');
  const tbody = document.getElementById('tbody-empleado-marcaciones');
  tbody.innerHTML = loadingRows(6);
  panel.classList.remove('hidden');

  const asistencias = await _obtenerAsistenciasEmpleado();
  const desde = new Date();
  desde.setDate(desde.getDate() - 30);

  const lista = asistencias
    .filter(a => String(a.empleadoId) === String(empleadoSeleccionadoId))
    .filter(a => a.fechaHoraReal && new Date(a.fechaHoraReal) >= desde)
    .sort((a, b) => new Date(b.fechaHoraReal) - new Date(a.fechaHoraReal));

  if (!lista.length) {
    tbody.innerHTML = emptyRow(6, 'Sin marcaciones registradas en los últimos 30 días.');
    return;
  }

  tbody.innerHTML = lista.map(a => `
    <tr>
      <td>${fmtDateTime(a.fechaHoraReal)}</td>
      <td>${badge(a.tipoMarcacion || 'ENTRADA', a.tipoMarcacion === 'SALIDA' ? 'blue' : 'green')}</td>
      <td><span class="fw-medium">${escStr(a.obraId) || '—'}</span> - ${escStr(a.obraNombre) || '—'}</td>
      <td>${a.requiereRevision ? badge('Revisión', 'yellow') : badge('Conforme', 'green')}</td>
      <td class="td-truncate" title="${escStr(a.motivoRevision)}">${escStr(a.motivoRevision) || '—'}</td>
      <td class="td-truncate" title="${escStr(a.deviceId)}">${escStr(a.deviceId) || '—'}</td>
    </tr>
  `).join('');
}

async function _obtenerAsignacionesEmpleado() {
  if (empAsignacionesCache.length) return empAsignacionesCache;
  try {
    const res = await apiFetch('/admin/asignaciones');
    if (!res?.ok) return [];
    const data = await res.json();
    empAsignacionesCache = Array.isArray(data) ? data : (data.content ?? []);
    return empAsignacionesCache;
  } catch {
    return [];
  }
}

async function _obtenerAsistenciasEmpleado() {
  try {
    const res = await apiFetch('/admin/dashboard/asistencias');
    if (!res?.ok) return [];
    const data = await res.json();
    empAsistenciasCache = Array.isArray(data) ? data : (data.content ?? []);
    return empAsistenciasCache;
  } catch {
    return [];
  }
}

function invalidarCacheDetalleEmpleados() {
  empAsignacionesCache = [];
  empAsistenciasCache = [];
  cerrarDetalleEmpleado();
}

function abrirModalNuevoEmpleado() {
  _resetFormEmpleado();
  document.getElementById('emp-modal-titulo').textContent = 'Nuevo Empleado';
  document.getElementById('emp-id').value = '';
  document.getElementById('emp-password-group').classList.remove('hidden');
  openModal('modal-empleado');
}

function abrirModalEditarEmpleado(id) {
  const emp = empListCache.find(e => String(e.id) === String(id));
  if (!emp) return;

  _resetFormEmpleado();
  document.getElementById('emp-modal-titulo').textContent = 'Editar Empleado';
  document.getElementById('emp-id').value                 = emp.id;
  document.getElementById('emp-nombreCompleto').value     = emp.nombreCompleto      ?? '';
  document.getElementById('emp-correo').value             = emp.correo              ?? '';
  document.getElementById('emp-documentoIdentidad').value = emp.documentoIdentidad  ?? '';
  document.getElementById('emp-rol').value                = emp.rol                 ?? '';
  document.getElementById('emp-tipoContrato').value       = emp.tipoContrato        ?? '';
  document.getElementById('emp-activo').checked           = emp.activo              ?? true;
  document.getElementById('emp-password-group').classList.add('hidden');
  openModal('modal-empleado');
}

function _resetFormEmpleado() {
  document.getElementById('form-empleado').reset();
  document.getElementById('emp-error').classList.add('hidden');
  document.getElementById('emp-activo').checked = true;
}

function configurarModalEmpleado() {
  // Poblar selects de rol y tipo de contrato
  fillSelect('emp-rol',         ROL_OPTIONS,      'Seleccioná un rol');
  fillSelect('emp-tipoContrato', CONTRATO_OPTIONS, 'Seleccioná tipo de contrato');

  document.getElementById('form-empleado').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errEl = document.getElementById('emp-error');
    const btnOk = document.getElementById('btn-emp-guardar');
    errEl.classList.add('hidden');

    const id             = document.getElementById('emp-id').value.trim();
    const nombreCompleto = document.getElementById('emp-nombreCompleto').value.trim();
    const correo         = document.getElementById('emp-correo').value.trim();
    const docId          = document.getElementById('emp-documentoIdentidad').value.trim();
    const rol            = document.getElementById('emp-rol').value;
    const tipoContrato   = document.getElementById('emp-tipoContrato').value;
    const activo         = document.getElementById('emp-activo').checked;
    const password       = document.getElementById('emp-password').value;

    // Validaciones
    if (!nombreCompleto)  return showFormError(errEl, 'El nombre completo es obligatorio.');
    if (!correo)          return showFormError(errEl, 'El correo es obligatorio.');
    if (!docId)           return showFormError(errEl, 'El documento de identidad es obligatorio.');
    if (!rol)             return showFormError(errEl, 'Seleccioná un rol.');
    if (!tipoContrato)    return showFormError(errEl, 'Seleccioná el tipo de contrato.');
    if (!id && !password) return showFormError(errEl, 'La contraseña es obligatoria para nuevos empleados.');

    /** @type {Object} DTO exacto que espera el backend */
    const body = {
      nombreCompleto,
      correo,
      documentoIdentidad: docId,
      rol,
      tipoContrato,
      activo,
    };
    if (!id) body.password = password;

    btnOk.disabled = true;
    try {
      const res = await apiFetch(
        id ? `/admin/empleados/${id}` : '/admin/empleados',
        { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) }
      );

      if (res?.ok) {
        closeModal('modal-empleado');
        toast(id ? '✓ Empleado actualizado' : '✓ Empleado creado');
        empAsignacionesCache = [];
        await cargarEmpleados();
      } else {
        const data = await res?.json().catch(() => ({}));
        showFormError(errEl, data.message ?? 'Error al guardar.');
      }
    } catch {
      showFormError(errEl, 'Error de conexión.');
    } finally {
      btnOk.disabled = false;
    }
  });
}

function showFormError(el, msg) {
  el.textContent = msg;
  el.classList.remove('hidden');
}

async function resolverAprobacionEmpleado(id, aprobar) {
  const res = await apiFetch(`/admin/empleados/${id}/aprobacion?aprobar=${aprobar}`, { method: 'PATCH' });
  if (res?.ok) {
    toast(aprobar ? 'Acceso móvil aprobado' : 'Acceso móvil rechazado', aprobar ? 'success' : 'warning');
    empAsignacionesCache = [];
    await cargarEmpleados();
  } else {
    toast('No se pudo actualizar la aprobación', 'error');
  }
}
