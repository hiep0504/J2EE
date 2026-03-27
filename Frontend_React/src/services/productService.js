import apiClient from './apiClient'

export const getAllProducts = () => apiClient.get('/products')

export const getAllCategories = () => apiClient.get('/categories')
