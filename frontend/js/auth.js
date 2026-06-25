// =============================================
// EPESA - Lógica de Autenticación
// =============================================

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('login-form');
  const errorMsg = document.getElementById('error-msg');
  const btnLogin = document.getElementById('btn-login');
  const btnText = document.getElementById('btn-text');
  const btnSpinner = document.getElementById('btn-spinner');

  if (localStorage.getItem('jwt_token')) {
    window.location.href = '../templates/dashboard.html';
    return;
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorMsg.classList.add('hidden');

    const correo = document.getElementById('correo').value.trim();
    const password = document.getElementById('password').value;

    // Estado de carga
    btnText.textContent = 'Ingresando...';
    btnSpinner.classList.remove('hidden');
    btnLogin.disabled = true;

    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ correo, password }),
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem('jwt_token', data.token);
        localStorage.setItem('user_role', data.rol);
        localStorage.setItem('user_name', data.nombre || correo);
        window.location.href = '../templates/dashboard.html';
      } else {
        const err = await response.json().catch(() => ({}));
        errorMsg.textContent = err.message || 'Credenciales incorrectas. Revisá tu correo y contraseña.';
        errorMsg.classList.remove('hidden');
      }
    } catch (error) {
      errorMsg.textContent = 'No se pudo conectar con el servidor. Verificá que el backend esté activo.';
      errorMsg.classList.remove('hidden');
    } finally {
      btnText.textContent = 'Ingresar';
      btnSpinner.classList.add('hidden');
      btnLogin.disabled = false;
    }
  });
});

function logout() {
  localStorage.clear();
  window.location.href = '../templates/index.html';
}