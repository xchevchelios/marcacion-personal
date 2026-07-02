<script>
  import { onMount } from 'svelte'
  import { canManageEmployees } from '../lib/auth.js'
  import { ApiError, del, get, post } from '../lib/api.js'

  let { role, onUnauthorized } = $props()
  let assignments = $state([]), employees = $state([]), obras = $state([])
  let loading = $state(true), saving = $state(false), error = $state(''), showForm = $state(false)
  let empleadoId = $state(''), obraId = $state(''), horaEntrada = $state('08:00'), horaSalida = $state('17:00')
  const list = (data) => Array.isArray(data) ? data : (data?.content ?? [])
  const formatDate = (value) => value ? new Intl.DateTimeFormat('es-PY').format(new Date(`${value}T00:00:00`)) : '—'
  function fail(cause, fallback) { if (cause instanceof ApiError && cause.status === 401) return onUnauthorized(); if (cause instanceof ApiError && cause.status === 403) { error = 'No tenés permisos para realizar esta acción.'; return } error = cause.message || fallback }
  async function load() {
    loading = true; error = ''
    try {
      assignments = list(await get('/admin/asignaciones'))
      if (canManageEmployees(role)) {
        const [employeeData, workData] = await Promise.all([get('/admin/empleados'), get('/admin/obras')])
        employees = list(employeeData).filter((item) => item.activo)
        obras = list(workData).filter((item) => item.activa)
      } else {
        employees = []
        obras = []
      }
    }
    catch (cause) { fail(cause, 'No se pudieron cargar las asignaciones.') }
    finally { loading = false }
  }
  function resetForm() { empleadoId = ''; obraId = ''; horaEntrada = '08:00'; horaSalida = '17:00'; error = '' }
  async function submit(event) {
    event.preventDefault(); error = ''
    if (!empleadoId || !obraId) { error = 'Seleccioná un empleado y una obra.'; return }
    if (horaSalida <= horaEntrada) { error = 'La hora de salida debe ser posterior a la entrada.'; return }
    saving = true
    try { await post('/admin/asignaciones', { empleadoId, obraId, horaEntrada, horaSalida }); showForm = false; resetForm(); await load() }
    catch (cause) { fail(cause, 'No se pudo crear la asignación.') }
    finally { saving = false }
  }
  async function remove(item) {
    if (!confirm(`¿Retirar a ${item.empleadoNombre} de ${item.obraNombre}?`)) return
    try { await del(`/admin/asignaciones/${item.id}`); await load() }
    catch (cause) { fail(cause, 'No se pudo eliminar la asignación.') }
  }
  onMount(load)
</script>

<section class="view-stack"><div class="view-heading"><div><span class="eyebrow">Organización</span><h2>Asignaciones de personal</h2><p>Gestioná quién trabaja en cada obra y sus horarios programados.</p></div><div class="row-actions"><button class="secondary-button" type="button" onclick={load}>Actualizar</button>{#if canManageEmployees(role)}<button class="primary-button" type="button" onclick={() => { resetForm(); showForm = true }}>Nueva asignación</button>{/if}</div></div>
  {#if error}<div class="alert error">{error}</div>{/if}
  <div class="panel table-wrap"><table><thead><tr><th>Empleado</th><th>Obra</th><th>Fecha de asignación</th><th>Entrada</th><th>Salida</th>{#if canManageEmployees(role)}<th>Acciones</th>{/if}</tr></thead><tbody>{#if loading}<tr><td colspan="6" class="table-message">Cargando asignaciones…</td></tr>{:else if !assignments.length}<tr><td colspan="6" class="table-message">No existen asignaciones.</td></tr>{:else}{#each assignments as assignment (assignment.id)}<tr><td><strong>{assignment.empleadoNombre}</strong></td><td><strong>{assignment.obraId}</strong> · {assignment.obraNombre}</td><td>{formatDate(assignment.fechaInicio)}</td><td>{assignment.horaEntrada || '08:00'}</td><td>{assignment.horaSalida || '17:00'}</td>{#if canManageEmployees(role)}<td><button class="danger-button" type="button" onclick={() => remove(assignment)}>Retirar</button></td>{/if}</tr>{/each}{/if}</tbody></table></div>
</section>

{#if showForm}<div class="modal-backdrop" role="presentation" onclick={(event) => event.target === event.currentTarget && (showForm = false)}><div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="assignment-title"><div class="detail-header"><div><span class="eyebrow">Nueva</span><h2 id="assignment-title">Asignación de personal</h2></div><button class="icon-button" aria-label="Cerrar" onclick={() => showForm = false}>×</button></div><form class="modal-form" onsubmit={submit}><label>Empleado<select bind:value={empleadoId}><option value="">Seleccioná un empleado</option>{#each employees as employee}<option value={employee.id}>{employee.nombreCompleto}</option>{/each}</select></label><label>Obra<select bind:value={obraId}><option value="">Seleccioná una obra</option>{#each obras as obra}<option value={obra.codigoSap}>{obra.codigoSap} — {obra.nombre}</option>{/each}</select></label><div class="form-grid"><label>Hora de entrada<input type="time" bind:value={horaEntrada} /></label><label>Hora de salida<input type="time" bind:value={horaSalida} /></label></div>{#if error}<div class="alert error">{error}</div>{/if}<div class="modal-actions"><button class="secondary-button" type="button" onclick={() => showForm = false}>Cancelar</button><button class="primary-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Asignar'}</button></div></form></div></div>{/if}
