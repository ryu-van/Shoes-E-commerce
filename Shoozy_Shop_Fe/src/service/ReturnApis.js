import apiClient from "@/auth/api.js";

const BASE_URL = "/returns";
const ADMIN_BASE_URL = "/returns/admin";

// Khách hàng
// Khách hàng
export const getAllReturnsByUser = (q = '', status = '') =>
    apiClient.get(`${BASE_URL}/user`, {
        params: {
            q: q?.trim() || undefined,          // nếu rỗng thì bỏ khỏi query string
            status: status || undefined         // ví dụ: 'PENDING', 'APPROVED', ...
        }
    });


export const getReturnDetail = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const createReturnRequest = (data) => apiClient.post(BASE_URL, data);

// ✅ Multi – 1 lần cho nhiều ảnh (key 'files' khớp controller)
export const uploadReturnImages = (files) => {
    const formData = new FormData();
    files.forEach(f => formData.append("files", f));
    return apiClient.post(`${BASE_URL}/upload-images`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
    });
};

// ADMIN
export const getAllReturnRequests = (page = 0, size = 10, status = '') =>
    apiClient.get(`${ADMIN_BASE_URL}`, {
        params: { page, size, status: status || undefined }
    });

export const updateReturnStatus = (returnRequestId, status) =>
    apiClient.post(`${ADMIN_BASE_URL}/update-status`, { returnRequestId, status });

export const getReturnDetailByAdmin = (id) =>
    apiClient.get(`${ADMIN_BASE_URL}/${id}`);
