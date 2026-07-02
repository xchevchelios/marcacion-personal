<script>
  import { onMount } from 'svelte'
  import L from 'leaflet'
  import 'leaflet/dist/leaflet.css'

  let { initial = [], onChange } = $props()
  let mapElement
  let map
  let layer
  let points = $state([])
  let search = $state('')
  let searchError = $state('')
  let searching = $state(false)

  function redraw() {
    if (!map || !layer) return
    layer.clearLayers()
    points.forEach((point, index) => L.circleMarker([point.lat, point.lng], { radius: 7, color: '#126744', fillColor: '#27a875', fillOpacity: 1 }).bindTooltip(`Punto ${index + 1}`).addTo(layer))
    if (points.length >= 2) L.polyline(points.map((point) => [point.lat, point.lng]), { color: '#16845b', weight: 2 }).addTo(layer)
    if (points.length >= 3) L.polygon(points.map((point) => [point.lat, point.lng]), { color: '#16845b', fillOpacity: .12 }).addTo(layer)
  }
  function update(next) { points = next; redraw(); onChange([...points]) }
  function remove(index) { update(points.filter((_, current) => current !== index)) }
  function clear() { update([]) }
  async function findLocation(event) {
    event.preventDefault(); searchError = ''
    if (!search.trim()) return
    searching = true
    try {
      const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(search.trim())}`, { headers: { 'Accept-Language': 'es' } })
      if (!response.ok) throw new Error()
      const results = await response.json()
      if (!results.length) { searchError = 'No se encontró la ubicación.'; return }
      map.setView([Number(results[0].lat), Number(results[0].lon)], 16)
    } catch { searchError = 'No se pudo consultar OpenStreetMap.' }
    finally { searching = false }
  }
  onMount(() => {
    points = [...initial]
    map = L.map(mapElement).setView([-25.2867, -57.3333], 11)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap contributors' }).addTo(map)
    layer = L.layerGroup().addTo(map)
    map.on('click', ({ latlng }) => update([...points, { lat: Number(latlng.lat.toFixed(7)), lng: Number(latlng.lng.toFixed(7)) }]))
    redraw()
    setTimeout(() => map.invalidateSize(), 0)
    return () => map.remove()
  })
</script>

<div class="map-picker"><form class="map-search" onsubmit={findLocation}><input bind:value={search} placeholder="Buscar dirección o lugar" /><button class="secondary-button" type="submit" disabled={searching}>{searching ? 'Buscando…' : 'Buscar'}</button></form>{#if searchError}<small class="map-error">{searchError}</small>{/if}<p>Hacé clic en el mapa para marcar al menos tres vértices de la geocerca.</p><div class="map-canvas" bind:this={mapElement}></div><div class="vertex-header"><strong>{points.length} puntos seleccionados</strong>{#if points.length}<button type="button" onclick={clear}>Limpiar</button>{/if}</div>{#if points.length}<div class="vertex-list">{#each points as point, index}<div><span>{index + 1}. {point.lat.toFixed(6)}, {point.lng.toFixed(6)}</span><button type="button" aria-label={`Eliminar punto ${index + 1}`} onclick={() => remove(index)}>×</button></div>{/each}</div>{/if}</div>
