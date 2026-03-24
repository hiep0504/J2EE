import apiClient from './apiClient'

export function getMe() {
  return apiClient.get('/account/me')
}

export function updateMe(payload) {
  return apiClient.put('/account/me', payload)
}
