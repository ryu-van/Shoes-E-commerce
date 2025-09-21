<template>
  <div class="order-success-container d-flex flex-column align-items-center justify-content-center py-5">
    <div class="success-icon mb-4">
      <svg width="80" height="80" fill="none">
        <circle cx="40" cy="40" r="38" stroke="#4CAF50" stroke-width="4" fill="#E8F5E9"/>
        <path d="M25 43 L37 55 L57 32" stroke="#4CAF50" stroke-width="5" fill="none" stroke-linecap="round"/>
      </svg>
    </div>
    <h2 class="mb-3 text-success fw-bold">Đặt hàng thành công!</h2>
    <p class="mb-4 text-secondary fs-5 text-center">
      Cảm ơn bạn đã mua sắm tại <span class="fw-semibold">Shoozy Shop</span>.<br/>
      Đơn hàng của bạn đã được tiếp nhận và đang được xử lý.
    </p>
    <button class="btn btn-success px-5 py-2 fw-bold fs-5" @click="goHome">
      <i class="bi bi-house me-2"></i>Về trang chủ
    </button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const goHome = () => {
  router.push({ path: "/" })
}
</script>

<style scoped>
.order-success-container {
  min-height: 60vh;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 8px 32px rgba(60,60,60,0.07), 0 1.5px 3px #d3ffe4;
}
.success-icon {
  width: 80px;
  height: 80px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 16px #8bc34a33;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
