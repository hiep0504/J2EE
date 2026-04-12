import apiClient from './apiClient'

export function askRag(question) {
  return apiClient.post('/chat/rag/ask', { question })
}
