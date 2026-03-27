import apiClient from './apiClient'

export function register(payload) {
  return apiClient.post('/auth/register', payload)
}

export function login(payload) {
  return apiClient.post('/auth/login', payload)
}

export function loginWithGoogle(payload) {
  return apiClient.post('/auth/google', payload)
}

export function logout() {
  return apiClient.post('/auth/logout')
}
