<script>
  import { onMount } from 'svelte'
  import { canManageEmployees } from '../lib/auth.js'
  import { ApiError, get, patch, post, put } from '../lib/api.js'

  let { role, initialEmployeeId = null, onUnauthorized } = $props()
  let empleados = $state([])
  let filtered = $state([])
  let query = $state('')
  let loading = $state(true)
  let error = $state('')
  let selected = $state(null)
  let asignaciones = $state([])
  let marcaciones = $state([])
  let detailLoading = $state(false)
  let actionId = $state(null)
  let showCreate = $state(false)
  let saving = $state(false)
  let formError = $state('')
  let form = $state({ nombreCompleto: '', correo: '', documentoIdentidad: '', rol: 'OPERATIVO', tipoContrato: 'INTERNO', password: '' })
  let editingId = $state(null)

  const list = (data) => Array.isArray(data) ? data : (data?.content ?? [])
  const formatDate = (value) => value ? new Intl.DateTimeFormat('es-PY').format(new Date(`${value}T00:00:00`)) : '-'
  const formatDateTime = (value) => value ? new Intl.DateTimeFormat('es-PY', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '-'
  const statusClass = (value) => String(value ?? '').toLowerCase().replaceAll('_', '-')

  function filter() {
    const term = query.trim().toLocaleLowerCase('es')
    filtered = term ? empleados.filter((item) => [item.nombreCompleto, item.documentoIdentidad, item.correo, item.rol].some((value) => String(value ?? '').toLocaleLowerCase('es').includes(term))) : empleados
  }

  async function loadEmployees() {
    loading = true; error = ''
    try {
      empleados = list(await get('/admin/empleados')); filter()
      if (initialEmployeeId) {
        const target = empleados.find((item) => String(item.id) === String(initialEmployeeId))
        if (target) await openDetail(target)
      }
    }
    catch (cause) { handleError(cause, 'No se pudieron cargar los empleados.') }
    finally { loading = false }
  }

  function handleError(cause, fallback) {
    if (cause instanceof ApiError && cause.status === 401) { onUnauthorized(); return }
    if (cause instanceof ApiError && cause.status === 403) { error = 'No tenes permisos para acceder a esta informacion.'; return }
    error = cause.message || fallback
  }

  async function openDetail(employee) {
    selected = employee; detailLoading = true; error = ''
    try {
      const [assignmentData, attendanceData] = await Promise.all([
        get('/admin/asignaciones'), get('/admin/dashboard/asistencias'),
      ])
      asignaciones = list(assignmentData).filter((item) => String(item.empleadoId) === String(employee.id))
      marcaciones = list(attendanceData)
        .filter((item) => String(item.empleadoId) === String(employee.id))
        .sort((a, b) => new Date(b.fechaHoraReal) - new Date(a.fechaHoraReal))
    } catch (cause) { handleError(cause, 'No se pudo cargar la ficha del empleado.') }
    finally { detailLoading = false }
  }

  async function resolveApproval(employee, approve) {
    actionId = employee.id; error = ''
    try {
      await patch(`/admin/empleados/${employee.id}/aprobacion?aprobar=${approve}`)
      await loadEmployees()
      if (selected?.id === employee.id) selected = empleados.find((item) => item.id === employee.id) ?? null
    } catch (cause) { handleError(cause, 'No se pudo actualizar la aprobacion.') }
    finally { actionId = null }
  }
  async function toggleEmployee(employee) {
    actionId = employee.id; error = ''
    try { await patch(`/admin/empleados/${employee.id}/estado?activo=${!employee.activo}`); await loadEmployees() }
    catch (cause) { handleError(cause, 'No se pudo cambiar el estado del empleado.') }
    finally { actionId = null }
  }
  function openCreate() { editingId = null; form = { nombreCompleto: '', correo: '', documentoIdentidad: '', rol: 'OPERATIVO', tipoContrato: 'INTERNO', password: '' }; formError = ''; showCreate = true }
  function openEdit(employee) { editingId = employee.id; form = { nombreCompleto: employee.nombreCompleto, correo: employee.correo, documentoIdentidad: employee.documentoIdentidad, rol: employee.rol, tipoContrato: employee.tipoContrato, password: '', activo: employee.activo }; formError = ''; showCreate = true }
  async function createEmployee(event) {
    event.preventDefault(); formError = ''
    if (!form.nombreCompleto.trim() || !form.correo.trim() || !form.documentoIdentidad.trim() || (!editingId && !form.password)) { formError = 'Completa todos los campos obligatorios.'; return }
    saving = true
    try { const payload = { ...form, nombreCompleto: form.nombreCompleto.trim(), correo: form.correo.trim(), documentoIdentidad: form.documentoIdentidad.trim(), activo: editingId ? form.activo : true }; if (editingId && !payload.password) delete payload.password; if (editingId) await put(`/admin/empleados/${editingId}`, payload); else await post('/admin/empleados', payload); showCreate = false; await loadEmployees() }
    catch (cause) { formError = cause.message || 'No se pudo crear el empleado.' }
    finally { saving = false }
  }

  onMount(loadEmployees)
</script>

<section class="view-stack">
  <div class="view-heading"><div><span class="eyebrow">Personal</span><h2>Empleados registrados</h2><p>Consulta el estado, las obras y las marcaciones de cada persona.</p></div><div class="row-actions"><button class="secondary-button" type="button" onclick={loadEmployees} disabled={loading}>Actualizar</button>{#if canManageEmployees(role)}<button class="primary-button" type="button" onclick={openCreate}>Nuevo empleado</button>{/if}</div></div>
  <div class="toolbar"><label class="search-field"><span>⌕</span><input type="search" bind:value={query} oninput={filter} placeholder="Buscar por nombre, documento, correo o rol" /></label><span class="result-count">{filtered.length} empleados</span></div>
  {#if error}<div class="alert error" role="alert">{error}</div>{/if}

  <div class="panel table-wrap">
    <table>
      <thead><tr><th>Empleado</th><th>Documento</th><th>Rol</th><th>Aprobacion movil</th><th>Estado</th>{#if canManageEmployees(role)}<th>Acciones</th>{/if}</tr></thead>
      <tbody>
        {#if loading}
          <tr><td colspan="6" class="table-message">Cargando empleados...</td></tr>
        {:else if !filtered.length}
          <tr><td colspan="6" class="table-message">No se encontraron empleados.</td></tr>
        {:else}
          {#each filtered as employee (employee.id)}
            <tr class:selected={selected?.id === employee.id} onclick={() => openDetail(employee)} tabindex="0" onkeydown={(event) => event.key === 'Enter' && openDetail(employee)}>
              <td><div class="employee-cell"><span class="mini-avatar">{employee.nombreCompleto?.[0] ?? '?'}</span><div><strong>{employee.nombreCompleto || 'Sin nombre'}</strong><small>{employee.correo || '-'}</small></div></div></td>
              <td>{employee.documentoIdentidad || '-'}</td><td><span class="pill neutral">{employee.rol || '-'}</span></td>
              <td><span class="pill {statusClass(employee.estadoAprobacion)}">{employee.estadoAprobacion || 'PENDIENTE'}</span></td>
              <td><span class="status-dot" class:enabled={employee.activo}></span>{employee.activo ? 'Activo' : 'Inactivo'}</td>
              {#if canManageEmployees(role)}<td><div class="row-actions">{#if employee.rol === 'OPERATIVO' && employee.estadoAprobacion === 'PENDIENTE'}<button type="button" onclick={(event) => { event.stopPropagation(); resolveApproval(employee, true) }} disabled={actionId === employee.id}>Aprobar</button><button class="danger-link" type="button" onclick={(event) => { event.stopPropagation(); resolveApproval(employee, false) }} disabled={actionId === employee.id}>Rechazar</button>{/if}<button type="button" onclick={(event) => { event.stopPropagation(); openEdit(employee) }}>Editar</button><button type="button" onclick={(event) => { event.stopPropagation(); toggleEmployee(employee) }} disabled={actionId === employee.id}>{employee.activo ? 'Desactivar' : 'Activar'}</button></div></td>{/if}
            </tr>
          {/each}
        {/if}
      </tbody>
    </table>
  </div>
</section>

{#if selected}
  <div class="modal-backdrop" role="presentation" onclick={(event) => event.target === event.currentTarget && (selected = null)}>
    <div class="modal-card employee-modal" role="dialog" aria-modal="true" aria-labelledby="employee-detail-title">
      <div class="detail-header"><div><span class="eyebrow">Ficha de empleado</span><h2 id="employee-detail-title">{selected.nombreCompleto}</h2><p>{selected.activo ? 'Empleado activo' : 'Empleado inactivo'} · aprobacion {selected.estadoAprobacion || 'PENDIENTE'}</p></div><button class="icon-button" aria-label="Cerrar" onclick={() => selected = null}>×</button></div>
      <div class="detail-grid"><article><span>Documento</span><strong>{selected.documentoIdentidad || '-'}</strong></article><article><span>Correo</span><strong>{selected.correo || '-'}</strong></article><article><span>Rol</span><strong>{selected.rol || '-'}</strong></article><article><span>Contrato</span><strong>{selected.tipoContrato || '-'}</strong></article></div>
      <div class="detail-section"><h3>Obras asignadas</h3>{#if detailLoading}<p class="table-message">Cargando ficha...</p>{:else if !asignaciones.length}<p class="empty-inline">Este empleado todavia no tiene obras asignadas.</p>{:else}<div class="table-wrap excel-wrap"><table class="excel-table"><thead><tr><th>Codigo SAP</th><th>Obra</th><th>Desde</th><th>Entrada</th><th>Salida</th></tr></thead><tbody>{#each asignaciones as assignment}<tr><td>{assignment.obraId || '-'}</td><td>{assignment.obraNombre || '-'}</td><td>{formatDate(assignment.fechaInicio)}</td><td>{assignment.horaEntrada || '08:00'}</td><td>{assignment.horaSalida || '17:00'}</td></tr>{/each}</tbody></table></div>{/if}</div>
      <div class="detail-section"><h3>Marcaciones</h3>{#if detailLoading}<p class="table-message">Cargando marcaciones...</p>{:else if !marcaciones.length}<p class="empty-inline">Sin marcaciones registradas.</p>{:else}<div class="table-wrap excel-wrap"><table class="excel-table"><thead><tr><th>Fecha y hora</th><th>Tipo</th><th>Codigo obra</th><th>Obra</th><th>Estado</th><th>Motivo</th><th>Dispositivo</th><th>Evento</th></tr></thead><tbody>{#each marcaciones as mark}<tr><td>{formatDateTime(mark.fechaHoraReal)}</td><td>{mark.tipoMarcacion || 'ENTRADA'}</td><td>{mark.obraId || '-'}</td><td>{mark.obraNombre || '-'}</td><td>{mark.requiereRevision ? 'Revision' : 'Conforme'}</td><td>{mark.motivoRevision || '-'}</td><td>{mark.deviceId || '-'}</td><td>{mark.eventId || '-'}</td></tr>{/each}</tbody></table></div>{/if}</div>
    </div>
  </div>
{/if}

{#if showCreate}<div class="modal-backdrop" role="presentation" onclick={(event) => event.target === event.currentTarget && (showCreate = false)}><div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="employee-create-title"><div class="detail-header"><div><span class="eyebrow">Personal</span><h2 id="employee-create-title">{editingId ? 'Editar empleado' : 'Nuevo empleado'}</h2></div><button class="icon-button" aria-label="Cerrar" onclick={() => showCreate = false}>×</button></div><form class="modal-form" onsubmit={createEmployee}><label>Nombre completo<input bind:value={form.nombreCompleto} autocomplete="name" /></label><label>Correo electronico<input type="email" bind:value={form.correo} autocomplete="email" /></label><label>Documento de identidad<input bind:value={form.documentoIdentidad} /></label><div class="form-grid"><label>Rol<select bind:value={form.rol}><option value="OPERATIVO">Operativo</option><option value="RRHH">RRHH</option><option value="ADMIN">Administrador</option><option value="JEFE_OBRA">Jefe de obra</option><option value="RESIDENTE">Residente</option>{#if role === 'SOPORTE'}<option value="SOPORTE">Soporte</option>{/if}</select></label><label>Tipo de contrato<select bind:value={form.tipoContrato}><option value="INTERNO">Interno</option><option value="TERCERIZADO">Tercerizado</option></select></label></div><label>{editingId ? 'Nueva contrasena (opcional)' : 'Contrasena inicial'}<input type="password" bind:value={form.password} autocomplete="new-password" /></label>{#if formError}<div class="alert error">{formError}</div>{/if}<div class="modal-actions"><button class="secondary-button" type="button" onclick={() => showCreate = false}>Cancelar</button><button class="primary-button" type="submit" disabled={saving}>{saving ? 'Guardando...' : (editingId ? 'Guardar cambios' : 'Crear empleado')}</button></div></form></div></div>{/if}
