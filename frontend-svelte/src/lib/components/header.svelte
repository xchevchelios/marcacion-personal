<script>
  let { title, session, onMenu, onLogout } = $props()
  const initials = (name = '') => name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
</script>

<header class="topbar">
  <button class="icon-button menu-button" type="button" aria-label="Abrir menú" onclick={onMenu}>☰</button>
  <h1>{title}</h1>
  <div class="user-info">
    <span class="avatar">{initials(session.name)}</span>
    <span class="user-copy"><strong>{session.name}</strong><small>{session.role}</small></span>
    <button class="secondary-button" type="button" onclick={onLogout}>Salir</button>
  </div>
</header>
