import apiClient from "@/auth/api";
const BASE_URL = '/shipping';

const BASE = '/shipping'

export const ShippingApi = {
  // Tính phí từ địa chỉ lưu theo provinces.open-api.vn (code/name)
  calcFeeFromOpenApi: (payload) =>
    apiClient.post(`${BASE}/fee-from-openapi`, payload),
}