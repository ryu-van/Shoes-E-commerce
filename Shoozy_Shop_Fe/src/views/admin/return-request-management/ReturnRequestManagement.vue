<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { getAllReturnRequests } from '@/service/ReturnApis'
import ShowToastComponent from "@/components/ShowToastComponent.vue"
import { useRouter } from 'vue-router'

const router = useRouter()
const toastRef = ref(null)

const requests = ref([])
const loading = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)

const filterStatus = ref('')
const PAGE_SIZE_OPTIONS = [5, 10, 15, 20, 25]
const statusLabels = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  WAIT_FOR_PICKUP: "Tạo lệnh lấy hàng",
  RETURNED: "Đã hoàn hàng",
  REFUNDED: "Đã hoàn tiền",
  REJECTED: "Bị từ chối",
  CANCELLED: "Đã huỷ",
  COMPLETED: "Hoàn tất"
}

const fetchRequests = async () => {
  loading.value = true
  try {
    const res = await getAllReturnRequests(currentPage.value - 1, pageSize.value, filterStatus.value)
    const data = res.data.data || {}
    requests.value = data.content || []
    totalPages.value = data.totalPages || 1
    totalElements.value = data.totalElements || 0
  } catch (e) {
    showToast('Lỗi khi tải danh sách yêu cầu trả hàng.', 'error')
  } finally {
    loading.value = false
  }
}

const searchRequests = () => {
  currentPage.value = 1
  fetchRequests()
}

const clearFilter = () => {
  filterStatus.value = ''
  currentPage.value = 1
  fetchRequests()
}

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type)
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value && page !== '...') {
    currentPage.value = page
  }
}

const changePageSize = (val) => {
  const newSize = parseInt(val)
  if (newSize !== pageSize.value) {
    pageSize.value = newSize
    currentPage.value = 1
  }
}

const paginationInfo = computed(() => {
  if (totalElements.value === 0) return { start: 0, end: 0, total: 0 }
  const start = (currentPage.value - 1) * pageSize.value + 1
  const end = Math.min(currentPage.value * pageSize.value, totalElements.value)
  return { start, end, total: totalElements.value }
})

const visiblePages = computed(() => {
  const current = currentPage.value
  const total = totalPages.value
  const pages = []
  if (total <= 1) return [1]
  if (current > 3) {
    pages.push(1)
    if (current > 4) pages.push('...')
  }
  for (let i = Math.max(1, current - 2); i <= Math.min(total, current + 2); i++) {
    pages.push(i)
  }
  if (current < total - 2) {
    if (current < total - 3) pages.push('...')
    pages.push(total)
  }
  return pages
})

watch([currentPage, pageSize], () => {
  fetchRequests()
})

onMounted(() => fetchRequests())

const viewDetail = (id) => {
  router.push(`/admin/return-requests/${id}`)
}
</script>

