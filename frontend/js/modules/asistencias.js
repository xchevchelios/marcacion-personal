// ============================================================
// EPESA — Módulo: Asistencias
// ============================================================

let _asistenciasCache = [];
let _obrasParaFiltro  = [];

async function cargarAsistencias() {
  if (isLoading('asistencias')) return;
  setLoadingKey('asistencias', true);

  const tbody = document.getElementById('tbody-asistencias');
  tbody.innerHTML = loadingRows(7);

  // Cargar obras para el filtro si aún no están disponibles
  if (!_obrasParaFiltro.length) await _cargarObrasParaFiltro();

  try {
    const res = await apiFetch('/admin/dashboard/asistencias');
    if (!res) return;

    if (!res.ok) {
      tbody.innerHTML = errorRow(7, 'cargarAsistencias()');
      return;
    }

    const data = await res.json();
    _asistenciasCache = Array.isArray(data) ? data : (data.content ?? []);
    _renderAsistencias(_asistenciasCache);

  } catch {
    tbody.innerHTML = errorRow(7, 'cargarAsistencias()');
  } finally {
    setLoadingKey('asistencias', false);
  }
}

async function _cargarObrasParaFiltro() {
  try {
    const res = await apiFetch('/admin/obras');
    if (!res?.ok) return;
    const data = await res.json();
    _obrasParaFiltro = Array.isArray(data) ? data : (data.content ?? []);
    const sel = document.getElementById('filtro-obra');
    if (!sel) return;
    sel.innerHTML = '<option value="">Todas las obras</option>' +
      _obrasParaFiltro.map(o => `<option value="${escStr(o.codigoSap)}">${escStr(o.codigoSap)} — ${escStr(o.nombre)}</option>`).join('');
  } catch { /* silencioso */ }
}

function _renderAsistencias(lista) {
  const tbody = document.getElementById('tbody-asistencias');
  tbody.innerHTML = '';

  if (!lista.length) {
    tbody.innerHTML = emptyRow(7, 'Sin asistencias que coincidan con los filtros.');
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
      <td>${escStr(a.obraNombre) || '—'}</td>
      <td>${fmtDateTime(a.horaEntrada)}</td>
      <td>${fmtDateTime(a.horaSalida)}</td>
      <td>${a.horasTrabajadas != null ? escStr(a.horasTrabajadas) + ' h' : '—'}</td>
      <td>${a.requiereRevision ? badge('Pendiente', 'yellow') : badge('Conforme', 'green')}</td>
      <td class="td-truncate" title="${escStr(a.motivoRevision)}">${escStr(a.motivoRevision) || '—'}</td>`;
    tbody.appendChild(tr);
  });
}

function aplicarFiltrosAsistencias() {
  const obraId = document.getElementById('filtro-obra').value;
  const desde  = document.getElementById('filtro-desde').value;
  const hasta  = document.getElementById('filtro-hasta').value;

  let result = [..._asistenciasCache];

  if (obraId) result = result.filter(a => String(a.obraId) === obraId || String(a.obraID) === obraId);
  if (desde)  result = result.filter(a => a.horaEntrada && new Date(a.horaEntrada) >= new Date(desde));
  if (hasta)  result = result.filter(a => a.horaEntrada && new Date(a.horaEntrada) <= new Date(hasta + 'T23:59:59'));

  _renderAsistencias(result);
}

function limpiarFiltros() {
  document.getElementById('filtro-obra').value  = '';
  document.getElementById('filtro-desde').value = '';
  document.getElementById('filtro-hasta').value = '';
  _renderAsistencias(_asistenciasCache);
}
