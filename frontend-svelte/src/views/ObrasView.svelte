<script>
  import { onMount } from 'svelte'
  import { ApiError, get, post, put, del } from '../lib/api.js'
  import MapPicker from '../lib/components/MapPicker.svelte'
  import { canManageEmployees } from '../lib/auth.js'

  let { role, onOpenEmployee, onUnauthorized } = $props()
  let obras = $state([])
  let filtered = $state([])
  let query = $state('')
  let loading = $state(true)
  let error = $state('')
  let selected = $state(null)
  let assignments = $state([])
  let detailLoading = $state(false)
  let showCreate = $state(false), saving = $state(false), formError = $state('')
  let form = $state({ codigoSap: '', nombre: '', ubicacion: '', descripcion: '', activa: true, vertices: [] })
  let editingCode = $state(null)

  const list = (data) => Array.isArray(data) ? data : (data?.content ?? [])
  const formatDate = (value) => value ? new Intl.DateTimeFormat('es-PY').format(new Date(`${value}T00:00:00`)) : '—'
  function fail(cause, fallback) {
    if (cause instanceof ApiError && cause.status === 401) return onUnauthorized()
    if (cause instanceof ApiError && cause.status === 403) { error = 'No tenés permisos para acceder a esta información.'; return }
    error = cause.message || fallback
  }
  function filter() {
    const term = query.trim().toLocaleLowerCase('es')
    filtered = term ? obras.filter((obra) => [obra.codigoSap, obra.nombre, obra.ubicacion].some((value) => String(value ?? '').toLocaleLowerCase('es').includes(term))) : obras
  }
  async function load() {
    loading = true; error = ''
    try { obras = list(await get('/admin/obras')); filter() }
    catch (cause) { fail(cause, 'No se pudieron cargar las obras.') }
    finally { loading = false }
  }
  async function openDetail(obra) {
    selected = obra; detailLoading = true; error = ''
    try { assignments = list(await get('/admin/asignaciones')).filter((item) => String(item.obraId) === String(obra.codigoSap)) }
    catch (cause) { fail(cause, 'No se pudieron cargar los empleados asignados.') }
    finally { detailLoading = false }
  }
  function openCreate() { editingCode = null; form = { codigoSap: '', nombre: '', ubicacion: '', descripcion: '', activa: true, vertices: [] }; formError = ''; showCreate = true }
  async function openEdit(obra) {
    formError = ''
    try { const detail = await get(`/admin/obras/${encodeURIComponent(obra.codigoSap)}`); editingCode = obra.codigoSap; form = { codigoSap: detail.codigoSap, nombre: detail.nombre, ubicacion: detail.ubicacion || '', descripcion: detail.descripcion || '', activa: detail.activa, vertices: detail.vertices || [] }; showCreate = true }
    catch (cause) { fail(cause, 'No se pudo cargar la obra.') }
  }
  async function deactivate(obra) {
    if (!confirm(`¿Desactivar la obra ${obra.nombre}?`)) return
    try { await del(`/admin/obras/${encodeURIComponent(obra.codigoSap)}`); if (selected?.codigoSap === obra.codigoSap) selected = null; await load() }
    catch (cause) { fail(cause, 'No se pudo desactivar la obra.') }
  }
  async function createWork(event) {
    event.preventDefault(); formError = ''
    const codigoSap = form.codigoSap.trim().toUpperCase()
    if (!codigoSap || !/^[A-Z0-9]+(?:-[A-Z0-9]+)*$/.test(codigoSap)) { formError = 'El código SAP debe contener letras, números y guiones simples.'; return }
    if (!form.nombre.trim()) { formError = 'El nombre de la obra es obligatorio.'; return }
    if (form.vertices.length < 3) { formError = 'Marcá al menos tres puntos para formar la geocerca.'; return }
    saving = true
    try { const payload = { ...form, codigoSap, nombre: form.nombre.trim(), ubicacion: form.ubicacion.trim(), descripcion: form.descripcion.trim() }; if (editingCode) await put(`/admin/obras/${encodeURIComponent(editingCode)}`, payload); else await post('/admin/obras', payload); showCreate = false; await load() }
    catch (cause) { formError = cause.message || 'No se pudo crear la obra.' }
    finally { saving = false }
  }
  onMount(load)
</script>

