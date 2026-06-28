// ============================================================
// EPESA — API Wrapper Centralizado
// ============================================================

/**
 * Wrapper centralizado para todas las peticiones al backend.
 * - Inyecta Authorization header automáticamente
 * - 401 → cierra sesión y redirige al login
 * - 403 → devuelve la respuesta (el módulo decide qué mostrar)
 * - Lanza error en fallo de red para que el caller lo capture
 *
 * @param {string} endpoint  - path relativo, ej: '/admin/empleados'
 * @param {RequestInit} opts - opciones fetch opcionales
 * @returns {Promise<Response|null>}
 */
async function apiFetch(endpoint, opts = {}) {
  const token = localStorage.getItem('jwt_token');

  const config = {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...(opts.headers ?? {}),
    },
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

  if (response.status === 401) {
    sessionExpired();
    return null;
  }

  return response;
}

function sessionExpired() {
  localStorage.clear();
  // Navegar a index.html relativo a la raíz del servidor
  const segments = window.location.pathname.split('/');
  // Encontrar 'frontend' u otro prefijo y construir ruta correcta
  const frontendIdx = segments.findIndex(s => s === 'frontend');
  const base = frontendIdx >= 0
    ? '/' + segments.slice(1, frontendIdx + 1).join('/')
    : '';
  window.location.href = `${base}/index.html`;
}
