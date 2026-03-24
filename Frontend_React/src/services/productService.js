import apiClient from './apiClient'

export const getAllProducts = () => apiClient.get('/products')
