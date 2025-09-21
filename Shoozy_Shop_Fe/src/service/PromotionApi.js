import apiClient from "@/auth/api.js";

const BASE_URL = '/promotions';

export default {
  getPromotions({ page, limit, keyword, startDate, endDate, status }) {
    const params = { page, limit };

    if (keyword) params.keyword = keyword;
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    if (status !== undefined) params.status = status;

    return apiClient.get(BASE_URL, { params });
  },

  getPromotionById(id) {
    return apiClient.get(`${BASE_URL}/${id}`);
  },

  disableStatus(id) {
    return apiClient.put(`${BASE_URL}/status/${id}`);
  },

  activeStatus(id) {
    return apiClient.put(`${BASE_URL}/active/${id}`);
  },

  createPromotion(data) {
    return apiClient.post(BASE_URL, data);
  },

  updatePromotion(id, data) {
    return apiClient.put(`${BASE_URL}/${id}`, data);
  }
};