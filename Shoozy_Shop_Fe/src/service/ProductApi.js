import apiClient from "@/auth/api.js";
import qs from 'qs';

const BASE_URL = '/products';

export const getAllProducts = (params = {}) =>
    apiClient.get(BASE_URL, {
        params,
        paramsSerializer: (params) =>
            qs.stringify(params, { arrayFormat: 'repeat' }) // hỗ trợ array param như ?sizes=1&sizes=2
    });

export const getProductById = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const createProduct = (productData) => apiClient.post(BASE_URL, productData);

export const updateProduct = (id, productData) => apiClient.put(`${BASE_URL}/${id}`, productData);

export const deleteProduct = (id) => apiClient.delete(`${BASE_URL}/${id}`);
export const getListOfProducts = (params = {}) => {
  return apiClient.get(`${BASE_URL}/product-promotion`, { params });
};

export const getAllProductsByCategory = (params = {}) => apiClient.get(`${BASE_URL}/by-category`, { params });


export const getActiveProductsForPromotion = (productIds = []) => {
  return apiClient.get(`${BASE_URL}/active-for-promotion`, {
    params: { productIds },
    paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
  });
};

export const restoreProduct = (id) => apiClient.put(`${BASE_URL}/${id}/restore`);
