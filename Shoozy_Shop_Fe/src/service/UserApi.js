import apiClient from "@/auth/api.js";
import {useAuthStore} from "@/stores/Auth.js";

const BASE_URL = '/users';

export const getProfile = () => apiClient.get(`${BASE_URL}/profile`)

export const updateProfile = (userData) => apiClient.put(`${BASE_URL}/profile`, userData)

export const changePassword = (passwordData) => apiClient.put(`${BASE_URL}/change-password`, passwordData)

export const updateUserAvatar = async (userId, file) => {
    try {
        // Client-side validation
        if (!(file instanceof File)) {
            throw new Error('Vui lòng chọn một file ảnh');
        }

        const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp'];
        const fileName = file.name.toLowerCase();
        const fileExtension = fileName.split('.').pop();

        if (!allowedExtensions.includes(fileExtension)) {
            throw new Error('Chỉ được phép tải lên file ảnh (JPG, PNG, GIF, WEBP)');
        }

        if (!file.type.startsWith('image/')) {
            throw new Error('File phải là ảnh hợp lệ');
        }

        if (file.size > 5 * 1024 * 1024) {
            throw new Error('File không được vượt quá 5MB');
        }

        const formData = new FormData();
        formData.append('file', file);

        const response = await apiClient.put(`${BASE_URL}/avatar/${userId}`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
                Authorization: `Bearer ${useAuthStore().token}`,
            },
        });

        return {
            success: true,
            message: response.data.message,
            avatarUrl: response.data.data, // Backend trả về avatarUrl trong data
        };
    } catch (error) {
        const errorMessage = error.response?.data?.message || error.message || 'Không thể cập nhật avatar';
        return {
            success: false,
            error: errorMessage,
        };
    }
};