<template>
  <div class="py-4 px-4">
    <h2 class="fw-extrabold mb-4" style="font-size: 2rem">
      <span style="font-size:2rem;vertical-align:middle;">📦</span>
      Quản lý yêu cầu trả hàng
    </h2>

    <div class="row gy-2 gx-3 align-items-center mb-3 flex-wrap">
      <div class="col-md-3">
        <select v-model="filterStatus" class="form-select">
          <option value="">-- Tất cả trạng thái --</option>
        <option value="PENDING">Chờ duyệt</option>
        <option value="APPROVED">Đã duyệt</option>
        <option value="WAIT_FOR_PICKUP">Tạo lệnh lấy hàng</option>
        <option value="RETURNED">Đã nhận hàng hoàn trả</option>
        <option value="REFUNDED">Đã hoàn tiền</option>
        <option value="REJECTED">Từ chối</option>
        <option value="CANCELLED">Đã huỷ</option>
        <option value="COMPLETED">Hoàn tất trả hàng</option>
        </select>
      </div>

      <div class="col-md-3 d-flex gap-2 flex-wrap">
        <button class="btn btn-dark flex-fill" @click="searchRequests">🔍 Tìm kiếm</button>
        <button class="btn btn-secondary flex-fill" @click="clearFilter">Clear</button>
      </div>
    </div>

    <div class="table-responsive mt-4">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
          <tr>
            <th class="text-center" style="width: 5%">STT</th>
            <th class="text-center">Mã đơn hàng</th>
            <th class="text-center">Người mua</th>
            <th class="text-center">SĐT</th>
            <th class="text-center">Trạng thái</th>
            <th class="text-center">Ngày tạo</th>
            <th class="text-center" style="width: 10%">Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!requests.length && !loading">
            <td colspan="8" class="text-center">Không có yêu cầu nào.</td>
          </tr>
          <tr v-for="(r, idx) in requests" :key="r.id">
            <td class="text-center">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
            <td class="text-center">{{ r.order?.orderCode }}</td>
            <td>{{ r.order?.fullname }}</td>
            <td class="text-center">{{ r.order?.phoneNumber }}</td>
            <td class="text-center">
              <span
                :class="{
                    'badge bg-warning text-dark': r.status === 'PENDING',
                    'badge bg-primary': r.status === 'APPROVED',
                    'badge bg-info text-dark': r.status === 'WAIT_FOR_PICKUP',
                    'badge bg-secondary': r.status === 'RETURNED',
                    'badge bg-success': r.status === 'COMPLETED',
                    'badge bg-dark': r.status === 'REFUNDED',
                    'badge bg-danger': r.status === 'REJECTED',
                    'badge bg-light text-dark': r.status === 'CANCELLED'
                    }"

                style="font-size: 13px; padding: 5px 10px; border-radius: 20px"
              >
               {{ statusLabels[r.status] || r.status }}
              </span>
            </td>
            <td class="text-center">{{ new Date(r.createdAt).toLocaleDateString('vi-VN') }}</td>
            <td class="text-center">
              <button class="btn btn-sm btn-primary" @click="viewDetail(r.id)">Chi tiết</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalElements > 0" class="pagination-wrapper">
      <div class="page-size-selector">
        <span class="me-2 text-muted">Hiển thị</span>
        <select class="form-select form-select-sm me-2 custom-select" v-model="pageSize" @change="changePageSize($event.target.value)">
          <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="size">{{ size }}</option>
        </select>
        <span class="text-muted">yêu cầu</span>
      </div>

      <div v-if="totalPages > 1" class="pagination-controls">
        <nav aria-label="Page navigation">
          <ul class="pagination pagination-sm mb-0 custom-pagination">
            <li class="page-item" :class="{ disabled: currentPage === 1 }">
              <button class="page-link custom-page-link" @click="goToPage(currentPage - 1)" :disabled="currentPage === 1">«</button>
            </li>
            <li v-for="page in visiblePages" :key="page" class="page-item" :class="{ active: page === currentPage, disabled: page === '...' }">
              <button v-if="page !== '...'" class="page-link custom-page-link" :class="{ 'active-page': page === currentPage }" @click="goToPage(page)">
                {{ page }}
              </button>
              <span v-else class="page-link custom-page-link disabled">...</span>
            </li>
            <li class="page-item" :class="{ disabled: currentPage === totalPages }">
              <button class="page-link custom-page-link" @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages">»</button>
            </li>
          </ul>
        </nav>
      </div>

      <div class="pagination-info">
        <span class="text-muted">
          Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong tổng số {{ paginationInfo.total }} yêu cầu
        </span>
      </div>
    </div>

    <ShowToastComponent ref="toastRef" />
  </div>
</template>

<style scoped>
/* Tái sử dụng style từ trang người dùng */
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-top: 1px solid #e9ecef;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 12px;
  margin-top: 20px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.page-size-selector {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #6c757d;
}
.custom-select {
  width: 80px !important;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.custom-select:focus {
  border-color: #212529;
  box-shadow: 0 0 0 3px rgba(33, 37, 41, 0.1);
}
.pagination-controls {
  flex: 1;
  display: flex;
  justify-content: center;
  max-width: 600px;
}
.custom-pagination {
  gap: 4px;
}
.custom-page-link {
  border: 2px solid #e9ecef;
  color: #495057;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-weight: 500;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  min-width: 40px;
  text-align: center;
}
.custom-page-link:hover:not(:disabled):not(.disabled) {
  background: #212529;
  border-color: #212529;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(33, 37, 41, 0.3);
}
.custom-page-link:disabled {
  background: #f8f9fa;
  border-color: #e9ecef;
  color: #adb5bd;
  cursor: not-allowed;
}
.active-page {
  background: linear-gradient(135deg, #212529 0%, #000000 100%) !important;
  border-color: #212529 !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(33, 37, 41, 0.4) !important;
  transform: translateY(-1px);
}
.page-item.active .custom-page-link {
  background: linear-gradient(135deg, #212529 0%, #000000 100%) !important;
  border-color: #212529 !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(33, 37, 41, 0.4) !important;
}
.pagination-info {
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}
</style>
