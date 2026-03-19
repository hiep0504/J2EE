import axiosInstance from './axiosInstance';

export const getAllCategories = () => axiosInstance.get('/categories');

export const getProductsByCategory = (id) => axiosInstance.get(`/products/category/${id}`);
