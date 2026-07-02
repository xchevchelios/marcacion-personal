<script>
  import { onMount } from 'svelte'
  import { ApiError, get } from '../lib/api.js'

  let { onUnauthorized } = $props()
  let records = $state([]), filtered = $state([]), obras = $state([])
  let obraId = $state(''), desde = $state(''), hasta = $state(''), query = $state('')
  let loading = $state(true), error = $state('')
  const list = (data) => Array.isArray(data) ? data : (data?.content ?? [])
  const formatDateTime = (value) => value ? new Intl.DateTimeFormat('es-PY', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—'

  function fail(cause, fallback) {
    if (cause instanceof ApiError && cause.status === 401) return onUnauthorized()
    if (cause instanceof ApiError && cause.status === 403) { error = 'No tenés permisos para acceder a esta información.'; return }
    error = cause.message || fallback
  }
  function applyFilters() {
    const term = query.trim().toLocaleLowerCase('es')
    const end = hasta ? new Date(`${hasta}T23:59:59`) : null
    filtered = records.filter((item) => {
      const timestamp = item.horaEntrada || item.fechaHoraReal
      return (!obraId || String(item.obraId) === obraId)
        && (!desde || (timestamp && new Date(timestamp) >= new Date(`${desde}T00:00:00`)))
        && (!end || (timestamp && new Date(timestamp) <= end))
        && (!term || [item.empleadoNombre, item.empleadoDocumento, item.obraNombre].some((value) => String(value ?? '').toLocaleLowerCase('es').includes(term)))
    })
  }
  function clearFilters() { obraId = ''; desde = ''; hasta = ''; query = ''; applyFilters() }
  async function load() {
    loading = true; error = ''
    try { const data = await Promise.all([get('/admin/dashboard/asistencias'), get('/admin/obras')]); records = list(data[0]); obras = list(data[1]); applyFilters() }
    catch (cause) { fail(cause, 'No se pudieron cargar las asistencias.') }
    finally { loading = false }
  }
  onMount(load)
</script>

<section class="view-stack">
  <div class="view-heading"><div><span class="eyebrow">Control horario</span><h2>Consolidado de asistencias</h2><p>Entradas, salidas, horas trabajadas y eventos que necesitan revisión.</p></div><button class="secondary-button" type="button" onclick={load}>Actualizar</button></div>
  <div class="filter-panel"><label class="wide-field">Buscar<input type="search" bind:value={query} oninput={applyFilters} placeholder="Empleado, documento u obra" /></label><label>Obra<select bind:value={obraId} onchange={applyFilters}><option value="">Todas las obras</option>{#each obras as obra}<option value={obra.codigoSap}>{obra.codigoSap} — {obra.nombre}</option>{/each}</select></label><label>Desde<input type="date" bind:value={desde} onchange={applyFilters} /></label><label>Hasta<input type="date" bind:value={hasta} onchange={applyFilters} /></label><button class="secondary-button filter-clear" type="button" onclick={clearFilters}>Limpiar</button></div>
  {#if error}<div class="alert error">{error}</div>{/if}
  <div class="panel table-wrap"><table><thead><tr><th>Empleado</th><th>Obra</th><th>Entrada</th><th>Salida</th><th>Horas</th><th>Estado</th><th>Motivo</th></tr></thead><tbody>
    {#if loading}<tr><td colspan="7" class="table-message">Cargando asistencias…</td></tr>
    {:else if !filtered.length}<tr><td colspan="7" class="table-message">No hay asistencias que coincidan con los filtros.</td></tr>
    {:else}{#each filtered as record (record.id)}<tr><td><div class="employee-cell"><span class="mini-avatar">{record.empleadoNombre?.[0] || '?'}</span><div><strong>{record.empleadoNombre || '—'}</strong><small>{record.empleadoDocumento || '—'}</small></div></div></td><td>{record.obraNombre || record.obraId || '—'}</td><td>{formatDateTime(record.horaEntrada)}</td><td>{formatDateTime(record.horaSalida)}</td><td>{record.horasTrabajadas != null ? `${record.horasTrabajadas} h` : '—'}</td><td><span class="pill {record.requiereRevision ? 'pendiente' : 'aprobado'}">{record.requiereRevision ? 'Pendiente' : 'Conforme'}</span></td><td class="reason-cell">{record.motivoRevision || '—'}</td></tr>{/each}{/if}
  </tbody></table></div><span class="result-count">{filtered.length} registros mostrados</span>
</section>
