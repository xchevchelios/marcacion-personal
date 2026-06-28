// ============================================================
// EPESA — Módulo: Obras  (solo RRHH/Admin)
// ============================================================

let obraListCache = [];

async function cargarObras() {
  if (isLoading('obras')) return;
  setLoadingKey('obras', true);

  const tbody = document.getElementById('tbody-obras');
  tbody.innerHTML = loadingRows(5);

  try {
    const res = await apiFetch('/admin/obras');
    if (!res) return;

    if (!res.ok) {
      tbody.innerHTML = errorRow(5, 'cargarObras()');
      return;
    }

    const data = await res.json();
    obraListCache = Array.isArray(data) ? data : (data.content ?? []);
    tbody.innerHTML = '';

    if (!obraListCache.length) {
      tbody.innerHTML = emptyRow(5, 'Sin obras registradas.');
      return;
    }

    obraListCache.forEach((obra, i) => {
      const tr = document.createElement('tr');
      tr.className = 'fade-in';
      tr.style.animationDelay = `${i * 25}ms`;

      tr.innerHTML = `
        <td class="fw-medium">${escStr(obra.nombre) || '—'}</td>
        <td>${escStr(obra.ubicacion) || '—'}</td>
        <td class="td-truncate" title="${escStr(obra.descripcion)}">${escStr(obra.descripcion) || '—'}</td>
        <td>${activeBadge(obra.activa)}</td>
        <td class="td-actions rrhh-only">
          <button class="btn-icon" title="Editar" data-admin-action onclick="abrirModalEditarObra('${escStr(obra.id)}')">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </button>
          <button class="btn-icon danger" title="Eliminar" data-admin-action onclick="confirmarEliminar('obra','${escStr(obra.id)}','${escStr(obra.nombre)}')">
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
    tbody.innerHTML = errorRow(5, 'cargarObras()');
  } finally {
    setLoadingKey('obras', false);
    applyRBAC();
  }
}

// ── Modal Obra ─────────────────────────────────────────────

function abrirModalNuevaObra() {
  document.getElementById('form-obra').reset();
  document.getElementById('obra-id').value = '';
  document.getElementById('obra-modal-titulo').textContent = 'Nueva Obra';
  document.getElementById('obra-activa').checked = true;
  document.getElementById('obra-error').classList.add('hidden');
  openModal('modal-obra');
}

function abrirModalEditarObra(id) {
  const obra = obraListCache.find(o => String(o.id) === String(id));
  if (!obra) return;

  document.getElementById('form-obra').reset();
  document.getElementById('obra-modal-titulo').textContent = 'Editar Obra';
  document.getElementById('obra-id').value          = obra.id;
  document.getElementById('obra-nombre').value      = obra.nombre      ?? '';
  document.getElementById('obra-ubicacion').value   = obra.ubicacion   ?? '';
  document.getElementById('obra-descripcion').value = obra.descripcion ?? '';
  document.getElementById('obra-activa').checked    = obra.activa      ?? true;
  document.getElementById('obra-error').classList.add('hidden');
  openModal('modal-obra');
}

function configurarModalObra() {
  document.getElementById('form-obra').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errEl = document.getElementById('obra-error');
    const btnOk = document.getElementById('btn-obra-guardar');
    errEl.classList.add('hidden');

    const id          = document.getElementById('obra-id').value.trim();
    const nombre      = document.getElementById('obra-nombre').value.trim();
    const ubicacion   = document.getElementById('obra-ubicacion').value.trim();
    const descripcion = document.getElementById('obra-descripcion').value.trim();
    const activa      = document.getElementById('obra-activa').checked;

    if (!nombre) return showFormError(errEl, 'El nombre de la obra es obligatorio.');

    /** @type {Object} DTO exacto — vertices nunca puede ser null */
    const body = { nombre, ubicacion, descripcion, vertices: [], activa };

    btnOk.disabled = true;
    try {
      const res = await apiFetch(
        id ? `/admin/obras/${id}` : '/admin/obras',
        { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) }
      );

      if (res?.ok) {
        closeModal('modal-obra');
        toast(id ? '✓ Obra actualizada' : '✓ Obra creada');
        await cargarObras();
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
