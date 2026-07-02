import { apiFetch } from './api.js'

const ALLOWED_ROLES = ['SOPORTE', 'RRHH', 'ADMIN', 'JEFE_OBRA', 'RESIDENTE']

export function getSession() {
  const token = localStorage.getItem('jwt_token')
  if (!token) return null
  return { token, role: localStorage.getItem('user_role') ?? '', name: localStorage.getItem('user_name') ?? 'Usuario' }
}

export async function login(correo, password) {
  const data = await apiFetch('/auth/login', { method: 'POST', body: JSON.stringify({ correo, password }) })
  const role = data.rol ?? data.role ?? data.roles?.[0] ?? ''
  if (role && !ALLOWED_ROLES.includes(role)) throw new Error('Tu cuenta no tiene acceso al panel administrativo.')
  localStorage.setItem('jwt_token', data.token)
  localStorage.setItem('user_role', role)
  localStorage.setItem('user_name', data.nombreCompleto ?? data.nombre ?? data.name ?? correo)
  return getSession()
}

export function logout() {
  localStorage.removeItem('jwt_token')
  localStorage.removeItem('user_role')
  localStorage.removeItem('user_name')
}

export const canManageEmployees = (role) => role === 'SOPORTE' || role === 'RRHH' || role === 'ADMIN'
