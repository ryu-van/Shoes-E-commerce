<!-- ReviewForm.vue -->
<template>
  <form class="review-form" @submit.prevent="submit">
    <div class="form-group mb-2">
      <label>Đánh giá:</label>
      <div class="star-input">
        <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= rating }" @click="rating = n">★</span>
      </div>
    </div>
    <div class="form-group mb-2">
      <label>Nội dung:</label>
      <textarea v-model="content" class="form-control" rows="3" required placeholder="Nhập bình luận..."></textarea>
    </div>
    <button type="submit" class="btn btn-primary" :disabled="loading">Gửi bình luận</button>
    <div v-if="error" class="text-danger mt-2">{{ error }}</div>
  </form>
</template>

<script setup>
import { ref } from 'vue';
import { addReview } from '@/service/ReviewApi.js';

const props = defineProps({ productId: [String, Number], orderDetailId: [String, Number] });
const emit = defineEmits(['submitReview']);

const rating = ref(5);
const content = ref('');
const loading = ref(false);
const error = ref('');

const submit = async () => {
  if (!content.value.trim()) {
    error.value = 'Vui lòng nhập nội dung bình luận.';
    return;
  }
  if (rating.value < 1 || rating.value > 5) {
    error.value = 'Điểm đánh giá phải từ 1 đến 5.';
    return;
  }

  const user = JSON.parse(localStorage.getItem('user'));
  if (!user?.id || !localStorage.getItem('token')) {
    error.value = 'Bạn cần đăng nhập để viết bình luận.';
    return;
  }

  loading.value = true;
  error.value = '';
  try {
    const res = await addReview({
      productId: Number(props.productId),
      content: content.value,
      rating: rating.value,
      orderDetailId: Number(props.orderDetailId)
    });
    emit('submitReview', {
      orderDetailId: props.orderDetailId,
      content: content.value,
      rating: rating.value,
      reviewId: res.data?.data?.id // id trả về từ backend
    });
    content.value = '';
    rating.value = 5;
  } catch (e) {
    // Nếu lỗi nhưng đã ghi vào DB, vẫn coi là thành công
    if (
      e.response &&
      e.response.data &&
      (
        e.response.data.status === 200 ||
        (typeof e.response.data.message === 'string' &&
          (e.response.data.message.toLowerCase().includes('thành công') ||
           e.response.data.message.toLowerCase().includes('success')))
      )
    ) {
      emit('submitReview', { orderDetailId: props.orderDetailId });
      content.value = '';
      rating.value = 5;
    } else {
      error.value = e.response?.data?.message || 'Gửi bình luận thất bại!';
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.review-form {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 24px;
  background: #fafbfc;
  margin-bottom: 24px;
}
.star-input {
  display: flex;
  gap: 2px;
  margin-bottom: 8px;
}
.star {
  color: #ddd;
  font-size: 22px;
  cursor: pointer;
}
.star.filled {
  color: #ffd700;
}
.form-group label {
  font-weight: 500;
}
.btn-primary {
  background: #000;
  border: none;
  border-radius: 20px;
  padding: 10px 20px;
}
.btn-primary:hover {
  background: #333;
}
.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.text-danger {
  font-size: 14px;
}
</style>