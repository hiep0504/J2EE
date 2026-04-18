import apiClient from './apiClient'

export function getAdminProducts(params) {
  return apiClient.get('/admin/products', { params })
}

export function createAdminProduct(payload) {
  return apiClient.post('/admin/products', payload)
}

export function updateAdminProduct(id, payload) {
  return apiClient.put(`/admin/products/${id}`, payload)
}

export function deleteAdminProduct(id) {
  return apiClient.delete(`/admin/products/${id}`)
}

export function getAdminCategories(params) {
  return apiClient.get('/admin/categories', { params })
}

export function getAdminSizes() {
  return apiClient.get('/admin/sizes')
}

export function createAdminCategory(payload) {
  return apiClient.post('/admin/categories', payload)
}

export function updateAdminCategory(id, payload) {
  return apiClient.put(`/admin/categories/${id}`, payload)
}

export function deleteAdminCategory(id) {
  return apiClient.delete(`/admin/categories/${id}`)
}

export function getAdminOrders(params) {
  return apiClient.get('/admin/orders', { params })
}

export function getAdminOrderDetail(id) {
  return apiClient.get(`/admin/orders/${id}`)
}

export function updateAdminOrderStatus(id, payload) {
  return apiClient.put(`/admin/orders/${id}/status`, payload)
}

export function getAdminRevenueSummary(params) {
  return apiClient.get('/admin/revenue', { params })
}

export function getAdminUsers(params) {
  return apiClient.get('/admin/users', { params })
}

export function updateAdminUserRole(id, payload) {
  return apiClient.put(`/admin/users/${id}/role`, payload)
}

export function updateAdminUserLock(id, payload) {
  return apiClient.put(`/admin/users/${id}/lock`, payload)
}

export function getAdminReviews(params) {
  return apiClient.get('/admin/reviews', { params })
}

export function deleteAdminReview(id) {
  return apiClient.delete(`/admin/reviews/${id}`)
}

export function uploadAdminImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/admin/uploads/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
