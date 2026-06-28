// ============================================================
// EPESA — Utilidades compartidas
// ============================================================

// ── RBAC ──────────────────────────────────────────────────

const _role = localStorage.getItem('user_role') ?? '';

const isAdmin = () => _role === ROLES.RRHH || _role === ROLES.ADMIN;

/** Oculta elementos .rrhh-only y deshabilita [data-admin-action] para no-admins */
function applyRBAC() {
  if (isAdmin()) return;
  document.querySelectorAll('.rrhh-only').forEach(el => el.classList.add('hidden'));
  document.querySelectorAll('[data-admin-action]').forEach(btn => {
    btn.disabled = true;
    btn.title    = 'Acción restringida a RRHH / Admin';
  });
}

// ── XSS escape ────────────────────────────────────────────

function escStr(val) {
  if (val == null) return '';
  return String(val)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ── Fechas ────────────────────────────────────────────────

/** ISO → DD/MM/YYYY HH:mm */
function fmtDateTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (isNaN(d)) return iso;
  const p = n => String(n).padStart(2, '0');
  return `${p(d.getDate())}/${p(d.getMonth()+1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

/** ISO → DD/MM/YYYY */
function fmtDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (isNaN(d)) return iso;
  const p = n => String(n).padStart(2, '0');
  return `${p(d.getDate())}/${p(d.getMonth()+1)}/${d.getFullYear()}`;
}

// ── Iniciales ─────────────────────────────────────────────

function initials(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2).map(w => w[0]).join('').toUpperCase();
}

// ── Toast ─────────────────────────────────────────────────

function toast(msg, type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = msg;
  container.appendChild(el);

  requestAnimationFrame(() => el.classList.add('visible'));
  setTimeout(() => {
    el.classList.remove('visible');
    setTimeout(() => el.remove(), 300);
  }, 3500);
}

// ── Modal genérico ─────────────────────────────────────────

function openModal(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.remove('hidden');
  requestAnimationFrame(() => el.classList.add('visible'));
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.remove('visible');
  setTimeout(() => el.classList.add('hidden'), 250);
}

/** Cierra cualquier modal abierto al presionar Escape o hacer clic en el overlay */
function initModalBehavior() {
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.visible').forEach(m => closeModal(m.id));
    }
  });
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', e => {
      if (e.target === overlay) closeModal(overlay.id);
    });
  });
}

// ── Tabla helpers ─────────────────────────────────────────

function loadingRows(colspan) {
  return `<tr>
    <td colspan="${colspan}" class="loading-cell">
      <div class="loading-dots"><span></span><span></span><span></span></div>
      <p>Cargando…</p>
    </td>
  </tr>`;
}

function errorRow(colspan, retryFn) {
  return `<tr>
    <td colspan="${colspan}" class="error-cell">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      Error al cargar. <button class="btn-link" onclick="${retryFn}">Reintentar</button>
    </td>
  </tr>`;
}

function emptyRow(colspan, msg = 'Sin registros.') {
  return `<tr><td colspan="${colspan}">
    <div class="empty-state">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/>
      </svg>
      <p>${msg}</p>
    </div>
  </td></tr>`;
}

// ── Badge helper ──────────────────────────────────────────

function badge(text, cls) {
  return `<span class="badge badge-${cls}">${escStr(text)}</span>`;
}

function activeBadge(activo) {
  return badge(activo ? 'Activo' : 'Inactivo', activo ? 'green' : 'gray');
}

// ── Select builder ─────────────────────────────────────────

/**
 * Rellena un <select> con opciones.
 * @param {string} selectId
 * @param {Array<{value,label}>} options
 * @param {string} placeholder - primera opción vacía
 */
function fillSelect(selectId, options, placeholder = 'Seleccioná una opción') {
  const sel = document.getElementById(selectId);
  if (!sel) return;
  sel.innerHTML = `<option value="">${escStr(placeholder)}</option>` +
    options.map(o => `<option value="${escStr(o.value)}">${escStr(o.label)}</option>`).join('');
}

// ── Prevent duplicate concurrent requests ─────────────────

const _loading = new Set();

function isLoading(key) { return _loading.has(key); }
function setLoadingKey(key, on) { on ? _loading.add(key) : _loading.delete(key); }
