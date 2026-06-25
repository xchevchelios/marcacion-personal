// =============================================
// EPESA - API Wrapper Centralizado
// =============================================

async function apiFetch(endpoint, options = {}) {
  const token = localStorage.getItem('jwt_token');

  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token,
  };

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...(options.headers || {}),
    },
  };

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

    if (response.status === 401 || response.status === 403) {
      localStorage.clear();
      window.location.href = '../templates/index.html';
      return null;
    }

    return response;
  } catch (error) {
    console.error('[apiFetch] Error de red:', error);
    throw error;
  }
}