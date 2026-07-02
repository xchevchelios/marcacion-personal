<script>
  import { onMount } from 'svelte'
  import { ApiError, get } from '../lib/api.js'
  let { name, onUnauthorized } = $props()
  let data = $state(null), error = $state('')
  onMount(async () => { try { data = await get('/admin/dashboard/resumen') } catch (cause) { if (cause instanceof ApiError && cause.status === 401) onUnauthorized(); else error = cause.message || 'No se pudo cargar el resumen.' } })
  const cards = [
    ['empleadosActivos', 'Empleados activos'], ['obrasActivas', 'Obras activas'], ['marcacionesHoy', 'Marcaciones hoy'],
    ['excepcionesPendientes', 'Excepciones pendientes'], ['llegadasTardiasHoy', 'Llegadas tardías'], ['salidasTardiasHoy', 'Salidas tardías'],
  ]
</script>
<section class="view-stack"><div class="view-heading"><div><span class="eyebrow">Panel administrativo</span><h2>Hola, {name}</h2><p>Estado operativo actualizado del sistema de marcación.</p></div></div>{#if error}<div class="alert error">{error}</div>{/if}<div class="summary-grid">{#each cards as card}<article><span>{card[1]}</span><strong>{data ? data[card[0]] : '—'}</strong></article>{/each}</div></section>
