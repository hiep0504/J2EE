import apiClient from './apiClient'

export function getMyOrders() {
  return apiClient.get('/account/orders')
}

export function getMyOrderDetail(orderId) {
  return apiClient.get(`/account/orders/${orderId}`)
}

export function getMyPurchasedProducts() {
  return apiClient.get('/account/purchased-products')
}

export function createMyReview(productId, payload) {
  return apiClient.post(`/account/purchased-products/${productId}/reviews`, payload, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export function updateMyReview(reviewId, payload) {
  return apiClient.put(`/account/reviews/${reviewId}`, payload, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export function deleteMyReview(reviewId) {
  return apiClient.delete(`/account/reviews/${reviewId}`)
}
