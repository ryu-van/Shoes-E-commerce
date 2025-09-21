<template>
  <div>
    <div class="product-info">
      <img :src="review.thumbnail" class="product-image" />
      <div class="product-name">{{ review.productName }}</div>
    </div>
    <div v-if="!isEditing">
      <div class="star-input">
        <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= review.reviewRating }">★</span>
      </div>
      <div style="margin: 10px 0;">{{ review.reviewContent }}</div>
      <div v-if="review.response || review.adminReply" class="review-response">
        <div class="response-label">Phản hồi từ quản trị viên:</div>
        <div class="response-content">{{ review.response || review.adminReply }}</div>
      </div>
      <!-- Chỉ hiển thị nút sửa nếu chưa quá 30 ngày -->
      <button 
        v-if="canEditReview" 
        class="btn btn-outline" 
        @click="isEditing = true"
      >
        Sửa đánh giá
      </button>

   

    
    </div>
    <div v-else>
      <form @submit.prevent="submitEdit">
        <div class="star-input">
          <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= editRating }" @click="editRating = n">★</span>
        </div>
        <textarea v-model="editContent" rows="3" class="form-control" />
        <div class="modal-actions">
          <button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu' }}</button>
          <button class="btn btn-outline" type="button" @click="isEditing = false">Hủy</button>
        </div>
        <div v-if="error" class="text-danger">{{ error }}</div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { updateReview } from '@/service/ReviewApi.js'

const props = defineProps({ review: Object })
const emit = defineEmits(['updated'])

const isEditing = ref(false)
const editRating = ref(props.review.reviewRating)
const editContent = ref(props.review.reviewContent)
const saving = ref(false)
const error = ref('')

// Kiểm tra xem có thể sửa đánh giá hay không (trong vòng 30 ngày)
const canEditReview = computed(() => {
  // Sử dụng updatedAt của đơn hàng (thời gian đơn hàng hoàn thành)
  const referenceDate = props.review.orderUpdatedAt || props.review.updatedAt;
  
  if (!referenceDate) return true; // Nếu không có thời gian tham chiếu, cho phép sửa
  
  const referenceDateTime = new Date(referenceDate);
  const now = new Date();
  const diffTime = Math.abs(now - referenceDateTime);
  const diffDays = diffTime / (1000 * 60 * 60 * 24);
  

  
  return diffDays <= 30; // Cho phép sửa trong vòng 30 ngày
});

const submitEdit = async () => {
  saving.value = true
  error.value = ''
  if (!editContent.value.trim()) {
    error.value = 'Vui lòng nhập nội dung đánh giá bạn muốn sửa .';
    saving.value = false;
    return;
  }
  try {
    await updateReview({
      id: props.review.reviewId,
      rating: editRating.value,
      content: editContent.value,
      isHidden: false
    })
    // Cập nhật lại dữ liệu hiển thị ngay
    props.review.reviewRating = editRating.value
    props.review.reviewContent = editContent.value
    isEditing.value = false
    emit('updated', { reviewId: props.review.reviewId, content: editContent.value, rating: editRating.value })
  } catch (e) {
    error.value = 'Cập nhật đánh giá thất bại'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.product-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.product-image {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}
.product-name {
  font-size: 18px;
  font-weight: 600;
  color: #222;
}
.star-input {
  display: flex;
  gap: 2px;
  margin-bottom: 8px;
  font-size: 22px;
}
.star {
  color: #ddd;
  cursor: pointer;
}
.star.filled {
  color: #ffd700;
}
.form-control {
  width: 100%;
  border-radius: 8px;
  border: 1px solid #eee;
  padding: 8px;
  font-size: 15px;
  margin-bottom: 12px;
}
.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}
.btn-primary {
  background: #000;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 10px 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}
.btn-primary:hover {
  background: #333;
}
.btn-outline {
  border: 2px solid #355e2b;
  background: white;
  color: #355e2b;
  border-radius: 20px;
  padding: 10px 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}
.btn-outline:hover {
  background: #eafbe7;
}
.text-danger {
  color: #dc3545;
  font-size: 14px;
}
.review-response {
  margin-top: 24px;
  padding: 16px;
  background: #f6f8fa;
  border-left: 4px solid #355e2b;
  border-radius: 8px;
}
.response-label {
  font-weight: 600;
  color: #355e2b;
  margin-bottom: 6px;
}
.response-content {
  color: #222;
}
/* Modal mở rộng */
:global(.modal-content) {
  max-width: 900px !important;
  min-width: 400px;
  min-height: 300px;
  padding: 40px 36px;
  max-height: 80vh;
  overflow-y: auto;
}
</style>
