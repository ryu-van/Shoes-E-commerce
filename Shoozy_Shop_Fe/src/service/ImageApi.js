import apiClient from "@/auth/api.js";

const BASE_URL = '/product-variant-images';

export const getImagesByColor = async (colorId) => {
    try {
        const response = await apiClient.get(`${BASE_URL}/by-color`, {
            params: { colorId }
        });
        return response;
    } catch (error) {
        console.error('Error fetching images by color:', error);
        throw error;
    }
};

// Upload ảnh cho nhiều variant
export const uploadImagesForVariants = async (imageIds, variantIds) => {
    try {
        const formData = new FormData();
        // Thêm existing image IDs
        if (imageIds && imageIds.length > 0) {
            formData.append('imageIds', imageIds.join(','));
        }
        // Thêm variant IDs
        if (variantIds && variantIds.length > 0) {
            variantIds.forEach(id => {
                formData.append('variantIds', id);
            });
        }
        const response = await apiClient.post(`${BASE_URL}/uploads`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        return response;
    } catch (error) {
        console.error('Error assigning images to variants:', error);
        throw error;
    }
};

// Upload ảnh lên hệ thống
export const uploadImagesOnly = async (files) => {
    try {
        const formData = new FormData();
        if (files && files.length > 0) {
            files.forEach(file => {
                formData.append('files', file);
            });
        }
        const response = await apiClient.post(`${BASE_URL}/upload-only`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        return response;
    } catch (error) {
        console.error('Error uploading images:', error);
        throw error;
    }
};

// Lấy ảnh theo IDs
export const getImagesByIds = async (imageIds) => {
    try {
        const response = await apiClient.get(`${BASE_URL}/grouped-images-by-ids/${imageIds.join(',')}`);
        return response;
    } catch (error) {
        console.error('Error fetching images by IDs:', error);
        throw error;
    }
};

// Xóa ảnh variant
export const deleteProductVariantImage = async (variantId, imageId) => {
    try {
        const response = await apiClient.delete(`${BASE_URL}/${variantId}/${imageId}`);
        return response;
    } catch (error) {
        console.error('Error deleting product variant image:', error);
        throw error;
    }
};

// Thêm hàm helper để validate file
export const validateImageFile = (file) => {
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    const maxSize = 5 * 1024 * 1024; // 5MB
    if (!allowedTypes.includes(file.type)) {
        throw new Error('Chỉ được phép upload file ảnh (JPEG, PNG, GIF, WebP)');
    }

    if (file.size > maxSize) {
        throw new Error('File ảnh không được vượt quá 5MB');
    }
    return true;
};