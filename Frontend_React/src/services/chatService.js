import apiClient from './apiClient'

export function startConversation(payload) {
  return apiClient.post('/chat/conversations/start', payload || {})
}

export function getMyActiveConversation() {
  return apiClient.get('/chat/conversations/my-active')
}

export function getMyConversationHistory() {
  return apiClient.get('/chat/conversations/my-history')
}

export function getConversationForUser(id) {
  return apiClient.get(`/chat/conversations/${id}`)
}

export function sendUserMessage(conversationId, payload) {
  return apiClient.post(`/chat/conversations/${conversationId}/messages`, payload)
}

export function getAdminOpenConversations() {
  return apiClient.get('/chat/admin/conversations/open')
}

export function getAdminAssignedConversations() {
  return apiClient.get('/chat/admin/conversations/assigned')
}

export function getAdminMyConversations() {
  return apiClient.get('/chat/admin/conversations/mine')
}

export function getAdminConversation(id) {
  return apiClient.get(`/chat/admin/conversations/${id}`)
}

export function adminPickConversation(id) {
  return apiClient.post(`/chat/admin/conversations/${id}/pick`)
}

export function adminTakeoverConversation(id) {
  return apiClient.post(`/chat/admin/conversations/${id}/takeover`)
}

export function adminSendMessage(id, payload) {
  return apiClient.post(`/chat/admin/conversations/${id}/messages`, payload)
}

export function adminCloseConversation(id) {
  return apiClient.post(`/chat/admin/conversations/${id}/close`)
}
