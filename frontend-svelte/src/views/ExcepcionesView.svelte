<script>
  import { onMount } from 'svelte'
  import { canManageEmployees } from '../lib/auth.js'
  import { ApiError, get, patch } from '../lib/api.js'

  let { role, onUnauthorized } = $props()
  let exceptions = $state([]), loading = $state(true), error = $state('')
  let selected = $state(null), note = $state(''), saving = $state(false)
  let historyMode = $state(false)
  const list = (data) => Array.isArray(data) ? data : (data?.content ?? [])
  const formatDateTime = (value) => value ? new Intl.DateTimeFormat('es-PY', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—'
  function exceptionLabel(item) { const reason = String(item.motivoRevision || '').toLowerCase(); if (item.tipoMarcacion === 'SALIDA' || reason.includes('salida')) return 'Salida tardía'; if (reason.includes('llegada')) return 'Llegada tardía'; return item.tipoMarcacion === 'SALIDA' ? 'Salida' : 'Entrada' }
  function fail(cause, fallback) { if (cause instanceof ApiError && cause.status === 401) return onUnauthorized(); if (cause instanceof ApiError && cause.status === 403) { error = 'No tenés permisos para realizar esta acción.'; return } error = cause.message || fallback }
  async function load() { loading = true; error = ''; try { exceptions = list(await get(historyMode ? '/admin/dashboard/excepciones/historial' : '/admin/dashboard/excepciones')) } catch (cause) { fail(cause, 'No se pudieron cargar las excepciones.') } finally { loading = false } }
  async function toggleHistory() { historyMode = !historyMode; await load() }
  function open(item) { selected = item; note = ''; error = '' }
  async function resolve(approve) {
    if (!note.trim()) { error = 'La nota de resolución es obligatoria.'; return }
    saving = true; error = ''
    try { const params = new URLSearchParams({ aprobar: String(approve), nota: note.trim() }); await patch(`/admin/dashboard/excepciones/${selected.id}/resolver?${params}`); selected = null; await load() }
    catch (cause) { fail(cause, 'No se pudo resolver la excepción.') }
    finally { saving = false }
  }
  onMount(load)
</script>

<section class="view-stack"><div class="view-heading"><div><span class="eyebrow">Revisión</span><h2>{historyMode ? 'Historial de excepciones' : 'Excepciones pendientes'}</h2><p>{historyMode ? 'Decisiones tomadas, responsables y notas de resolución.' : 'Llegadas y salidas tardías que requieren una decisión de RRHH.'}</p></div><div class="row-actions"><span class="count-badge">{exceptions.length}</span><button class="secondary-button" type="button" onclick={toggleHistory}>{historyMode ? 'Ver pendientes' : 'Ver historial'}</button><button class="secondary-button" type="button" onclick={load}>Actualizar</button></div></div>
  {#if error && !selected}<div class="alert error">{error}</div>{/if}
  <div class="panel table-wrap"><table><thead><tr><th>Empleado</th><th>Obra</th><th>Fecha y hora</th><th>Tipo</th><th>Motivo</th><th>Estado</th><th>{historyMode ? 'Resolución' : 'Acción'}</th></tr></thead><tbody>{#if loading}<tr><td colspan="7" class="table-message">Cargando excepciones…</td></tr>{:else if !exceptions.length}<tr><td colspan="7" class="table-message success-message">{historyMode ? 'Sin historial disponible.' : 'Sin excepciones pendientes ✓'}</td></tr>{:else}{#each exceptions as item (item.id)}<tr><td><strong>{item.empleadoNombre || '—'}</strong></td><td>{item.obraNombre || item.obraId || '—'}</td><td>{formatDateTime(item.fechaHoraReal)}</td><td><span class="pill {item.tipoMarcacion === 'SALIDA' ? 'neutral' : 'pendiente'}">{exceptionLabel(item)}</span></td><td class="reason-cell">{item.motivoRevision || '—'}</td><td><span class="pill {item.estadoRevision === 'APROBADA' ? 'aprobado' : 'pendiente'}">{item.estadoRevision || 'PENDIENTE'}</span></td><td>{#if historyMode}<span title={item.notaResolucion}>{item.resueltoPor || '—'} · {formatDateTime(item.fechaResolucion)}</span>{:else}<button class="primary-button compact" type="button" disabled={!canManageEmployees(role)} title={canManageEmployees(role) ? 'Resolver excepción' : 'Solo RRHH/Admin/SOPORTE'} onclick={() => open(item)}>Resolver</button>{/if}</td></tr>{/each}{/if}</tbody></table></div>
</section>

{#if selected}<div class="modal-backdrop" role="presentation" onclick={(event) => event.target === event.currentTarget && (selected = null)}><div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="exception-title"><div class="detail-header"><div><span class="eyebrow">Resolver excepción</span><h2 id="exception-title">{selected.empleadoNombre}</h2><p>{exceptionLabel(selected)} · {selected.obraNombre}</p></div><button class="icon-button" aria-label="Cerrar" onclick={() => selected = null}>×</button></div><div class="exception-summary"><strong>{selected.motivoRevision}</strong><span>{formatDateTime(selected.fechaHoraReal)}</span></div><form class="modal-form" onsubmit={(event) => event.preventDefault()}><label>Nota de resolución<textarea bind:value={note} rows="4" placeholder="Describí el motivo de la decisión…"></textarea></label>{#if error}<div class="alert error">{error}</div>{/if}<div class="modal-actions split-actions"><button class="reject-button" type="button" disabled={saving} onclick={() => resolve(false)}>Rechazar</button><button class="primary-button" type="button" disabled={saving} onclick={() => resolve(true)}>{saving ? 'Procesando…' : 'Aprobar'}</button></div></form></div></div>{/if}
