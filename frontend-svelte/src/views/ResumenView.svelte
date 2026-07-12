<script>
  import { onMount } from 'svelte'
  import { ApiError, get } from '../lib/api.js'

  let { name, onUnauthorized } = $props()
  let data = $state(null)
  let error = $state('')
  let selected = $state(null)
  let loadingDetail = $state(false)
  let empleados = $state([])
  let obras = $state([])
  let asistencias = $state([])
  let excepciones = $state([])

  const list = (value) => Array.isArray(value) ? value : (value?.content ?? [])
  const todayKey = () => new Date().toISOString().slice(0, 10)
  const isToday = (value) => value && new Date(value).toISOString().slice(0, 10) === todayKey()
  const formatDateTime = (value) => value ? new Intl.DateTimeFormat('es-PY', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '-'

  const cards = [
    ['empleadosActivos', 'Empleados activos', 'empleados'],
    ['obrasActivas', 'Obras activas', 'obras'],
    ['marcacionesHoy', 'Marcaciones hoy', 'marcaciones'],
    ['excepcionesPendientes', 'Excepciones pendientes', 'excepciones'],
    ['llegadasTardiasHoy', 'Llegadas tardias', 'llegadas'],
    ['salidasTardiasHoy', 'Salidas tardias', 'salidas'],
  ]

  const detailTitle = $derived(cards.find((card) => card[2] === selected)?.[1] ?? '')
  const detailRows = $derived.by(() => {
    if (selected === 'empleados') return empleados.filter((item) => item.activo)
    if (selected === 'obras') return obras.filter((item) => item.activa)
    if (selected === 'marcaciones') return asistencias.filter((item) => isToday(item.fechaHoraReal))
    if (selected === 'excepciones') return excepciones
    if (selected === 'llegadas') return asistencias.filter((item) => isToday(item.fechaHoraReal) && item.requiereRevision && item.tipoMarcacion === 'ENTRADA')
    if (selected === 'salidas') return asistencias.filter((item) => isToday(item.fechaHoraReal) && item.requiereRevision && item.tipoMarcacion === 'SALIDA')
    return []
  })

  async function loadSummary() {
    try {
      data = await get('/admin/dashboard/resumen')
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 401) onUnauthorized()
      else error = cause.message || 'No se pudo cargar el resumen.'
    }
  }

  async function openDetail(type) {
    selected = type
    loadingDetail = true
    error = ''
    try {
      const requests = []
      if (!empleados.length && type === 'empleados') requests.push(get('/admin/empleados').then((value) => empleados = list(value)))
      if (!obras.length && type === 'obras') requests.push(get('/admin/obras').then((value) => obras = list(value)))
      if (!asistencias.length && ['marcaciones', 'llegadas', 'salidas'].includes(type)) requests.push(get('/admin/dashboard/asistencias').then((value) => asistencias = list(value)))
      if (!excepciones.length && type === 'excepciones') requests.push(get('/admin/dashboard/excepciones').then((value) => excepciones = list(value)))
      await Promise.all(requests)
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 401) onUnauthorized()
      else error = cause.message || 'No se pudo cargar el detalle.'
    } finally {
      loadingDetail = false
    }
  }

  onMount(loadSummary)
</script>

<section class="view-stack">
  <div class="view-heading">
    <div><span class="eyebrow">Panel administrativo</span><h2>Hola, {name}</h2><p>Estado operativo actualizado del sistema de marcacion.</p></div>
  </div>
  {#if error}<div class="alert error">{error}</div>{/if}
  <div class="summary-grid">
    {#each cards as card}
      <button class:active={selected === card[2]} class="summary-card" type="button" onclick={() => openDetail(card[2])}>
        <span>{card[1]}</span><strong>{data ? data[card[0]] : '-'}</strong>
      </button>
    {/each}
  </div>

  {#if selected}
    <section class="panel metric-detail">
      <div class="detail-header">
        <div><span class="eyebrow">Detalle</span><h2>{detailTitle}</h2><p>{detailRows.length} registros</p></div>
        <button class="secondary-button" type="button" onclick={() => openDetail(selected)} disabled={loadingDetail}>Actualizar</button>
      </div>
      <div class="table-wrap excel-wrap">
        <table class="excel-table">
          {#if selected === 'empleados'}
            <thead><tr><th>Empleado</th><th>Documento</th><th>Correo</th><th>Rol</th><th>Aprobacion</th></tr></thead>
            <tbody>{#if loadingDetail}<tr><td colspan="5" class="table-message">Cargando...</td></tr>{:else if !detailRows.length}<tr><td colspan="5" class="table-message">Sin datos.</td></tr>{:else}{#each detailRows as row}<tr><td>{row.nombreCompleto || '-'}</td><td>{row.documentoIdentidad || '-'}</td><td>{row.correo || '-'}</td><td>{row.rol || '-'}</td><td>{row.estadoAprobacion || '-'}</td></tr>{/each}{/if}</tbody>
          {:else if selected === 'obras'}
            <thead><tr><th>Codigo de obra</th><th>Nombre</th><th>Ubicacion</th><th>Descripcion</th><th>Estado</th></tr></thead>
            <tbody>{#if loadingDetail}<tr><td colspan="5" class="table-message">Cargando...</td></tr>{:else if !detailRows.length}<tr><td colspan="5" class="table-message">Sin datos.</td></tr>{:else}{#each detailRows as row}<tr><td>{row.codigoSap || '-'}</td><td>{row.nombre || '-'}</td><td>{row.ubicacion || '-'}</td><td>{row.descripcion || '-'}</td><td>{row.activa ? 'Activa' : 'Inactiva'}</td></tr>{/each}{/if}</tbody>
          {:else}
            <thead><tr><th>Codigo de obra</th><th>Empleado</th><th>Hora de marcacion</th><th>Tipo</th><th>Estado</th><th>Motivo</th><th>Dispositivo</th></tr></thead>
            <tbody>{#if loadingDetail}<tr><td colspan="7" class="table-message">Cargando...</td></tr>{:else if !detailRows.length}<tr><td colspan="7" class="table-message">Sin datos.</td></tr>{:else}{#each detailRows as row}<tr><td>{row.obraId || '-'}</td><td>{row.empleadoNombre || '-'}</td><td>{formatDateTime(row.fechaHoraReal)}</td><td>{row.tipoMarcacion || '-'}</td><td>{row.requiereRevision ? 'Revision' : 'Conforme'}</td><td>{row.motivoRevision || '-'}</td><td>{row.deviceId || '-'}</td></tr>{/each}{/if}</tbody>
          {/if}
        </table>
      </div>
    </section>
  {/if}
</section>
