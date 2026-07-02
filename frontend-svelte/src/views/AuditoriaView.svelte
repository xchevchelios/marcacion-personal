<script>
  import { onMount } from 'svelte'
  import { ApiError, get } from '../lib/api.js'
  let { onUnauthorized } = $props()
  let events = $state([]), loading = $state(true), error = $state('')
  const format = (value) => value ? new Intl.DateTimeFormat('es-PY', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value)) : '—'
  async function load() { loading = true; try { const data = await get('/admin/auditoria?size=100'); events = data.content || [] } catch (cause) { if (cause instanceof ApiError && cause.status === 401) onUnauthorized(); else error = cause.status === 403 ? 'Acceso exclusivo para SOPORTE.' : cause.message } finally { loading = false } }
  onMount(load)
</script>
<section class="view-stack"><div class="view-heading"><div><span class="eyebrow">SOPORTE</span><h2>Registro de auditoría</h2><p>Acciones administrativas y decisiones sensibles del sistema.</p></div><button class="secondary-button" onclick={load}>Actualizar</button></div>{#if error}<div class="alert error">{error}</div>{/if}<div class="panel table-wrap"><table><thead><tr><th>Fecha</th><th>Actor</th><th>Acción</th><th>Entidad</th><th>Identificador</th><th>Detalle</th></tr></thead><tbody>{#if loading}<tr><td colspan="6" class="table-message">Cargando auditoría…</td></tr>{:else if !events.length}<tr><td colspan="6" class="table-message">Todavía no existen eventos.</td></tr>{:else}{#each events as event (event.id)}<tr><td>{format(event.createdAt)}</td><td><strong>{event.actor}</strong></td><td><span class="pill neutral">{event.action}</span></td><td>{event.entityType}</td><td>{event.entityId}</td><td>{event.details || '—'}</td></tr>{/each}{/if}</tbody></table></div></section>
