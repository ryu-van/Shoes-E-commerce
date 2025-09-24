import apiClient from "@/auth/api.js";

const BASE_URL = '/orders';

export const getAllOrders = () => apiClient.get(BASE_URL);

export const getOrderById = (id) => apiClient.get(`${BASE_URL}/${id}`);

export const createOrder = (orderRequest) => apiClient.post(BASE_URL, orderRequest);

export const updateOrder = (id, orderRequest) => apiClient.put(`${BASE_URL}/${id}`, orderRequest);

export const deleteOrderById = (id) => apiClient.delete(`${BASE_URL}/${id}`);

export const updateOrderStatus = (id, status) => apiClient.put(`${BASE_URL}/status/${id}`, { status });

export const updateCustomerInfo = (id, data) => {
  return apiClient.put(`${BASE_URL}/user-info/${id}`, data);
};
export const getAllOrdersByUserId = (userId) => apiClient.get(`${BASE_URL}/user`, {
  params: { userId }
});

export const getOrderTimelinesByOrderId = async (orderId) => {
  try {
    const response = await apiClient.get(`/order-timeline?orderId=${orderId}`);
    return response.data.data;
  } catch (error) {
    console.error('Error fetching order timelines:', error);
    throw error;
  }
};

export const getCoupon = (couponCode) =>
  apiClient.get(`${BASE_URL}/use-coupon?codeCoupon=${couponCode}`);

export const getCheckoutItems = (cartItemIds) =>
  apiClient.get('/orders/checkout-items', {
    params: { ids: cartItemIds.join(',') }
  });

export const getReturnableItems = (orderId, userId) => {
  return apiClient.get(`/orders/${orderId}/returnable-items`, {
    params: { userId }
  });
};

// ====== EMAIL NOTIFICATION APIs ======
// Gửi email thông báo đặt hàng thành công
export const sendOrderSuccessEmail = (orderId) => {
  return apiClient.post(`/order-email/send-success/${orderId}`);
};

// Gửi email thông báo hủy đơn hàng bởi người dùng
export const sendOrderCancelledByUserEmail = (orderId) => {
  return apiClient.post(`/order-email/send-cancelled-by-user/${orderId}`);
};

// Gửi email thông báo hoàn thành đơn hàng
export const sendOrderCompletedEmail = (orderId) => {
  return apiClient.post(`/order-email/send-completed/${orderId}`);
};

// Gửi email thông báo hủy đơn hàng bởi shop
export const sendOrderCancelledByShopEmail = (orderId, reason) => {
  return apiClient.post(`/order-email/send-cancelled-by-shop/${orderId}`, {
    reason: reason
  })
};
export const getTransactionByOrderCode = (orderCode) => {
  return apiClient.get(`/orders/transaction`, {
    params: { orderCode }
  });
};

