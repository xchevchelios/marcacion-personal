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

  const headers = {
    'Content-Type': 'application/json',
    ...(opts.headers ?? {}),
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...opts,
    headers,
  });

  if (response.status === 401) {
    sessionExpired();
    return null;
  }

  return response;
}

function sessionExpired() {
  localStorage.clear();
  window.location.href = 'index.html';
}
