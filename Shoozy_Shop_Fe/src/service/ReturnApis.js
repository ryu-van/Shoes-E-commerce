import apiClient from "@/auth/api.js";

const BASE_URL = "/returns";
const ADMIN_BASE_URL = "/returns/admin";

// ----- CUSTOMER -----
export const getAllReturnsByUser = (q = "", status = "") =>
    apiClient.get(`${BASE_URL}/user`, {
        params: {
            q: q?.trim() || undefined,
            status: status || undefined, // 'PENDING', 'APPROVED', ...
        },
    });

export const getReturnDetail = (id) =>
    apiClient.get(`${BASE_URL}/${id}`);

export const createReturnRequest = (data) =>
    apiClient.post(BASE_URL, data);

// Upload nhiều ảnh: key 'files' khớp với Controller
export const uploadReturnImages = (files) => {
    const formData = new FormData();
    files.forEach((f) => formData.append("files", f));
    return apiClient.post(`${BASE_URL}/upload-images`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
    });
};

// ----- ADMIN -----
export const getAllReturnRequests = (page = 0, size = 10, status = "") =>
    apiClient.get(`${ADMIN_BASE_URL}`, {
        params: { page, size, status: status || undefined },
    });

/**
 * Cập nhật trạng thái:
 * - Trạng thái thường: chỉ cần { returnRequestId, status }
 * - REFUNDED: cần thêm { refundMethod, referenceCode, refundNote }
 *   refundMethod: 'CASH' | 'BANK_TRANSFER' | 'EWALLET'
 */
// ADMIN update status
export const updateReturnStatus = (returnRequestId, status, extra = {}) => {
    const payload = { returnRequestId, status };

    if (status === 'REFUNDED') {
        payload.refundMethod = extra.refundMethod;   // 'CASH' | 'BANK_TRANSFER' | 'EWALLET'
        payload.referenceCode = extra.referenceCode || '';
        payload.refundNote = extra.refundNote || ''; // ✅ Đúng key BE
    }

    return apiClient.post(`/returns/admin/update-status`, payload);
};

export const getReturnDetailByAdmin = (id) =>
    apiClient.get(`${ADMIN_BASE_URL}/${id}`);
