import apiClient from "@/auth/api"; 
const BASE_URL = "/payments";

export const createVnPayPayment = (orderRequest) => {
  return apiClient.post(`${BASE_URL}/create-vnpay`, orderRequest);
};

export const getVnPayReturn = (queryParams) => {
  return apiClient.get(`${BASE_URL}/vnpay-return`, {
    params: queryParams,
  });
};

export const retryVnPay = async (orderCode) => {
  const resp = await apiClient.post(`${BASE_URL}/retry-vnpay/${encodeURIComponent(orderCode)}`)
  return resp?.data?.data 
};