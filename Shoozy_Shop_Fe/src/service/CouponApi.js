import apiClient from "@/auth/api.js";

const BASE_URL = '/coupons';

export default {
  getCoupons({ page = 0, limit = 10, keyword, startDate, expirationDate, status }) {
    const params = { page, limit };

    if (keyword) params.keyword = keyword;
    if (startDate) params.startDate = startDate;
    if (expirationDate) params.expirationDate = expirationDate;
    if (status !== undefined && status !== null && status !== '') params.status = status;

    return apiClient.get(`${BASE_URL}/filter`, { params });
  },

  getAllCoupons() {
    return apiClient.get(BASE_URL);
  },

  getCouponById(id) {
    return apiClient.get(`${BASE_URL}/${id}`);
  },

  createCoupon(data) {
    return apiClient.post(`${BASE_URL}/create`, data);
  },

  updateCoupon(id, data) {
    return apiClient.put(`${BASE_URL}/update/${id}`, data);
  },

  activeCoupon(id, status) {
    return apiClient.put(`${BASE_URL}/update-status/${id}`, null, { params: { status } });
  },

  deleteCoupon(id) {
    return apiClient.delete(`${BASE_URL}/delete/${id}`);
  },

  getCouponByCode(code,id_user) {
    return apiClient.get(`${BASE_URL}/order`, {
      params: { code,id_user }
    });
  },

  getAllCoupons(id_user, money_order) {
    return apiClient.get(`${BASE_URL}/order/all`, {
      params: { id_user, money_order }, 
    })
  },

};
