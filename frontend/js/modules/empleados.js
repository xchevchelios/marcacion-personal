// ============================================================
// EPESA — Módulo: Empleados  (solo RRHH/Admin)
// ============================================================

let empListCache = [];

async function cargarEmpleados() {
  if (isLoading('empleados')) return;
  setLoadingKey('empleados', true);

  const tbody = document.getElementById('tbody-empleados');
  tbody.innerHTML = loadingRows(5);

  try {
    const res = await apiFetch('/admin/empleados');
    if (!res) return;

    if (!res.ok) {
      tbody.innerHTML = errorRow(5, 'cargarEmpleados()');
      return;
    }

    const data = await res.json();
    empListCache = Array.isArray(data) ? data : (data.content ?? []);
    tbody.innerHTML = '';

    if (!empListCache.length) {
      tbody.innerHTML = emptyRow(5, 'Sin empleados registrados.');
      return;
    }

    empListCache.forEach((emp, i) => {
      const tr = document.createElement('tr');
      tr.className = 'fade-in';
      tr.style.animationDelay = `${i * 25}ms`;

      tr.innerHTML = `
        <td>
          <div class="cell-avatar">
            <span class="avatar-chip">${initials(emp.nombreCompleto)}</span>
            <span class="fw-medium">${escStr(emp.nombreCompleto) || '—'}</span>
          </div>
        </td>
        <td>${escStr(emp.documentoIdentidad) || '—'}</td>
        <td>${badge(emp.rol, 'blue')}</td>
        <td>${activeBadge(emp.activo)}</td>
        <td class="td-actions rrhh-only">
          <button class="btn-icon" title="Editar" data-admin-action onclick="abrirModalEditarEmpleado('${escStr(emp.id)}')">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </button>
          <button class="btn-icon danger" title="Eliminar" data-admin-action onclick="confirmarEliminar('empleado','${escStr(emp.id)}','${escStr(emp.nombreCompleto)}')">
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
    tbody.innerHTML = errorRow(5, 'cargarEmpleados()');
  } finally {
    setLoadingKey('empleados', false);
    applyRBAC(); // re-aplicar tras renderizar
  }
}

// ── Modal Empleado ─────────────────────────────────────────

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
