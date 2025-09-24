<script setup>
import { useRoute, useRouter } from 'vue-router'
import { getVnPayReturn } from '@/service/PaymentService.js'
import { sendOrderSuccessEmail } from '@/service/OrderApi.js'
import { onMounted, ref } from 'vue'

const route = useRoute()
const router = useRouter()
const result = ref(null)
const error = ref('')
const isLoading = ref(true)

onMounted(async () => {
  try {
    const params = route.query
    const res = await getVnPayReturn(params)
    result.value = res.data
    if (result.value.status === 200) {
      // Gửi email thông báo đặt hàng thành công cho thanh toán online
      if (result.value.data?.orderId) {
        try {
          await sendOrderSuccessEmail(result.value.data.orderId);
        } catch (emailError) {
          console.warn("Không thể gửi email thông báo:", emailError);
          // Không hiển thị lỗi cho user vì thanh toán đã thành công
        }
      }
      router.replace('/orders/success')
    } else {
      error.value = result.value.message || 'Thanh toán thất bại!'
    }
  } catch (e) {
    error.value = 'Có lỗi khi xác thực thanh toán!'
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <div class="verify-container">
    <div v-if="isLoading" class="verify-box loading">
      <div class="spinner-border text-primary mb-3"></div>
      <div class="verify-title">Đang xác thực thanh toán...</div>
      <div class="verify-desc text-muted">Vui lòng chờ trong giây lát.</div>
    </div>
    <div v-else-if="error" class="verify-box error">
      <div class="verify-icon text-danger">
        <i class="fas fa-times-circle fa-3x"></i>
      </div>
      <div class="verify-title text-danger">Thanh toán thất bại</div>
      <div class="verify-desc">{{ error }}</div>
      <button class="btn btn-outline-primary mt-3" @click="$router.replace('/carts')">
        <i class="fas fa-arrow-left me-2"></i> Quay lại giỏ hàng
      </button>
    </div>
  </div>
</template>

<style scoped>
.verify-container {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.verify-box {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  padding: 2.5rem 2rem;
  text-align: center;
  max-width: 400px;
  width: 100%;
}
.verify-title {
  font-size: 1.35rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
}
.verify-desc {
  font-size: 1rem;
  color: #555;
}
.verify-icon {
  margin-bottom: 1rem;
}
.loading .spinner-border {
  width: 2.5rem;
  height: 2.5rem;
}
.btn {
  padding: 0.7rem 1.5rem;
  border-radius: 6px;
  font-weight: 500;
  font-size: 1rem;
}
</style>