<script>
  import Header from '../lib/components/header.svelte'
  import Sidebar from '../lib/components/sidebar.svelte'
  import EmpleadosView from './EmpleadosView.svelte'
  import ObrasView from './ObrasView.svelte'
  import AsignacionesView from './AsignacionesView.svelte'
  import AsistenciasView from './AsistenciasView.svelte'
  import ExcepcionesView from './ExcepcionesView.svelte'
  import ResumenView from './ResumenView.svelte'
  import AuditoriaView from './AuditoriaView.svelte'
  let { session, onLogout } = $props()
  let active = $state('inicio')
  let menuOpen = $state(false)
  let employeeToOpen = $state(null)
  const titles = { inicio: 'Resumen', empleados: 'Gestión de empleados', obras: 'Gestión de obras', asignaciones: 'Asignaciones', asistencias: 'Asistencias', excepciones: 'Excepciones', auditoria: 'Auditoría del sistema' }
  function navigate(view) { active = view; menuOpen = false }
  function openEmployee(id) { employeeToOpen = id; navigate('empleados') }
</script>

<div class="app-shell">
  <Sidebar {active} role={session.role} open={menuOpen} onNavigate={navigate} onClose={() => menuOpen = false} />
  <div class="main-column">
    <Header title={titles[active]} {session} onMenu={() => menuOpen = !menuOpen} {onLogout} />
    <main class="content">
      {#if active === 'inicio'}
        <ResumenView name={session.name} onUnauthorized={onLogout} />
      {:else if active === 'empleados'}
        <EmpleadosView role={session.role} initialEmployeeId={employeeToOpen} onUnauthorized={onLogout} />
      {:else if active === 'obras'}
        <ObrasView role={session.role} onOpenEmployee={openEmployee} onUnauthorized={onLogout} />
      {:else if active === 'asignaciones'}
        <AsignacionesView role={session.role} onUnauthorized={onLogout} />
      {:else if active === 'asistencias'}
        <AsistenciasView onUnauthorized={onLogout} />
      {:else if active === 'excepciones'}
        <ExcepcionesView role={session.role} onUnauthorized={onLogout} />
      {:else if active === 'auditoria' && session.role === 'SOPORTE'}
        <AuditoriaView onUnauthorized={onLogout} />
      {:else}
        <section class="empty-state"><span>En migración</span><h2>{titles[active]}</h2><p>Esta sección continúa disponible en el frontend anterior mientras se traslada.</p></section>
      {/if}
    </main>
  </div>
</div>
