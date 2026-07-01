// ============================================================
// EPESA — Módulo: Asignaciones  (solo RRHH/Admin)
// ============================================================

async function cargarAsignaciones() {
  if (isLoading('asignaciones')) return;
  setLoadingKey('asignaciones', true);

  const tbody = document.getElementById('tbody-asignaciones');
  tbody.innerHTML = loadingRows(6);

  // Precargar empleados y obras para los selects del modal
  await Promise.all([_poblarSelectEmpleados(), _poblarSelectObras()]);

  try {
    const res = await apiFetch('/admin/asignaciones');
    if (!res) return;

    if (!res.ok) {
      tbody.innerHTML = errorRow(6, 'cargarAsignaciones()');
      return;
    }

    const data  = await res.json();
    const lista = Array.isArray(data) ? data : (data.content ?? []);
    tbody.innerHTML = '';

    if (!lista.length) {
      tbody.innerHTML = emptyRow(6, 'Sin asignaciones registradas.');
      return;
    }

    lista.forEach((a, i) => {
      const tr = document.createElement('tr');
      tr.className = 'fade-in';
      tr.style.animationDelay = `${i * 25}ms`;

      tr.innerHTML = `
        <td>
          <div class="cell-avatar">
            <span class="avatar-chip">${initials(a.empleadoNombre)}</span>
            <span class="fw-medium">${escStr(a.empleadoNombre) || '—'}</span>
          </div>
        </td>
        <td><span class="fw-medium">${escStr(a.obraId)}</span> - ${escStr(a.obraNombre) || '—'}</td>
        <td>${fmtDate(a.fechaInicio)}</td>
        <td>${escStr(a.horaEntrada) || '08:00'}</td>
        <td>${escStr(a.horaSalida) || '17:00'}</td>
        <td class="td-actions rrhh-only">
          <button class="btn-icon danger" title="Retirar de la obra" data-admin-action
            onclick="confirmarEliminar('asignacion','${escStr(a.id)}','${escStr(a.empleadoNombre)}')">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <line x1="22" y1="18" x2="16" y2="18"/>
            </svg>
          </button>
        </td>`;
      tbody.appendChild(tr);
    });

  } catch {
    tbody.innerHTML = errorRow(6, 'cargarAsignaciones()');
  } finally {
    setLoadingKey('asignaciones', false);
    applyRBAC();
  }
}

async function _poblarSelectEmpleados() {
  const sel = document.getElementById('asig-empleado');
  if (!sel || sel.options.length > 1) return; // ya cargado
  try {
    const res = await apiFetch('/admin/empleados');
    if (!res?.ok) return;
    const data = await res.json();
    const lista = Array.isArray(data) ? data : (data.content ?? []);
    sel.innerHTML = '<option value="">Seleccioná un empleado</option>' +
      lista.map(e => `<option value="${escStr(e.id)}">${escStr(e.nombreCompleto)}</option>`).join('');
  } catch { /* silencioso */ }
}

async function _poblarSelectObras() {
  const sel = document.getElementById('asig-obra');
  if (!sel || sel.options.length > 1) return; // ya cargado
  try {
    const res = await apiFetch('/admin/obras');
    if (!res?.ok) return;
    const data = await res.json();
    const lista = Array.isArray(data) ? data : (data.content ?? []);
    sel.innerHTML = '<option value="">Seleccioná una obra</option>' +
      lista.map(o => `<option value="${escStr(o.codigoSap)}">${escStr(o.codigoSap)} — ${escStr(o.nombre)}</option>`).join('');
  } catch { /* silencioso */ }
}

// ── Modal Asignación ───────────────────────────────────────

function abrirModalNuevaAsignacion() {
  document.getElementById('asig-empleado').value = '';
  document.getElementById('asig-obra').value     = '';
  document.getElementById('asig-hora-entrada').value = '08:00';
  document.getElementById('asig-hora-salida').value = '17:00';
  document.getElementById('asig-error').classList.add('hidden');
  openModal('modal-asignacion');
}

function configurarModalAsignacion() {
  document.getElementById('form-asignacion').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errEl     = document.getElementById('asig-error');
    const btnOk     = document.getElementById('btn-asig-guardar');
    const empleadoId = document.getElementById('asig-empleado').value;
    const obraId     = document.getElementById('asig-obra').value;
    const horaEntrada = document.getElementById('asig-hora-entrada').value || '08:00';
    const horaSalida = document.getElementById('asig-hora-salida').value || '17:00';

    if (!empleadoId || !obraId) {
      errEl.textContent = 'Seleccioná un empleado y una obra.';
      errEl.classList.remove('hidden');
      return;
    }

    btnOk.disabled = true;
    errEl.classList.add('hidden');

    try {
      const res = await apiFetch('/admin/asignaciones', {
        method: 'POST',
        body:   JSON.stringify({ empleadoId, obraId, horaEntrada, horaSalida }),
      });

      if (res?.ok) {
        closeModal('modal-asignacion');
        toast('✓ Asignación creada');
        invalidarCacheDetalleEmpleados?.();
        invalidarCacheDetalleObras?.();
        // Resetear selects para forzar recarga en próxima apertura
        document.getElementById('asig-empleado').innerHTML = '<option value="">Seleccioná un empleado</option>';
        document.getElementById('asig-obra').innerHTML     = '<option value="">Seleccioná una obra</option>';
        await cargarAsignaciones();
      } else {
        const data = await res?.json().catch(() => ({}));
        errEl.textContent = data.message ?? 'Error al crear la asignación.';
        errEl.classList.remove('hidden');
      }
    } catch {
      errEl.textContent = 'Error de conexión.';
      errEl.classList.remove('hidden');
    } finally {
      btnOk.disabled = false;
    }
  });
}
