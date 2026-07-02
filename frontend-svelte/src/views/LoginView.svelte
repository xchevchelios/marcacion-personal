<script>
  import { login } from '../lib/auth.js'
  let { onLogin } = $props()
  let correo = $state('')
  let password = $state('')
  let loading = $state(false)
  let error = $state('')

  async function submit(event) {
    event.preventDefault(); error = ''
    if (!correo.trim() || !password) { error = 'Completá todos los campos.'; return }
    loading = true
    try { onLogin(await login(correo.trim(), password)) }
    catch (cause) { error = cause.message || 'No se pudo iniciar sesión.' }
    finally { loading = false }
  }
</script>

<main class="login-page">
  <section class="login-card">
    <div class="login-brand"><span>EP</span><div><strong>EPESA</strong><small>Sistema de marcación</small></div></div>
    <div class="login-heading"><h1>Bienvenido</h1><p>Ingresá para administrar el personal y sus marcaciones.</p></div>
    <form onsubmit={submit}>
      <label>Correo electrónico<input type="email" bind:value={correo} autocomplete="username" placeholder="usuario@epesa.com.py" /></label>
      <label>Contraseña<input type="password" bind:value={password} autocomplete="current-password" placeholder="••••••••" /></label>
      {#if error}<div class="alert error" role="alert">{error}</div>{/if}
      <button class="primary-button full" type="submit" disabled={loading}>{loading ? 'Ingresando…' : 'Ingresar'}</button>
    </form>
  </section>
</main>
