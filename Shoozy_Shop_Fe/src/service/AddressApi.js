import apiClient from "@/auth/api";
const BASE_URL = '/addresses';

// Tạo địa chỉ mới
export const createAddress = (data) => apiClient.post(`${BASE_URL}`, data);

// Cập nhật địa chỉ
export const updateAddress = (addressId, data) => apiClient.put(`${BASE_URL}/${addressId}`, data);

// Lấy danh sách địa chỉ theo user_id
export const getAllAddresses = (userId) => apiClient.get(`${BASE_URL}?user_id=${userId}`);

// Lấy chi tiết địa chỉ theo id
export const getAddressById = (addressId) => apiClient.get(`${BASE_URL}/${addressId}`);

// Xóa địa chỉ
export const deleteAddress = (addressId) => apiClient.delete(`${BASE_URL}/${addressId}`);

// Đặt địa chỉ mặc định (selected)
export const setAddressSelected = (addressId, userId) => 
  apiClient.put(`${BASE_URL}/${addressId}/select?user_id=${userId}`);

// Lấy địa chỉ mặc định
export const getSelectedAddress = (userId) => apiClient.get(`${BASE_URL}/selected?user_id=${userId}`);

