import apiClient from "@/auth/api.js";

const BASE_URL = '/brands';

export const getAllBrands = () => apiClient.get(BASE_URL);

export const getActiveBrands = () => apiClient.get(`${BASE_URL}/active`);

export const getBrandsById = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const createBrands = (brandData) => apiClient.post(BASE_URL, brandData);

export const updateBrands = (id, brandData) => apiClient.put(`${BASE_URL}/${id}`, brandData);

export const deleteBrands = (id) => apiClient.delete(`${BASE_URL}/${id}`);

export const restoreBrand = (id) => apiClient.put(`${BASE_URL}/${id}/restore`);
