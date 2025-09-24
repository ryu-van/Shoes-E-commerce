import apiClient from "@/auth/api.js"; // Đã config baseURL, interceptor...

const BASE_URL = "/carts";

export default {
  // Thêm sản phẩm vào giỏ hàng
   addToCart({ userId, productVariantId, quantity }) {
    return apiClient.post(`${BASE_URL}/add`, { userId, productVariantId, quantity });
  },

  // // Xóa cart item theo cartId và cartItemId
  // deleteCartItem(cartId, cartItemId) {
  //   return apiClient.delete(`${BASE_URL}/${cartId}/items/${cartItemId}`);
  // },

  // Đổi số lượng cart item
  changeCartItemQuantity(cartItemId, quantity) {
    return apiClient.put(`${BASE_URL}/items/${cartItemId}/quantity`, { quantity });
  },

  // Lấy toàn bộ sản phẩm trong giỏ hàng của user (trả về List<ProductCartResponse>)
  // Truyền userId từ FE
  getCartItems(userId) {
    // Phía BE đã sửa đúng: /user/{userId}/items
    return apiClient.get(`${BASE_URL}/user/${userId}/items`);
  },
  deleteCartById(cartItemId) {
    return apiClient.delete(`${BASE_URL}/delete/${cartItemId}`);
  }

};
