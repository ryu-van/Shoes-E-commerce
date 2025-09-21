import apiClient from "@/auth/api.js";

const BASE_URL = '/categories';

export const getAllCategories = () => apiClient.get(BASE_URL);

export const getActiveCategories = () => apiClient.get(`${BASE_URL}/active`);

export const getCategoryById = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const createCategory = (categoryData) => apiClient.post(BASE_URL, categoryData);

export const updateCategory = (id, categoryData) => apiClient.put(`${BASE_URL}/${id}`, categoryData);

export const deleteCategory = (id) => apiClient.delete(`${BASE_URL}/${id}`);

export const restoreCategory = (id) => apiClient.put(`${BASE_URL}/${id}/restore`);

