import apiClient from "@/auth/api.js";

const BASE_URL = '/admin/users';

export const getAllUsers = (role, page, size, name, status) => {
  return apiClient.get(BASE_URL, {
    params: {
      role,
      page,
      size,
      keyword: name,
      status
    }
  });
};

export const getUserById = (id) => apiClient.get(`${BASE_URL}/${id}`);

// ❌ Không dùng buildFormData cho createUser
export const createUser = (formData) => {
  return apiClient.post(BASE_URL, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// JSON
export function updateUser(id, payload) {
  // KHÔNG đặt Content-Type thủ công; axios tự set application/json
  return apiClient.put(`/admin/users/${id}`, payload)
}

// multipart
export function updateUserMultipart(id, formData) {
  return apiClient.put(`/admin/users/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const lockUser = (id) => {
  return apiClient.put(`/admin/users/${id}/lock`)
}

