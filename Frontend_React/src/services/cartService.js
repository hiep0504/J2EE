import apiClient from './apiClient'

export function getCart() {
  return apiClient.get('/cart')
}

export function addToCart(productId, sizeId, quantity = 1) {
  return apiClient.post('/cart/items', { productId, sizeId, quantity })
}

export function updateCartItem(productId, sizeId, quantity) {
  return apiClient.put(`/cart/items/${productId}/${sizeId}`, { sizeId, quantity })
}

export function removeCartItem(productId, sizeId) {
  return apiClient.delete(`/cart/items/${productId}/${sizeId}`)
}

export function checkoutCart(payload) {
  return apiClient.post('/cart/checkout', payload)
}

