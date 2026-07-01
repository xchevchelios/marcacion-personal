// ============================================================
// EPESA — Módulo: Obras  (solo RRHH/Admin)
// ============================================================

let obraListCache = [];
let obraAsignacionesCache = [];
let obraVerticesActual = []; // Para almacenar vértices del modal
let obraMap = null; // Leaflet map instance
let obraMapMarkers = []; // Array de marcadores en el mapa

async function cargarObras() {
  if (isLoading('obras')) return;
  setLoadingKey('obras', true);

  const tbody = document.getElementById('tbody-obras');
  tbody.innerHTML = loadingRows(6);

  try {
    const res = await apiFetch('/admin/obras');
    if (!res) return;

    if (!res.ok) {
      tbody.innerHTML = errorRow(6, 'cargarObras()');
      return;
    }

    const data = await res.json();
    obraListCache = Array.isArray(data) ? data : (data.content ?? []);
    tbody.innerHTML = '';

    if (!obraListCache.length) {
      tbody.innerHTML = emptyRow(6, 'Sin obras registradas.');
      return;
    }

    obraListCache.forEach((obra, i) => {
      const tr = document.createElement('tr');
      tr.className = 'fade-in';
      tr.style.animationDelay = `${i * 25}ms`;
      tr.title = 'Click para ver la ficha de la obra';
      tr.addEventListener('click', () => mostrarDetalleObra(obra.codigoSap));

      tr.innerHTML = `
        <td class="fw-medium">${escStr(obra.codigoSap) || '—'}</td>
        <td class="fw-medium">${escStr(obra.nombre) || '—'}</td>
        <td>${escStr(obra.ubicacion) || '—'}</td>
        <td class="td-truncate" title="${escStr(obra.descripcion)}">${escStr(obra.descripcion) || '—'}</td>
        <td>${activeBadge(obra.activa)}</td>
        <td class="td-actions rrhh-only">
          <button class="btn-icon" title="Editar" data-admin-action onclick="event.stopPropagation(); abrirModalEditarObra('${escStr(obra.codigoSap)}')">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </button>
          <button class="btn-icon danger" title="Eliminar" data-admin-action onclick="event.stopPropagation(); confirmarEliminar('obra','${escStr(obra.codigoSap)}','${escStr(obra.nombre)}')">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
              <path d="M10 11v6M14 11v6M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
            </svg>
          </button>
        </td>`;
      tbody.appendChild(tr);
    });

  } catch {
    tbody.innerHTML = errorRow(6, 'cargarObras()');
  } finally {
    setLoadingKey('obras', false);
    applyRBAC();
  }
}

// ── Modal Obra ─────────────────────────────────────────────

async function mostrarDetalleObra(codigoSap) {
  const obra = obraListCache.find(o => String(o.codigoSap) === String(codigoSap));
  if (!obra) return;

  const panel = document.getElementById('obra-detalle-panel');
  document.getElementById('obra-detalle-nombre').textContent = obra.nombre || 'Obra sin nombre';
  document.getElementById('obra-detalle-subtitulo').textContent = obra.codigoSap || 'Sin código SAP';
  document.getElementById('obra-detalle-codigo').textContent = obra.codigoSap || '—';
  document.getElementById('obra-detalle-ubicacion').textContent = obra.ubicacion || '—';
  document.getElementById('obra-detalle-estado').textContent = obra.activa ? 'Activa' : 'Inactiva';
  document.getElementById('obra-detalle-descripcion').textContent = obra.descripcion || '—';

  const tbody = document.getElementById('tbody-obra-empleados');
  tbody.innerHTML = loadingRows(5);
  panel.classList.remove('hidden');
  panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

  const asignaciones = await _obtenerAsignacionesObra();
  const empleados = asignaciones.filter(a => String(a.obraId) === String(codigoSap));

  if (!empleados.length) {
    tbody.innerHTML = emptyRow(5, 'Esta obra todavía no tiene empleados asignados.');
    return;
  }

  tbody.innerHTML = empleados.map(a => `
    <tr title="Click para abrir la ficha del empleado" onclick="abrirFichaEmpleadoDesdeObra('${escStr(a.empleadoId)}')">
      <td>
        <div class="cell-avatar">
          <span class="avatar-chip">${initials(a.empleadoNombre)}</span>
          <span class="fw-medium">${escStr(a.empleadoNombre) || '—'}</span>
        </div>
      </td>
      <td>${escStr(a.obraId) || '—'}</td>
      <td>${fmtDate(a.fechaInicio)}</td>
      <td>${escStr(a.horaEntrada) || '08:00'}</td>
      <td>${escStr(a.horaSalida) || '17:00'}</td>
    </tr>
  `).join('');
}

async function abrirFichaEmpleadoDesdeObra(empleadoId) {
  if (!empleadoId) return;
  await goToView('empleados');
  await mostrarDetalleEmpleado(empleadoId);
}

