// ============================================================
// EPESA — Módulo: Excepciones
// ============================================================

let _excepcionActualId = null;

async function cargarExcepciones() {
  if (isLoading('excepciones')) return;
  setLoadingKey('excepciones', true);

  const tbody = document.getElementById('tbody-excepciones');
  tbody.innerHTML = loadingRows(6);

  try {
    const res = await apiFetch('/admin/dashboard/excepciones');
    if (!res) return;

    if (!res.ok) {
      tbody.innerHTML = errorRow(6, 'cargarExcepciones()');
      return;
    }

    const data  = await res.json();
    const lista = Array.isArray(data) ? data : (data.content ?? []);

    // Actualizar contador del badge lateral
    document.getElementById('badge-excepciones').textContent = lista.length || '';

    tbody.innerHTML = '';

    if (!lista.length) {
      tbody.innerHTML = emptyRow(6, 'Sin excepciones pendientes. ✓');
      return;
    }

    lista.forEach((item, i) => {
      const tr = document.createElement('tr');
      tr.className = 'fade-in';
      tr.style.animationDelay = `${i * 30}ms`;

      const canResolve = isAdmin();

      tr.innerHTML = `
        <td>
          <div class="cell-avatar">
            <span class="avatar-chip">${initials(item.empleadoNombre)}</span>
            <span class="fw-medium">${escStr(item.empleadoNombre) || '—'}</span>
          </div>
        </td>
        <td>${escStr(item.obraNombre) || '—'}</td>
        <td>${fmtDateTime(item.fechaHoraReal)}</td>
        <td class="td-truncate" title="${escStr(item.motivoRevision)}">${escStr(item.motivoRevision) || '—'}</td>
        <td>${badge('Pendiente', 'yellow')}</td>
        <td>
          <button
            class="btn-action ${canResolve ? '' : 'disabled'}"
            ${canResolve ? `onclick="abrirModalExcepcion('${escStr(item.id)}','${escStr(item.empleadoNombre)}')"` : 'disabled title="Solo RRHH/Admin"'}
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            Resolver
          </button>
        </td>`;
      tbody.appendChild(tr);
    });
  } catch {
    tbody.innerHTML = errorRow(6, 'cargarExcepciones()');
  } finally {
    setLoadingKey('excepciones', false);
  }
}

function abrirModalExcepcion(id, nombre) {
  _excepcionActualId = id;
  document.getElementById('exc-empleado-nombre').textContent = nombre;
  document.getElementById('exc-nota').value = '';
  document.getElementById('exc-error').classList.add('hidden');
  openModal('modal-excepcion');
}

async function resolverExcepcion(aprobar) {
  if (!_excepcionActualId) return;

  const nota    = document.getElementById('exc-nota').value.trim();
  const errEl   = document.getElementById('exc-error');
  const btnA    = document.getElementById('btn-exc-aprobar');
  const btnR    = document.getElementById('btn-exc-rechazar');

  if (!nota) {
    errEl.textContent = 'La nota de resolución es obligatoria.';
    errEl.classList.remove('hidden');
    return;
  }

  btnA.disabled = btnR.disabled = true;
  errEl.classList.add('hidden');

  try {
    const params = new URLSearchParams({ aprobar: String(aprobar), nota });
    const res = await apiFetch(
      `/admin/dashboard/excepciones/${_excepcionActualId}/resolver?${params}`,
      { method: 'PATCH' }
    );

    if (res?.ok) {
      closeModal('modal-excepcion');
      toast(aprobar ? '✓ Excepción aprobada' : '✓ Excepción rechazada', aprobar ? 'success' : 'warning');
      await cargarExcepciones();
    } else {
      const data = await res?.json().catch(() => ({}));
      errEl.textContent = data.message ?? 'Error al procesar.';
      errEl.classList.remove('hidden');
    }
  } catch {
    errEl.textContent = 'Error de conexión.';
    errEl.classList.remove('hidden');
  } finally {
    btnA.disabled = btnR.disabled = false;
  }
}
