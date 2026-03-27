import apiClient from '../services/apiClient'

function getBackendOrigin() {
  const baseUrl = apiClient?.defaults?.baseURL
  if (!baseUrl) {
    return window.location.origin
  }

  try {
    return new URL(baseUrl).origin
  } catch {
    return window.location.origin
  }
}

export function toMediaUrl(rawUrl) {
  const value = String(rawUrl || '').trim()
  if (!value) return ''

  // Keep already absolute/data/blob URLs untouched.
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }

  const backendOrigin = getBackendOrigin()
  const normalizedPath = value.startsWith('/') ? value : `/${value}`
  return `${backendOrigin}${normalizedPath}`
}