function cerrarDetalleObra() {
  document.getElementById('obra-detalle-panel')?.classList.add('hidden');
}

async function _obtenerAsignacionesObra() {
  if (obraAsignacionesCache.length) return obraAsignacionesCache;
  try {
    const res = await apiFetch('/admin/asignaciones');
    if (!res?.ok) return [];
    const data = await res.json();
    obraAsignacionesCache = Array.isArray(data) ? data : (data.content ?? []);
    return obraAsignacionesCache;
  } catch {
    return [];
  }
}

function invalidarCacheDetalleObras() {
  obraAsignacionesCache = [];
  cerrarDetalleObra();
}

function inicializarMapaObra() {
  // Si el mapa ya existe, limpiar
  if (obraMap) {
    obraMap.remove();
  }
  
  obraMapMarkers = [];
  
  // Crear mapa centrado en Asunción, Paraguay
  obraMap = L.map('obra-map').setView([-25.2637, -57.5759], 12);
  
  // Agregar tile layer de OpenFreeMap (basado en OpenStreetMap)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 19
  }).addTo(obraMap);
  
  // Manejar clics en el mapa para agregar vértices
  obraMap.on('click', (e) => {
    const { lat, lng } = e.latlng;
    agregarVerticeDesdeMapaObra(lat, lng);
  });
}

function agregarVerticeDesdeMapaObra(lat, lng) {
  obraVerticesActual.push({ lat, lng });
  
  // Agregar marcador al mapa
  const marker = L.circleMarker([lat, lng], {
    radius: 6,
    fillColor: '#2563eb',
    color: '#1e40af',
    weight: 2,
    opacity: 1,
    fillOpacity: 0.8
  }).addTo(obraMap);
  
  // Popup con número del vértice
  marker.bindPopup(`<strong>Punto ${obraVerticesActual.length}</strong><br>Lat: ${lat.toFixed(6)}<br>Lng: ${lng.toFixed(6)}`);
  
  obraMapMarkers.push({ marker, lat, lng, index: obraVerticesActual.length - 1 });
  
  actualizarListaVertices();
}

