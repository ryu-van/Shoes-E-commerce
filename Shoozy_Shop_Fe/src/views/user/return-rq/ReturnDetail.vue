<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getReturnDetail } from '@/service/ReturnApis'
import noImage from '@/assets/img/no-image.png'

const router = useRouter()
const route = useRoute()

const returnRequest = ref(null)
const loading = ref(true)

const goBack = () => router.back()

// Helpers
function formatDate(dateString) {
  const d = new Date(dateString)
  return d.toLocaleDateString('vi-VN')
}

const statusTextMap = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  WAIT_FOR_PICKUP: 'Chờ lấy hàng',
  RETURNED: 'Đã nhận hàng',
  REFUNDED: 'Đã hoàn tiền',
  CANCELLED: 'Đã huỷ',
  COMPLETED: 'Hoàn tất'
}
function getStatusText(status) {
  return statusTextMap[status] ?? status
}
function getStatusClass(status) {
  switch (status) {
    case 'PENDING': return 'badge bg-warning text-dark'
    case 'APPROVED': return 'badge bg-success'
    case 'REJECTED': return 'badge bg-danger'
    case 'WAIT_FOR_PICKUP': return 'badge bg-info text-dark'
    case 'RETURNED': return 'badge bg-primary'
    case 'REFUNDED': return 'badge bg-secondary'
    case 'CANCELLED': return 'badge bg-dark'
    case 'COMPLETED': return 'badge bg-success'
    default: return 'badge bg-secondary'
  }
}

function onImgError(e) {
  e.target.src = noImage
}

// Fetch
onMounted(async () => {
  try {
    const res = await getReturnDetail(route.params.id)
    returnRequest.value = res.data?.data || res.data
  } catch (err) {
    console.error('❌ Lỗi khi lấy chi tiết trả hàng:', err)
    returnRequest.value = null
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container py-4">
    <div class="mb-3">
      <button class="btn btn-outline-secondary" @click="goBack">← Quay lại</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center my-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Đang tải...</span>
      </div>
      <p class="mt-2">Đang tải chi tiết trả hàng...</p>
    </div>

    <!-- Found -->
    <div v-else-if="returnRequest" class="card p-4 shadow-sm">
      <h3 class="mb-3">Chi tiết yêu cầu trả hàng #{{ returnRequest.id }}</h3>

      <!-- Thông tin chung -->
      <div class="mb-3">
        <p>
          <strong>Mã đơn hàng:</strong>
          <span class="badge bg-dark ms-1">{{ returnRequest.order?.orderCode || '—' }}</span>
        </p>
        <p>
          <strong>Trạng thái:</strong>
          <span :class="getStatusClass(returnRequest.status)" class="ms-1">
            {{ getStatusText(returnRequest.status) }}
          </span>
        </p>
        <p>
          <strong>Số tiền hoàn dự kiến:</strong>
          {{ returnRequest.refundAmount?.toLocaleString('vi-VN') || 0 }} đ
        </p>
        <p v-if="returnRequest.returnItems?.length > 1">
          <strong>Lý do chung:</strong> {{ returnRequest.reason || '—' }}
        </p>
        <p v-if="returnRequest.note">
          <strong>Ghi chú thêm:</strong> {{ returnRequest.note }}
        </p>
        <p>
          <strong>Ngày tạo:</strong> {{ formatDate(returnRequest.createdAt) }}
        </p>
      </div>

      <!-- Danh sách sản phẩm -->
      <h4 class="mt-3 mb-2">Sản phẩm được trả</h4>
      <div
        v-for="item in returnRequest.returnItems"
        :key="item.id"
        class="border rounded p-3 mb-3 product-row d-flex align-items-center gap-3"
      >

        <div class="flex-grow-1">
          <h6 class="mb-1">
            {{ item.productName }}
            <small class="text-muted ms-1">(x{{ item.quantity }})</small>
          </h6>

          <!-- Lý do -->
          <p class="mb-2 small text-muted" v-if="returnRequest.returnItems.length === 1">
            <strong>Lý do trả hàng:</strong> {{ item.note || 'Không có' }}
          </p>
          <p class="mb-2 small text-muted" v-else>
            <strong>Lý do riêng:</strong> {{ item.note || 'Không có' }}
          </p>

          <!-- Ảnh minh họa -->
          <div v-if="Array.isArray(item.imageUrls) && item.imageUrls.length" class="d-flex flex-wrap gap-2 mt-1">
            <img
              v-for="(url, i) in item.imageUrls"
              :key="i"
              :src="url || noImage"
              @error="onImgError"
              alt="Ảnh minh họa"
              class="rounded border preview"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Not found -->
    <div v-else class="alert alert-danger">Không tìm thấy dữ liệu yêu cầu trả hàng.</div>
  </div>
</template>

<style scoped>
.thumb {
  width: 90px;
  height: 90px;
  object-fit: cover;
}
.preview {
  width: 90px;
  height: 90px;
  object-fit: cover;
}
.product-row {
  background: #fff;
}
</style>
