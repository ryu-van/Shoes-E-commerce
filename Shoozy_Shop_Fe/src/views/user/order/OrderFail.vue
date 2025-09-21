<template>
  <div class="order-success-container d-flex flex-column align-items-center justify-content-center py-5">
    <div class="success-icon mb-4">
      <svg width="96" height="96" fill="none" aria-hidden="true">
        <circle cx="48" cy="48" r="46" stroke="#d32f2f" stroke-width="4" fill="#ffebee"/>
        <path d="M34 34 L62 62 M62 34 L34 62" stroke="#d32f2f" stroke-width="6" fill="none" stroke-linecap="round"/>
      </svg>
    </div>

    <h2 class="mb-2 text-success fw-bold">Thanh toán thất bại</h2>

    <p class="mb-3 text-secondary fs-6 text-center">
      {{ displayMsg }}
    </p>

    <div v-if="orderCode" class="chip mb-3" title="Mã đơn hàng">
      <span>Mã đơn:</span>
      <strong>{{ orderCode }}</strong>
      <span v-if="code" class="code">(#{{ code }})</span>
    </div>

    <div class="d-flex flex-wrap gap-2 justify-content-center">
      <button class="btn btn-success px-4 py-2 fw-bold" @click="retry" :disabled="loading">
        <i v-if="!loading" class="bi bi-arrow-repeat me-2"></i>
        <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        {{ loading ? 'Đang xử lý...' : 'Thanh toán lại với VNPay' }}
      </button>
      <button class="btn btn-outline-secondary px-4 py-2 fw-bold" @click="goOrders">
        <i class="bi bi-receipt me-2"></i>Xem đơn hàng
      </button>
      <button class="btn btn-light px-4 py-2 fw-bold" @click="goHome">
        <i class="bi bi-house me-2"></i>Về trang chủ
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { retryVnPay } from '@/service/PaymentService' // Import API retryVnPay

const route = useRoute()
const router = useRouter()

const orderCode = computed(() => route.query.orderCode || '')
const code = computed(() => route.query.code || '')
const loading = ref(false)

const displayMsg = computed(() => {
  if (route.query.msg) return String(route.query.msg)
  if (route.query.code === '24') return 'Bạn đã hủy thanh toán.'
  if (route.query.code === '51') return 'Tài khoản không đủ số dư.'
  if (route.query.code === '09') return 'Giao dịch đang xử lý. Vui lòng kiểm tra lại sau.'
  return 'Thanh toán chưa hoàn thành. Vui lòng thử lại hoặc chọn phương thức khác.'
})

const goHome = () => router.push({ path: '/' })
const goOrders = () => router.push({ path: '/orders' })

const retry = async () => {
  if (!orderCode.value) {
    router.push({ path: '/checkout' })
    return
  }
  loading.value = true
  try {
    const paymentUrl = await retryVnPay(orderCode.value)  // <-- trả string
    if (typeof paymentUrl === 'string' && paymentUrl.startsWith('http')) {
      window.location.assign(paymentUrl) // hoặc window.location.href = paymentUrl
      return
    }
    // Không nhận được URL hợp lệ -> đưa về checkout
    router.push({ path: '/checkout', query: { orderCode: orderCode.value, retry: 'true' } })
  } catch (err) {
    console.error('Retry VNPay error:', err)
    router.push({ path: '/checkout', query: { orderCode: orderCode.value, retry: 'true' } })
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>
/* ===== THEME (Fail) — có thể đổi màu tại đây ===== */
.order-success-container{
  --status: #d32f2f;            /* đỏ chính */
  --status-bg: #ffebee;         /* nền nhạt trong icon */
  --status-shadow: rgba(211,47,47,.22);
}

/* Card */
.order-success-container{
  min-height: 70vh;
  max-width: 760px;
  margin: 3rem auto;
  padding: 2.5rem 2rem;
  background: #fff;
  border-radius: 18px;
  border: 1px solid #f1f1f1;
  box-shadow:
    0 16px 45px rgba(0,0,0,.06),
    0 3px 9px var(--status-shadow);
  text-align: center;
  animation: pop-in .35s ease-out;
}

/* Icon */
.success-icon{
  width: 96px;
  height: 96px;
  margin-bottom: 1.25rem;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 6px 24px var(--status-shadow);
  display: flex;
  align-items: center;
  justify-content: center;
}
.success-icon svg circle{
  stroke: var(--status) !important;
  fill: var(--status-bg) !important;
}
.success-icon svg path{
  stroke: var(--status) !important;
}

/* Override bootstrap green within this component */
.order-success-container :is(h1,h2,h3).text-success{
  color: var(--status) !important;
}

/* Chip */
.chip{
  display: inline-flex;
  align-items: center;
  gap: .5rem;
  padding: .35rem .75rem;
  border-radius: 999px;
  border: 1px solid #ffcdd2;
  background: #fff5f5;
  color: #c62828;
  font-weight: 600;
}
.chip .code{ opacity:.8; font-weight:500; }

/* Buttons */
.order-success-container .btn-success{
  background: var(--status);
  border-color: var(--status);
  color: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 16px var(--status-shadow);
  transition: transform .15s ease, box-shadow .2s ease, filter .2s ease;
}
.order-success-container .btn-success:hover{
  filter: brightness(1.03);
  transform: translateY(-1px);
  box-shadow: 0 10px 22px var(--status-shadow);
}
.order-success-container .btn-light{
  border: 1px solid #e9ecef;
  border-radius: 12px;
}

/* Responsive */
@media (max-width: 576px){
  .order-success-container{
    margin: 2rem 1rem;
    padding: 1.75rem 1rem;
  }
  .success-icon{ width: 84px; height: 84px; }
}

/* Anim */
@keyframes pop-in{
  from{ opacity: 0; transform: translateY(6px) scale(.98); }
  to  { opacity: 1; transform: translateY(0)  scale(1); }
}
</style>
