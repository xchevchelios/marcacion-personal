// =============================================
// EPESA - Lógica del Dashboard
// =============================================

const userRole = localStorage.getItem('user_role');
const userName = localStorage.getItem('user_name') || 'Usuario';

// Configurar nombre de usuario en UI
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('user-name').textContent = userName;
  document.getElementById('user-role-badge').textContent = userRole || 'Sin rol';

  // Ocultar secciones de RRHH si el rol no corresponde
  if (userRole !== 'RRHH') {
    document.querySelectorAll('.rrhh-only').forEach(el => el.classList.add('hidden'));
  }

  cargarExcepciones();
  configurarModal();
  configurarSidebar();
});

// ─────────────────────────────────────────
// Carga y renderizado de excepciones
// ─────────────────────────────────────────

async function cargarExcepciones() {
  const tbody = document.getElementById('tabla-asistencias');
  const counter = document.getElementById('excepciones-counter');
  const emptyState = document.getElementById('empty-state');
  const tableWrapper = document.getElementById('table-wrapper');

  tbody.innerHTML = `
    <tr id="loading-row">
      <td colspan="6" class="loading-cell">
        <div class="loading-dots">
          <span></span><span></span><span></span>
        </div>
        <p>Cargando excepciones...</p>
      </td>
    </tr>`;

  try {
    const response = await apiFetch('/admin/dashboard/excepciones');
    if (!response) return;

    const data = await response.json();
    const lista = Array.isArray(data) ? data : (data.content || []);

    counter.textContent = lista.length;
    actualizarIndicadorHeader(lista.length);

    tbody.innerHTML = '';

    if (lista.length === 0) {
      tableWrapper.classList.add('hidden');
      emptyState.classList.remove('hidden');
      return;
    }

    emptyState.classList.add('hidden');
    tableWrapper.classList.remove('hidden');

    lista.forEach((item, index) => {
      const tr = document.createElement('tr');
      tr.style.animationDelay = `${index * 40}ms`;
      tr.classList.add('fade-in-row');

      const estadoBadge = getEstadoBadge(item.motivoRevision);
      const fecha = item.fechaMarcacion
        ? new Date(item.fechaMarcacion).toLocaleDateString('es-PY', { day: '2-digit', month: 'short', year: 'numeric' })
        : '—';

      tr.innerHTML = `
        <td class="td-empleado">
          <div class="empleado-avatar">${getInitials(item.empleadoNombre)}</div>
          <span>${item.empleadoNombre || '—'}</span>
        </td>
        <td>${item.obraNombre || '—'}</td>
        <td>${fecha}</td>
        <td><span class="badge ${estadoBadge.clase}">${estadoBadge.texto}</span></td>
        <td class="td-motivo" title="${item.motivoRevision || ''}">${item.motivoRevision || '—'}</td>
        <td class="td-acciones">
          <button class="btn-resolver" data-id="${item.id}" onclick="abrirModal('${item.id}', '${escapeHtml(item.empleadoNombre)}')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            Resolver
          </button>
        </td>
      `;

      tbody.appendChild(tr);
    });

  } catch (error) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" class="error-cell">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <span>Error al cargar los datos. <button onclick="cargarExcepciones()" class="btn-retry">Reintentar</button></span>
        </td>
      </tr>`;
  }
}

// ─────────────────────────────────────────
// Modal de resolución
// ─────────────────────────────────────────

let uuidActual = null;

function abrirModal(uuid, nombre) {
  uuidActual = uuid;
  document.getElementById('modal-empleado-nombre').textContent = nombre;
  document.getElementById('modal-nota').value = '';
  document.getElementById('modal-error').classList.add('hidden');
  document.getElementById('modal-overlay').classList.remove('hidden');
  setTimeout(() => document.getElementById('modal-overlay').classList.add('visible'), 10);
  document.getElementById('modal-nota').focus();
}

function cerrarModal() {
  const overlay = document.getElementById('modal-overlay');
  overlay.classList.remove('visible');
  setTimeout(() => overlay.classList.add('hidden'), 250);
  uuidActual = null;
}

function configurarModal() {
  document.getElementById('modal-overlay').addEventListener('click', (e) => {
    if (e.target === document.getElementById('modal-overlay')) cerrarModal();
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') cerrarModal();
  });

  document.getElementById('btn-aprobar').addEventListener('click', () => resolverExcepcion(true));
  document.getElementById('btn-rechazar').addEventListener('click', () => resolverExcepcion(false));
}

async function resolverExcepcion(aprobar) {
  if (!uuidActual) return;

  const nota = document.getElementById('modal-nota').value.trim();
  const modalError = document.getElementById('modal-error');

  if (!nota) {
    modalError.textContent = 'La nota de resolución es obligatoria.';
    modalError.classList.remove('hidden');
    document.getElementById('modal-nota').focus();
    return;
  }

  const btnAprobar = document.getElementById('btn-aprobar');
  const btnRechazar = document.getElementById('btn-rechazar');
  btnAprobar.disabled = true;
  btnRechazar.disabled = true;
  modalError.classList.add('hidden');

  try {
    const params = new URLSearchParams({ aprobar, nota });
    const response = await apiFetch(
      `/admin/dashboard/excepciones/${uuidActual}/resolver?${params}`,
      { method: 'PATCH' }
    );

    if (response && response.ok) {
      cerrarModal();
      mostrarToast(aprobar ? '✓ Asistencia aprobada correctamente' : '✓ Asistencia rechazada correctamente', aprobar ? 'success' : 'warning');
      await cargarExcepciones();
    } else {
      const err = await response?.json().catch(() => ({}));
      modalError.textContent = err.message || 'Ocurrió un error al procesar la resolución.';
      modalError.classList.remove('hidden');
    }
  } catch (error) {
    modalError.textContent = 'Error de conexión. Verificá tu red e intentá nuevamente.';
    modalError.classList.remove('hidden');
  } finally {
    btnAprobar.disabled = false;
    btnRechazar.disabled = false;
  }
}

// ─────────────────────────────────────────
// Sidebar móvil
// ─────────────────────────────────────────

function configurarSidebar() {
  document.getElementById('menu-toggle').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('open');
  });
}

// ─────────────────────────────────────────
// Utilidades
// ─────────────────────────────────────────

function getInitials(nombre) {
  if (!nombre) return '?';
  return nombre.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase();
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

function getEstadoBadge(motivo) {
  if (!motivo) return { clase: 'badge-gray', texto: 'Sin motivo' };
  const m = motivo.toLowerCase();
  if (m.includes('tardanza') || m.includes('tarde')) return { clase: 'badge-yellow', texto: 'Tardanza' };
  if (m.includes('ausencia') || m.includes('ausente') || m.includes('falta')) return { clase: 'badge-red', texto: 'Ausencia' };
  if (m.includes('hora extra') || m.includes('horas extra')) return { clase: 'badge-blue', texto: 'Hora extra' };
  return { clase: 'badge-gray', texto: 'Revisión' };
}

function actualizarIndicadorHeader(count) {
  const indicator = document.getElementById('pending-indicator');
  indicator.textContent = count;
  indicator.classList.toggle('pulse', count > 0);
}

function mostrarToast(mensaje, tipo = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${tipo}`;
  toast.textContent = mensaje;
  document.getElementById('toast-container').appendChild(toast);
  requestAnimationFrame(() => toast.classList.add('visible'));
  setTimeout(() => {
    toast.classList.remove('visible');
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}