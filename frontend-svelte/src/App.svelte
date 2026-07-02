<script>
  import { onMount } from 'svelte'
  import { getSession, logout } from './lib/auth.js'
  import LoginView from './views/LoginView.svelte'
  import DashboardView from './views/DashboardView.svelte'

  let session = $state(getSession())
  function handleLogout() { logout(); session = null }
  onMount(() => {
    let timer
    const reset = () => { clearTimeout(timer); if (session) timer = setTimeout(handleLogout, 30 * 60 * 1000) }
    const events = ['click', 'keydown', 'mousemove', 'touchstart']
    events.forEach((event) => window.addEventListener(event, reset, { passive: true }))
    reset()
    return () => { clearTimeout(timer); events.forEach((event) => window.removeEventListener(event, reset)) }
  })
</script>

{#if session}
  <DashboardView {session} onLogout={handleLogout} />
{:else}
  <LoginView onLogin={(newSession) => session = newSession} />
{/if}
