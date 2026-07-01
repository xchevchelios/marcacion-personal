// ============================================================
// EPESA — Autenticación
// ============================================================

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('login-form');
  if (!form) return; // Este script se carga en index.html solamente

  // Si ya hay sesión activa, ir al dashboard
  if (localStorage.getItem('jwt_token')) {
    goToDashboard();
    return;
  }

  const errorEl   = document.getElementById('error-msg');
  const btnLogin  = document.getElementById('btn-login');
  const btnText   = document.getElementById('btn-text');
  const btnSpin   = document.getElementById('btn-spinner');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorEl.classList.add('hidden');

    const correo   = document.getElementById('correo').value.trim();
    const password = document.getElementById('password').value;

    if (!correo || !password) {
      showError('Completá todos los campos.');
      return;
    }

    setLoading(true);

    try {
      const res = await fetch(`${API_BASE_URL}/auth/login`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ correo, password }),
      });

      if (res.ok) {
        const data = await res.json();
        localStorage.setItem('jwt_token', data.token);
        localStorage.setItem('user_role', data.rol ?? data.role ?? (Array.isArray(data.roles) ? data.roles[0] : ''));
        localStorage.setItem('user_name', data.nombreCompleto ?? data.nombre ?? data.name ?? correo);
        goToDashboard();
      } else {
        const err = await res.json().catch(() => ({}));
        showError(err.message ?? 'Credenciales incorrectas.');
      }
    } catch {
      showError('No se pudo conectar con el servidor. Verificá que el backend esté activo.');
    } finally {
      setLoading(false);
    }
  });

  function showError(msg) {
    errorEl.textContent = msg;
    errorEl.classList.remove('hidden');
  }

  function setLoading(on) {
    btnLogin.disabled      = on;
    btnText.textContent    = on ? 'Ingresando…' : 'Ingresar';
    btnSpin.classList.toggle('hidden', !on);
  }
});

function goToDashboard() {
  window.location.href = 'dashboard.html';
}

function logout() {
  localStorage.clear();
  window.location.href = 'index.html';
}
