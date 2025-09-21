import apiClient from "@/auth/api.js";

// Lấy tất cả các đánh giá
export const getAllReviews = async () => {
  try {
    const response = await apiClient.get('/reviews');
    console.log('getAllReviews response:', response.data);
    return response;
  } catch (error) {
    console.error('Lỗi khi gọi API getAllReviews:', error.response || error.message);
    throw error;
  }
};

// Cập nhật đánh giá
export const updateReview = async (data) => {
  try {
    const response = await apiClient.put(`/reviews/${data.id}`, {
      content: data.content,
      isHidden: data.isHidden,
      rating: data.rating,
    });
    console.log('updateReview response:', response.data);
    return response;
  } catch (error) {
    console.error('Lỗi khi gọi API updateReview:', error.response || error.message);
    throw error;
  }
};

// Thêm bình luận cho đánh giá
export const addComment = async (data) => {
  try {
    const response = await apiClient.post('/reviews/comments', {
      reviewId: data.reviewId,
      content: data.content,
    });
    console.log('addComment response:', response.data);
    return response;
  } catch (error) {
    console.error('Lỗi khi gọi API addComment:', error.response || error.message);
    throw error;
  }
};

// Lấy review theo productId
export const getReviewsByProductId = async (productId) => {
  try {
    const response = await  apiClient.get(`/reviews/by-product/${productId}`);
    return response;
  } catch (error) {
    console.error('Lỗi khi gọi API getReviewsByProductId:', error.response || error.message);
    throw error;
  }
};

// Thêm đánh giá mới
export const addReview = async (data) => {
  try {
    const response = await apiClient.post('/reviews', {
      productId: data.productId,
      content: data.content,
      rating: data.rating,
      orderDetailId: data.orderDetailId, 
    });
    return response;
  } catch (error) {
    console.error('Lỗi khi gọi API addReview:', error.response || error.message);
    throw error;
  }
};

// Gửi phản hồi cho review
export const replyToReview = async (reviewId, adminId, content) => {
  try {
    const response = await apiClient.post(
      `/reviews/reviews/${reviewId}/reply?adminId=${adminId}`,
      JSON.stringify(content),
      { headers: { 'Content-Type': 'application/json' } }
    );
    return response;
  } catch (error) {
    console.error('Lỗi khi gửi phản hồi:', error.response || error.message);
    throw error;
  }
};

// Lấy tất cả review từ API mới
export const getAllReviewInfo = async () => {
  try {
    const response = await apiClient.get('/reviews/info');
    return response;
  } catch (error) {
    console.error('Lỗi khi gọi API getAllReviewInfo:', error.response || error.message);
    throw error;
  }
};

// Lấy chi tiết review kèm phản hồi từ API mới
export const getReviewInfoById = async (id) => {
  try {
    const response = await apiClient.get(`/reviews/info/${id}`);
    return response;
  } catch (error) {
    console.error('Lỗi khi gọi API getReviewInfoById:', error.response || error.message);
    throw error;
  }
};