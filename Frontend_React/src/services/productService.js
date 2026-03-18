import axiosInstance from './axiosInstance';

export const getAllProducts = () => axiosInstance.get('/products');

export const getProductById = (id) => axiosInstance.get(`/products/${id}`);
