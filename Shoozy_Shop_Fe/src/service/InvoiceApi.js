import apiClient from "@/auth/api.js";

// Lấy toàn bộ hóa đơn
export const getInvoices = async () => {
  try {
    const response = await apiClient.get('/invoice');
    console.log('getInvoices response:', response.data);
    return response;
  } catch (error) {
    console.error('Lỗi khi gọi API getInvoices:', error.response || error.message);
    throw error;
  }
};

// Gửi email hóa đơn
export const sendInvoiceEmail = async (orderId, recipientEmail) => {
  try {
    const response = await apiClient.post(`/invoice/${orderId}/send-email`, null, {
      params: { recipientEmail },
    });
    console.log('sendInvoiceEmail response:', response.data);
    return response.data;
  } catch (error) {
    console.error('Lỗi khi gửi email:', error.response || error.message);
    throw error;
  }
};

// Xuất file excel hóa đơn
export const exportInvoicesToExcel = async () => {
  try {
    const response = await apiClient.get('invoice/export-excel', {
      responseType: 'blob',
    });
    return response;
  } catch (error) {
    console.error('Lỗi khi export excel:', error.response || error.message);
    throw error;
  }
};