<section class="view-stack">
  <div class="view-heading"><div><span class="eyebrow">Proyectos</span><h2>Obras registradas</h2><p>Consultá los datos de cada obra y su personal asignado.</p></div><div class="row-actions"><button class="secondary-button" type="button" onclick={load} disabled={loading}>Actualizar</button>{#if canManageEmployees(role)}<button class="primary-button" type="button" onclick={openCreate}>Nueva obra</button>{/if}</div></div>
  <div class="toolbar"><label class="search-field"><span>⌕</span><input type="search" bind:value={query} oninput={filter} placeholder="Buscar por código SAP, nombre o ubicación" /></label><span class="result-count">{filtered.length} obras</span></div>
  {#if error}<div class="alert error">{error}</div>{/if}
  <div class="panel table-wrap"><table><thead><tr><th>Código SAP</th><th>Nombre</th><th>Ubicación</th><th>Descripción</th><th>Estado</th>{#if canManageEmployees(role)}<th>Acciones</th>{/if}</tr></thead><tbody>
    {#if loading}<tr><td colspan="6" class="table-message">Cargando obras…</td></tr>
    {:else if !filtered.length}<tr><td colspan="6" class="table-message">No se encontraron obras.</td></tr>
    {:else}{#each filtered as obra (obra.codigoSap)}<tr class:selected={selected?.codigoSap === obra.codigoSap} tabindex="0" onclick={() => openDetail(obra)} onkeydown={(event) => event.key === 'Enter' && openDetail(obra)}><td><strong>{obra.codigoSap}</strong></td><td>{obra.nombre || '—'}</td><td>{obra.ubicacion || '—'}</td><td>{obra.descripcion || '—'}</td><td><span class="status-dot" class:enabled={obra.activa}></span>{obra.activa ? 'Activa' : 'Inactiva'}</td>{#if canManageEmployees(role)}<td><div class="row-actions"><button type="button" onclick={(event) => { event.stopPropagation(); openEdit(obra) }}>Editar</button>{#if obra.activa}<button class="danger-link" type="button" onclick={(event) => { event.stopPropagation(); deactivate(obra) }}>Desactivar</button>{/if}</div></td>{/if}</tr>{/each}{/if}
  </tbody></table></div>
  {#if selected}<section class="detail-panel"><div class="detail-header"><div><span class="eyebrow">Ficha de obra</span><h2>{selected.nombre}</h2><p>{selected.codigoSap}</p></div><button class="secondary-button" type="button" onclick={() => selected = null}>Cerrar</button></div>
    <div class="detail-grid"><article><span>Código SAP</span><strong>{selected.codigoSap}</strong></article><article><span>Ubicación</span><strong>{selected.ubicacion || '—'}</strong></article><article><span>Estado</span><strong>{selected.activa ? 'Activa' : 'Inactiva'}</strong></article><article><span>Descripción</span><strong>{selected.descripcion || '—'}</strong></article></div>
    <div class="detail-section"><h3>Empleados asignados</h3>{#if detailLoading}<p class="table-message">Cargando empleados…</p>{:else if !assignments.length}<p class="empty-inline">Esta obra todavía no tiene empleados asignados.</p>{:else}<div class="table-wrap"><table><thead><tr><th>Empleado</th><th>Código SAP</th><th>Desde</th><th>Entrada</th><th>Salida</th></tr></thead><tbody>{#each assignments as assignment}<tr class="clickable-row" tabindex="0" onclick={() => onOpenEmployee(assignment.empleadoId)} onkeydown={(event) => event.key === 'Enter' && onOpenEmployee(assignment.empleadoId)}><td><strong>{assignment.empleadoNombre || '—'}</strong></td><td>{assignment.obraId}</td><td>{formatDate(assignment.fechaInicio)}</td><td>{assignment.horaEntrada || '08:00'}</td><td>{assignment.horaSalida || '17:00'}</td></tr>{/each}</tbody></table></div>{/if}</div>
  </section>{/if}
</section>

{#if showCreate}<div class="modal-backdrop" role="presentation" onclick={(event) => event.target === event.currentTarget && (showCreate = false)}><div class="modal-card map-modal" role="dialog" aria-modal="true" aria-labelledby="work-create-title"><div class="detail-header"><div><span class="eyebrow">Proyecto</span><h2 id="work-create-title">{editingCode ? 'Editar obra' : 'Nueva obra'}</h2><p>Definí sus datos y la geocerca sobre OpenStreetMap.</p></div><button class="icon-button" aria-label="Cerrar" onclick={() => showCreate = false}>×</button></div><form class="modal-form" onsubmit={createWork}><div class="form-grid"><label>Código SAP<input bind:value={form.codigoSap} disabled={Boolean(editingCode)} placeholder="OBRA-2026-001" maxlength="100" /></label><label>Nombre<input bind:value={form.nombre} /></label></div><label>Ubicación<input bind:value={form.ubicacion} placeholder="Dirección descriptiva" /></label><label>Descripción<textarea bind:value={form.descripcion} rows="2"></textarea></label><label class="checkbox-field"><input type="checkbox" bind:checked={form.activa} /> Obra activa</label><MapPicker initial={form.vertices} onChange={(vertices) => form.vertices = vertices} />{#if formError}<div class="alert error">{formError}</div>{/if}<div class="modal-actions"><button class="secondary-button" type="button" onclick={() => showCreate = false}>Cancelar</button><button class="primary-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : (editingCode ? 'Guardar cambios' : 'Crear obra')}</button></div></form></div></div>{/if}
