import apiClient from './apiClient'

export function askRag(question, history = []) {
  const lastAssistantWithProducts = [...history]
    .reverse()
    .find((item) => item.role === 'bot' && Array.isArray(item.products) && item.products.length > 0)
  const focusProductName = lastAssistantWithProducts?.products?.[0]?.name || ''

  return apiClient.post('/chat/rag/ask', { question, history, focusProductName })
}