function buscarUbicacionObra() {
  const searchInput = document.getElementById('obra-search');
  const query = searchInput.value.trim();
  
  if (!query) {
    alert('Ingresá una ubicación para buscar');
    return;
  }
  
  // Usar Nominatim API (geocodificación abierta)
  fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`)
    .then(res => res.json())
    .then(data => {
      if (!data || data.length === 0) {
        alert('Ubicación no encontrada');
        return;
      }
      
      const result = data[0];
      const lat = parseFloat(result.lat);
      const lng = parseFloat(result.lon);
      
      // Centrar el mapa en la ubicación encontrada
      if (obraMap) {
        obraMap.setView([lat, lng], 15);
        
        // Agregar marcador de búsqueda temporal
        L.marker([lat, lng], {
          icon: L.icon({
            iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
            shadowSize: [41, 41]
          })
        }).addTo(obraMap).bindPopup(`<strong>${result.display_name}</strong>`).openPopup();
      }
      
      searchInput.value = '';
    })
    .catch(() => alert('Error al buscar la ubicación'));
}

function actualizarListaVertices() {
  const lista = document.getElementById('obra-vertices-list');
  if (!obraVerticesActual.length) {
    lista.innerHTML = '<div style="text-align:center; color:#999; padding:1rem;">Haz clic en el mapa para agregar puntos</div>';
    return;
  }
  
  lista.innerHTML = obraVerticesActual.map((v, idx) => `
    <div style="display:flex; justify-content:space-between; align-items:center; padding:0.5rem; border-bottom:1px solid #ddd; font-size:0.9rem;">
      <span>${idx + 1}. Lat: ${v.lat.toFixed(6)} | Lng: ${v.lng.toFixed(6)}</span>
      <button type="button" class="btn-icon danger" onclick="eliminarVerticeObra(${idx})" title="Eliminar">×</button>
    </div>
  `).join('');
}

function eliminarVerticeObra(idx) {
  obraVerticesActual.splice(idx, 1);
  
  // Remover marcador del mapa
  if (obraMapMarkers[idx] && obraMapMarkers[idx].marker) {
    obraMap.removeLayer(obraMapMarkers[idx].marker);
  }
  
  obraMapMarkers = obraMapMarkers.filter((_, i) => i !== idx).map((m, i) => ({
    ...m,
    index: i
  }));
  
  actualizarListaVertices();
}

function abrirModalNuevaObra() {
  document.getElementById('form-obra').reset();
  document.getElementById('obra-id').value = '';
  document.getElementById('obra-codigo-sap').disabled = false;
  document.getElementById('obra-modal-titulo').textContent = 'Nueva Obra';
  document.getElementById('obra-activa').checked = true;
  document.getElementById('obra-error').classList.add('hidden');
  document.getElementById('obra-search').value = '';
  obraVerticesActual = [];
  
  openModal('modal-obra');
  
  // Inicializar mapa después de que el modal sea visible
  setTimeout(() => {
    inicializarMapaObra();
  }, 100);
}

function abrirModalEditarObra(id) {
  const obra = obraListCache.find(o => String(o.codigoSap) === String(id));
  if (!obra) return;

  document.getElementById('form-obra').reset();
  document.getElementById('obra-modal-titulo').textContent = 'Editar Obra';
  document.getElementById('obra-id').value = obra.codigoSap;
  document.getElementById('obra-codigo-sap').value = obra.codigoSap;
  document.getElementById('obra-codigo-sap').disabled = true;
  document.getElementById('obra-nombre').value = obra.nombre ?? '';
  document.getElementById('obra-ubicacion').value = obra.ubicacion ?? '';
  document.getElementById('obra-descripcion').value = obra.descripcion ?? '';
  document.getElementById('obra-activa').checked = obra.activa ?? true;
  document.getElementById('obra-error').classList.add('hidden');
  document.getElementById('obra-search').value = '';
  obraVerticesActual = [];
  
  openModal('modal-obra');
  
  // Inicializar mapa después de que el modal sea visible
  setTimeout(() => {
    inicializarMapaObra();
    // Cargar vértices de la obra existente
    cargarVerticesDeObra(id);
  }, 100);
}

async function cargarVerticesDeObra(id) {
  try {
    const res = await apiFetch(`/admin/obras/${id}`);
    if (res?.ok) {
      const obra = await res.json();
      if (obra.vertices && obra.vertices.length > 0) {
        obraVerticesActual = obra.vertices;
        
        // Agregar vértices al mapa
        obraVerticesActual.forEach(v => {
          const marker = L.circleMarker([v.lat, v.lng], {
            radius: 6,
            fillColor: '#2563eb',
            color: '#1e40af',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.8
          }).addTo(obraMap);
          
          marker.bindPopup(`<strong>Punto ${obraMapMarkers.length + 1}</strong><br>Lat: ${v.lat.toFixed(6)}<br>Lng: ${v.lng.toFixed(6)}`);
          obraMapMarkers.push({ marker, lat: v.lat, lng: v.lng, index: obraMapMarkers.length });
        });
        
        // Centrar el mapa en el centroide de los vértices
        if (obraVerticesActual.length > 0) {
          const avgLat = obraVerticesActual.reduce((sum, v) => sum + v.lat, 0) / obraVerticesActual.length;
          const avgLng = obraVerticesActual.reduce((sum, v) => sum + v.lng, 0) / obraVerticesActual.length;
          obraMap.setView([avgLat, avgLng], 14);
        }
        
        actualizarListaVertices();
      }
    }
  } catch (e) {
    console.error('Error cargando vértices de obra:', e);
  }
}

function configurarModalObra() {
  document.getElementById('form-obra').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errEl = document.getElementById('obra-error');
    const btnOk = document.getElementById('btn-obra-guardar');
    errEl.classList.add('hidden');

    if (!obraVerticesActual.length) {
      showFormError(errEl, 'Agregá al menos un vértice para la geocerca.');
      return;
    }

    const id          = document.getElementById('obra-id').value.trim();
    const codigoSap   = document.getElementById('obra-codigo-sap').value.trim().toUpperCase();
    const nombre      = document.getElementById('obra-nombre').value.trim();
    const ubicacion   = document.getElementById('obra-ubicacion').value.trim();
    const descripcion = document.getElementById('obra-descripcion').value.trim();
    const activa      = document.getElementById('obra-activa').checked;

    if (!codigoSap) return showFormError(errEl, 'El código SAP es obligatorio.');
    if (!/^[A-Z0-9]+(?:-[A-Z0-9]+)*$/.test(codigoSap)) {
      return showFormError(errEl, 'El código SAP solo admite letras, números y guiones simples.');
    }
    if (!nombre) return showFormError(errEl, 'El nombre de la obra es obligatorio.');

    /** @type {Object} DTO exacto — vertices nunca puede ser null */
    const body = { codigoSap, nombre, ubicacion, descripcion, vertices: obraVerticesActual, activa };

    btnOk.disabled = true;
    try {
      const res = await apiFetch(
        id ? `/admin/obras/${id}` : '/admin/obras',
        { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) }
      );

      if (res?.ok) {
        closeModal('modal-obra');
        toast(id ? '✓ Obra actualizada' : '✓ Obra creada');
        obraAsignacionesCache = [];
        await cargarObras();
      } else {
        const data = await res?.json().catch(() => ({}));
        showFormError(errEl, data.message ?? 'Error al guardar.');
      }
    } catch {
      showFormError(errEl, 'Error de conexión.');
    } finally {
      btnOk.disabled = false;
    }
  });
}
