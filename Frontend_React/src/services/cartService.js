import apiClient from './apiClient'

export function getCart() {
  return apiClient.get('/cart')
}

export function addToCart(productId, quantity = 1) {
  return apiClient.post('/cart/items', { productId, quantity })
}

export function updateCartItem(productId, quantity) {
  return apiClient.put(`/cart/items/${productId}`, { quantity })
}

export function removeCartItem(productId) {
  return apiClient.delete(`/cart/items/${productId}`)
}
