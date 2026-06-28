// ============================================================
// EPESA — Dashboard SPA: router e inicialización
// ============================================================

const VIEW_TITLES = {
  excepciones:  'Excepciones Pendientes',
  asistencias:  'Consolidado de Asistencias',
  empleados:    'Gestión de Empleados',
  obras:        'Gestión de Obras',
  asignaciones: 'Asignaciones de Personal',
};

const VIEW_LOADERS = {
  excepciones:  cargarExcepciones,
  asistencias:  cargarAsistencias,
  empleados:    cargarEmpleados,
  obras:        cargarObras,
  asignaciones: cargarAsignaciones,
};

document.addEventListener('DOMContentLoaded', () => {
  // ── Datos de sesión ──────────────────────────────────────
  const userName = localStorage.getItem('user_name') ?? 'Usuario';
  const role     = localStorage.getItem('user_role') ?? '';

  document.getElementById('user-name').textContent      = userName;
  document.getElementById('user-role-label').textContent = role;
  document.getElementById('user-avatar').textContent    = initials(userName);

  // ── RBAC inicial ─────────────────────────────────────────
  applyRBAC();

  // ── Modales ──────────────────────────────────────────────
  initModalBehavior();
  configurarModalEmpleado();
  configurarModalObra();
  configurarModalAsignacion();
  configurarModalConfirm();
  configurarModalExcepcion();

  // ── Sidebar toggle (móvil) ───────────────────────────────
  document.getElementById('menu-toggle').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('open');
  });
  document.addEventListener('click', e => {
    const sb = document.getElementById('sidebar');
    if (sb.classList.contains('open') &&
        !sb.contains(e.target) &&
        e.target.id !== 'menu-toggle') {
      sb.classList.remove('open');
    }
  });

  // ── Vista inicial ────────────────────────────────────────
  goToView('excepciones');
});

// ── Excepción: inicializar modal (botones aprobar/rechazar) ─

function configurarModalExcepcion() {
  document.getElementById('btn-exc-aprobar').addEventListener('click',  () => resolverExcepcion(true));
  document.getElementById('btn-exc-rechazar').addEventListener('click', () => resolverExcepcion(false));
}

// ── SPA Router ───────────────────────────────────────────────

function goToView(vista) {
  // Ocultar todas las vistas
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));

  // Desactivar todos los nav links
  document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

  // Activar la vista y el link correspondiente
  document.getElementById(`view-${vista}`)?.classList.add('active');
  document.querySelector(`[data-view="${vista}"]`)?.classList.add('active');

  // Título del topbar
  document.getElementById('topbar-title').textContent = VIEW_TITLES[vista] ?? 'Dashboard';

  // Cerrar sidebar en móvil
  document.getElementById('sidebar').classList.remove('open');

  // Cargar datos
  VIEW_LOADERS[vista]?.();
}
