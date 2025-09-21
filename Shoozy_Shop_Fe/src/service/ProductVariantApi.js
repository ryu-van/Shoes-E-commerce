import apiClient from "@/auth/api.js";

const BASE_URL = '/product-variants';

export const createProductVariant = (productVariantData) => apiClient.post(BASE_URL, productVariantData);

export const getProductVariantById = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const updateProductVariant = (id, productVariantData) => apiClient.put(`${BASE_URL}/${id}`, productVariantData);

export const deleteProductVariant = (id) => apiClient.delete(`${BASE_URL}/${id}`);
