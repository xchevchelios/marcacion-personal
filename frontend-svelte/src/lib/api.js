const API_PREFIX = '/api/v1'

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

async function readBody(response) {
  if (response.status === 204) return null
  const type = response.headers.get('content-type') ?? ''
  return type.includes('application/json') ? response.json() : response.text()
}

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem('jwt_token')
  const headers = new Headers(options.headers)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  let response
  try {
    response = await fetch(`${API_PREFIX}${path}`, { ...options, headers })
  } catch {
    throw new ApiError('No se pudo conectar con el servidor.', 0, null)
  }

  const data = await readBody(response)
  if (!response.ok) {
    const message = data?.message || data?.error || data || `Error HTTP ${response.status}`
    throw new ApiError(String(message), response.status, data)
  }
  return data
}

export const get = (path) => apiFetch(path)
export const patch = (path, body) => apiFetch(path, {
  method: 'PATCH',
  ...(body !== undefined && { body: JSON.stringify(body) }),
})
export const post = (path, body) => apiFetch(path, { method: 'POST', body: JSON.stringify(body) })
export const put = (path, body) => apiFetch(path, { method: 'PUT', body: JSON.stringify(body) })
export const del = (path) => apiFetch(path, { method: 'DELETE' })
