import apiClient from "@/auth/api.js";

const BASE_URL = '/sizes';

export const getAllSizes = () => apiClient.get(BASE_URL);

export const getActiveSizes = () => apiClient.get(`${BASE_URL}/active`);

export const getSizeById = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const createSize = (sizeData) => apiClient.post(BASE_URL, sizeData);

export const updateSize = (id, sizeData) => apiClient.put(`${BASE_URL}/${id}`, sizeData);

export const deleteSize = (id) => apiClient.delete(`${BASE_URL}/${id}`);

export const restoreSize = (id) => apiClient.put(`${BASE_URL}/${id}/restore`);

