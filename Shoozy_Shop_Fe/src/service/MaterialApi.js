import apiClient from "@/auth/api.js";

const BASE_URL = '/materials';

export const getAllMaterials = () => apiClient.get(BASE_URL);

export const getActiveMaterials = () => apiClient.get(`${BASE_URL}/active`);

export const getMaterialById = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const createMaterial = (materialData) => apiClient.post(BASE_URL, materialData);

export const updateMaterial = (id, materialData) => apiClient.put(`${BASE_URL}/${id}`, materialData);

export const deleteMaterial = (id) => apiClient.delete(`${BASE_URL}/${id}`);

export const restoreMaterial = (id) => apiClient.put(`${BASE_URL}/${id}/restore`);
