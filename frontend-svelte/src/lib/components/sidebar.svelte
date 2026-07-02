<script>
  let { active, open, role, onNavigate, onClose } = $props()
  let items = $derived([
    { id: 'inicio', label: 'Resumen', icon: '⌂', ready: true },
    { id: 'empleados', label: 'Empleados', icon: '♙', ready: true },
    { id: 'obras', label: 'Obras', icon: '▦' },
    { id: 'asignaciones', label: 'Asignaciones', icon: '⇄' },
    { id: 'asistencias', label: 'Asistencias', icon: '✓', ready: true },
    { id: 'excepciones', label: 'Excepciones', icon: '!', ready: true },
    ...(role === 'SOPORTE' ? [{ id: 'auditoria', label: 'Auditoría', icon: '◉', ready: true }] : []),
  ])
</script>

{#if open}<button class="sidebar-scrim" aria-label="Cerrar menú" onclick={onClose}></button>{/if}
<aside class:open class="sidebar">
  <div class="brand"><span>EP</span><div><strong>EPESA</strong><small>Marcación</small></div></div>
  <nav aria-label="Navegación principal">
    {#each items as item}
      <button class:active={active === item.id} type="button" onclick={() => onNavigate(item.id)}>
        <span>{item.icon}</span>{item.label}{#if !item.ready}<small>Próximo</small>{/if}
      </button>
    {/each}
  </nav>
</aside>
