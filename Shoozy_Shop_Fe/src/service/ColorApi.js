import apiClient from "@/auth/api.js";

const BASE_URL = '/colors';

export const getAllColors = () => apiClient.get(BASE_URL);

export const getActiveColors = () => apiClient.get(`${BASE_URL}/active`);

export const getColorById = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const createColor = (colorData) => apiClient.post(BASE_URL, colorData);

export const updateColor = (id, colorData) => apiClient.put(`${BASE_URL}/${id}`, colorData);

export const deleteColor = (id) => apiClient.delete(`${BASE_URL}/${id}`);

export const restoreColor = (id) => apiClient.put(`${BASE_URL}/${id}/restore`);

