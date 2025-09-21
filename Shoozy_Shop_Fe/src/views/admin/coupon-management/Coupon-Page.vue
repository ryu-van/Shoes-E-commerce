<script setup>
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import couponApi from '@/service/CouponApi'
import { debounce } from 'lodash-es'
import { useRouter } from 'vue-router'
import ConfirmModal from '@/components/ConfirmModal.vue'
import ShowToastComponent from '@/components/ShowToastComponent.vue'
import {
  connectWebSocket,
  disconnectWebSocket,
  addMessageListener,
  removeMessageListener,
  addCouponListener,        // ✅ thêm
  removeCouponListener      // ✅ thêm
} from '@/service/Websocket'

const router = useRouter()

const coupons = ref([])
const totalPages = ref(1)
const totalElements = ref(0)
const currentPage = ref(1)
const pageSize = ref(5)
const loading = ref(false)
const showFilters = ref(false)
const error = ref('')
const toastRef = ref(null)

// User info & role check
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const roleId = localStorage.getItem('roleId') ||
               localStorage.getItem('userRole') ||
               user.value?.roleId ||
               user.value?.role?.id ||
               user.value?.role_id

const isStaffOrCustomer = (role) => {
  if (!role) return false
  const roleStr = String(role).toLowerCase()
  return roleStr === 'staff' || roleStr === '2' || roleStr === 'nhân viên' ||
         roleStr === 'customer' || roleStr === '3' || roleStr === 'khách hàng'
}

// Confirm modal state
const showConfirmModal = ref(false)
const confirmModalConfig = ref({
  title: '',
  message: '',
  type: 'warning',
  confirmText: 'Xác nhận',
  action: null
})

const filters = ref({
  keyword: '',
  dateAfter: '',
  dateBefore: '',
  status: ''
})

const PAGE_SIZE_OPTIONS = [5, 10, 15, 20, 25]
const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 0, label: 'Sắp diễn ra' },
  { value: 1, label: 'Đang hoạt động' },
  { value: 2, label: 'Đã hết hạn' },
  { value: 3, label: 'Đã xóa' },
  { value: 4, label: 'Hết số lượng' }
]

const buildApiParams = () => {
  const params = {
    page: currentPage.value - 1,
    limit: pageSize.value
  }
  if (filters.value.keyword) params.keyword = filters.value.keyword
  if (filters.value.dateAfter) params.startDate = filters.value.dateAfter
  if (filters.value.dateBefore) params.expirationDate = filters.value.dateBefore
  if (filters.value.status !== '' && filters.value.status !== null) params.status = filters.value.status
  return params
}

const fetchCoupons = async () => {
  loading.value = true
  error.value = ''
  try {
    const params = buildApiParams()
    const res = await couponApi.getCoupons(params)
    if (res.data && res.data.status === 200) {
      const data = res.data.data
      if (Array.isArray(data)) {
        coupons.value = data
        totalPages.value = 1
        totalElements.value = data.length
      } else if (data && typeof data === 'object') {
        const { coupons: list, totalPage, totalElements: total } = data
        coupons.value = Array.isArray(list) ? list : []
        totalPages.value = totalPage || 1
        totalElements.value = total || coupons.value.length
      } else {
        coupons.value = []
        totalPages.value = 1
        totalElements.value = 0
        error.value = 'Dữ liệu trả về không hợp lệ.'
      }
      if (currentPage.value > totalPages.value) currentPage.value = totalPages.value
      if (currentPage.value < 1) currentPage.value = 1
      error.value = ''
    } else {
      error.value = res.data?.message || 'Không thể tải danh sách coupon. Vui lòng thử lại.'
    }
  } catch (err) {
    error.value = 'Không thể tải danh sách coupon. Vui lòng thử lại.'
    console.error('Error fetching coupons:', err)
  } finally {
    loading.value = false
  }
}

const debouncedFetch = debounce(fetchCoupons, 350)

watch(filters, () => {
  currentPage.value = 1
  debouncedFetch()
}, { deep: true })

watch([currentPage, pageSize], fetchCoupons)
onMounted(fetchCoupons)

/* ===================== WEBSOCKET HOOKS ===================== */
// Vẫn giữ để tương thích format cũ bắn vào /topic/refresh
const handleWsMessage = (msg) => {
  // msg = { type, action, payload: { couponId, code, quantity, status, ... }, timestamp }
  if (msg?.type === 'coupon' && msg?.action === 'DECREMENT') {
    const { couponId, quantity, status } = msg.payload || {}
    const idx = coupons.value.findIndex(c => c.id === couponId)
    if (idx !== -1) {
      if (typeof quantity !== 'undefined') coupons.value[idx].quantity = quantity
      if (typeof status !== 'undefined')   coupons.value[idx].status = status
      showToast(`Cập nhật mã ${getCouponCode(coupons.value[idx])}: còn ${quantity}`, 'success')
    }
  }
}

// ✅ Handler cho các kênh coupon chuyên dụng: /topic/admin/coupon, /topic/coupon/status, /topic/refresh (format mới)
const handleCouponRealtime = (msg) => {
  if (!msg) return

  // Hỗ trợ cả 2 format (mới & cũ)
  const couponId = msg.couponId ?? msg?.payload?.couponId
  const codeRaw  = msg.code ?? msg?.payload?.code
  const code     = codeRaw ? String(codeRaw).toUpperCase() : undefined
  const qty      = msg.quantity ?? msg?.payload?.quantity
  const status   = msg.status ?? msg?.payload?.status
  const action   = msg.action || msg.type

  // Tìm item trong bảng hiện tại
  let idx = -1
  if (couponId != null) {
    idx = coupons.value.findIndex(c => Number(c.id) === Number(couponId))
  }
  if (idx === -1 && code) {
    idx = coupons.value.findIndex(c =>
      String(c.code || c.couponCode || '').toUpperCase() === code
    )
  }
  if (idx === -1) return // không có trong trang đang hiển thị

  // Hết lượt / out of stock
  if (action === 'OUT_OF_STOCK' || msg.type === 'COUPON_OUT_OF_STOCK') {
    coupons.value[idx].quantity = 0
    coupons.value[idx].status = 4 // Hết số lượng
    showToast(`Mã ${getCouponCode(coupons.value[idx])} đã hết lượt`, 'warning')
    return
  }

  // Giảm lượt / cập nhật số lượng & trạng thái
  if (typeof qty !== 'undefined')   coupons.value[idx].quantity = qty
  if (typeof status !== 'undefined') coupons.value[idx].status   = status

  if (action === 'DECREMENT' && typeof qty === 'number') {
    showToast(`Cập nhật mã ${getCouponCode(coupons.value[idx])}: còn ${qty}`, 'success')
  }
}

onMounted(() => {
  connectWebSocket()
  addMessageListener(handleWsMessage)       // kênh /topic/refresh (format cũ)
  addCouponListener(handleCouponRealtime)   // ✅ lắng nghe kênh coupon chuyên dụng
})

onBeforeUnmount(() => {
  removeMessageListener(handleWsMessage)
  removeCouponListener(handleCouponRealtime) // ✅ huỷ lắng nghe
  // disconnectWebSocket() // nếu muốn giữ kết nối toàn cục thì không cần gọi
})
/* =================== END WEBSOCKET HOOKS =================== */

const resetFilters = () => {
  filters.value = { keyword: '', dateAfter: '', dateBefore: '', status: '' }
  currentPage.value = 1
  showFilters.value = false
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

const editCoupon = (id) => {
  if (isStaffOrCustomer(roleId)) {
    showToast('Bạn không có quyền thực hiện thao tác này!', 'error')
    return
  }
  router.push(`/admin/coupons/update/${id}`)
}

const viewCoupon = (id) => router.push(`/admin/coupons/${id}`)

const createCoupon = () => {
  if (isStaffOrCustomer(roleId)) {
    showToast('Bạn không có quyền thực hiện thao tác này!', 'error')
    return
  }
  router.push('/admin/coupons/create')
}

const showDeleteConfirm = (id) => {
  if (isStaffOrCustomer(roleId)) {
    showToast('Bạn không có quyền thực hiện thao tác này!', 'error')
    return
  }
  confirmModalConfig.value = {
    title: 'Xóa mã giảm giá',
    message: 'Bạn có chắc chắn muốn xóa mã giảm giá này không?',
    type: 'danger',
    confirmText: 'Xóa',
    action: () => deleteCoupon(id)
  }
  showConfirmModal.value = true
}

const deleteCoupon = async (id) => {
  loading.value = true
  try {
    const res = await couponApi.deleteCoupon(id)
    if (res.data && res.data.status === 200) {
      await fetchCoupons()
      showToast('Xóa mã giảm giá thành công!', 'success')
    } else {
      showToast(res.data?.message || 'Xoá coupon thất bại!', 'error')
    }
  } catch (err) {
    showToast('Xoá coupon thất bại!', 'error')
  } finally {
    loading.value = false
  }
}

const showRestoreConfirm = (id) => {
  if (isStaffOrCustomer(roleId)) {
    showToast('Bạn không có quyền thực hiện thao tác này!', 'error')
    return
  }
  confirmModalConfig.value = {
    title: 'Khôi phục mã giảm giá',
    message: 'Bạn có chắc chắn muốn khôi phục mã giảm giá này không?',
    type: 'success',
    confirmText: 'Khôi phục',
    action: () => restoreCoupon(id)
  }
  showConfirmModal.value = true
}

const restoreCoupon = async (id) => {
  loading.value = true
  try {
    const res = await couponApi.activeCoupon(id, true)
    if (res.data && res.data.status === 200) {
      await fetchCoupons()
      showToast('Khôi phục mã giảm giá thành công!', 'success')
    } else {
      showToast(res.data?.message || 'Khôi phục coupon thất bại!', 'error')
    }
  } catch (err) {
    showToast('Khôi phục coupon thất bại!', 'error')
  } finally {
    loading.value = false
  }
}

const canRestoreCoupon = (coupon) => {
  if (coupon.status !== 3) return false
  const today = new Date()
  const start = coupon.startDate ? new Date(coupon.startDate) : null
  const end = coupon.expirationDate ? new Date(coupon.expirationDate) : null
  if (start && today < start) return true
  if (start && end && today >= start && today <= end) return true
  return false
}

const canDeleteCoupon = (coupon) => {
  return coupon.status !== 3 && coupon.status !== 2
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
    if (current > 4) {
      pages.push('...')
    }
  }

  const start = Math.max(1, current - 2)
  const end = Math.min(total, current + 2)

  for (let i = start; i <= end; i++) {
    if (!pages.includes(i)) {
      pages.push(i)
    }
  }

  if (current < total - 2) {
    if (current < total - 3) {
      pages.push('...')
    }
    if (!pages.includes(total)) {
      pages.push(total)
    }
  }
  return pages
})

const isValidDateRange = computed(() => {
  if (!filters.value.dateAfter || !filters.value.dateBefore) return true
  return filters.value.dateAfter <= filters.value.dateBefore
})

const hasData = computed(() => Array.isArray(coupons.value) && coupons.value.length > 0)

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const getStatusInfo = (status) => {
  switch (status) {
    case 0: return { text: 'Sắp diễn ra',   badge: 'bg-info' }
    case 1: return { text: 'Đang hoạt động', badge: 'bg-success' }
    case 2: return { text: 'Đã hết hạn',     badge: 'bg-warning' }
    case 3: return { text: 'Đã xóa',         badge: 'bg-secondary' }
    case 4: return { text: 'Hết số lượng',   badge: 'bg-danger' }
    default: return { text: 'Không xác định', badge: 'bg-dark' }
  }
}

const getCouponCode = (coupon) => coupon.code || coupon.couponCode || coupon.discountCode || 'N/A'

const getCouponTypeDisplay = (coupon) => {
  if (typeof coupon.type === 'boolean') return coupon.type ? 'Phần trăm' : 'Tiền'
  if (typeof coupon.type === 'string') {
    return ['percentage', 'percent'].includes(coupon.type.toLowerCase()) ? 'Phần trăm' : 'Tiền'
  }
  return 'Không xác định'
}

const formatCouponValue = (coupon) => {
  if (!coupon.value && coupon.value !== 0) return 'N/A'
  if (typeof coupon.type === 'boolean' && coupon.type) return `${coupon.value}%`
  if (typeof coupon.type === 'string' && ['percentage', 'percent'].includes(coupon.type.toLowerCase())) {
    return `${coupon.value}%`
  }
  return coupon.value.toLocaleString('vi-VN')
}

const handleConfirmAction = () => {
  if (confirmModalConfig.value.action) {
    confirmModalConfig.value.action()
  }
  showConfirmModal.value = false
}

const showToast = (message, type = 'success') => {
  if (toastRef.value) {
    toastRef.value.showToast(message, type)
  } else {
    console.log(`${type.toUpperCase()}: ${message}`)
  }
}
</script>


<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
      <h2 class="fw-extrabold m-0">🎫 Quản lý Mã giảm giá</h2>
      <button class="btn btn-primary" style="height: 38px" @click="createCoupon" :disabled="loading">
        Thêm mã giảm giá
      </button>
    </div>

    <!-- Filters Section -->
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <form class="row g-3" @submit.prevent>
          <div class="col-md-4">
            <label class="form-label">
              <i class="fas fa-search me-1"></i>
              Tìm kiếm
            </label>
            <input type="text" class="form-control filter-input" placeholder="🔍 Tên hoặc mã coupon..."
                   v-model.trim="filters.keyword" maxlength="100"/>
          </div>
          <div class="col-md-2">
            <label class="form-label">
              <i class="fas fa-calendar-alt me-1"></i>
              Từ ngày
            </label>
            <input type="date" class="form-control" v-model="filters.dateAfter"
                   :max="filters.dateBefore || undefined"/>
          </div>
          <div class="col-md-2">
            <label class="form-label">
              <i class="fas fa-calendar-alt me-1"></i>
              Đến ngày
            </label>
            <input type="date" class="form-control" v-model="filters.dateBefore"
                   :min="filters.dateAfter || undefined"/>
            <div v-if="!isValidDateRange" class="form-text text-danger">
              Ngày kết thúc phải sau ngày bắt đầu
            </div>
          </div>
          <div class="col-md-2">
            <label class="form-label">
              <i class="fas fa-toggle-on me-1"></i>
              Trạng thái
            </label>
            <select class="form-select" v-model="filters.status">
              <option v-for="option in STATUS_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
          <div class="col-md-2 d-flex align-items-end gap-3">
            <button type="button" class="btn btn-secondary flex-fill" @click="resetFilters">
              Xóa lọc
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Error Alert -->
    <div v-if="error" class="alert alert-danger alert-dismissible fade show mb-3" role="alert">
      <i class="fas fa-exclamation-triangle me-2"></i>
      {{ error }}
      <button type="button" class="btn-close" @click="error = ''" aria-label="Close"></button>
    </div>

    <!-- Main Table -->
    <div style="margin-top: 32px" class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
        <tr>
          <th class="text-center" style="width: 5%">STT</th>
          <th class="text-center">Tên coupon</th>
          <th class="text-center">Mã coupon</th>
          <th class="text-center">Loại</th>
          <th class="text-center">Giá trị</th>
          <th class="text-center">Số lượng</th>
          <th class="text-center">Điều kiện</th>
          <th class="text-center">Thời gian</th>
          <th class="text-center">Trạng thái</th>
          <th class="text-center" style="width: 20%">Hành động</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="!hasData && !loading">
          <td colspan="10" class="text-center">Không có mã giảm giá nào.</td>
        </tr>
        <tr v-for="(coupon, index) in coupons" :key="coupon.id">
          <td class="text-center">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
          <td><strong>{{ coupon.name }}</strong></td>
          <td class="text-center"><code>{{ getCouponCode(coupon) }}</code></td>
          <td class="text-center">
              <span class="badge bg-secondary" style="font-size: 14px; padding: 5px 10px; border-radius: 20px">
                {{ getCouponTypeDisplay(coupon) }}
              </span>
          </td>
          <td class="text-center"><strong>{{ formatCouponValue(coupon) }}</strong></td>
          <td class="text-center">{{ coupon.quantity }}</td>
          <td>{{ coupon.condition }}</td>
          <!-- Thời gian -->
          <td class="text-center">
            <div>
                <span>
                  <i class="fas fa-play-circle text-success me-1"></i>{{ formatDate(coupon.startDate) || 'Chưa đặt' }}
                </span>
            </div>
            <div>
              <i class="fas fa-stop-circle text-danger me-1"></i>{{
                formatDate(coupon.expirationDate) || 'Không hạn'
              }}
            </div>
          </td>
          <td class="text-center">
              <span :class="['badge', getStatusInfo(coupon.status).badge]"
                    style="font-size: 14px; padding: 5px 10px; border-radius: 20px">
                {{ getStatusInfo(coupon.status).text }}
              </span>
          </td>
          <td class="text-center vertical-mid">
            <button class="btn btn-sm btn-warning me-2" @click="viewCoupon(coupon.id)" title="Chi tiết">
              Chi tiết
            </button>
            <button class="btn btn-sm btn-primary me-2" @click="editCoupon(coupon.id)" title="Chỉnh sửa">
              Sửa
            </button>
            <button v-if="canDeleteCoupon(coupon)" class="btn btn-sm btn-danger" @click="showDeleteConfirm(coupon.id)"
                    title="Xóa">
              Xóa
            </button>
            <button v-if="canRestoreCoupon(coupon)" class="btn btn-sm btn-success ms-1"
                    @click="showRestoreConfirm(coupon.id)" title="Khôi phục">
              Khôi phục
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="coupons.length && totalPages > 1" class="mt-4">
      <div class="pagination-wrapper">
        <div class="page-size-selector">
          <span class="me-2 text-muted">Hiển thị</span>
          <select class="form-select form-select-sm me-2 custom-select" v-model="pageSize"
                  @change="changePageSize($event.target.value)">
            <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="size">
              {{ size }}
            </option>
          </select>
          <span class="text-muted">mã giảm giá</span>
        </div>

        <div class="pagination-controls">
          <nav aria-label="Page navigation">
            <ul class="pagination pagination-sm mb-0 custom-pagination">
              <li class="page-item" :class="{ disabled: currentPage === 1 }">
                <button class="page-link custom-page-link" @click="goToPage(currentPage - 1)"
                        :disabled="currentPage === 1">
                  «
                </button>
              </li>
              <li v-for="page in visiblePages" :key="page" class="page-item"
                  :class="{ active: page === currentPage, disabled: page === '...' }">
                <button v-if="page !== '...'" class="page-link custom-page-link"
                        :class="{ 'active-page': page === currentPage }" @click="goToPage(page)">
                  {{ page }}
                </button>
                <span v-else class="page-link custom-page-link disabled">...</span>
              </li>
              <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                <button class="page-link custom-page-link" @click="goToPage(currentPage + 1)"
                        :disabled="currentPage === totalPages">
                  »
                </button>
              </li>
            </ul>
          </nav>
        </div>

        <div class="pagination-info">
          <span class="text-muted">
            Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong tổng số {{ paginationInfo.total }} mã
            giảm giá
          </span>
        </div>
      </div>
    </div>
  </div>

  <!-- Confirm Modal -->
  <ConfirmModal
      :show="showConfirmModal"
      :title="confirmModalConfig.title"
      :message="confirmModalConfig.message"
      :type="confirmModalConfig.type"
      :confirm-text="confirmModalConfig.confirmText"
      :loading="loading"
      @confirm="handleConfirmAction"
      @close="showConfirmModal = false"
  />

  <!-- ShowToastComponent -->
  <ShowToastComponent ref="toastRef" />
</template>

<style scoped>
.transition-transform {
  transition: transform 0.2s ease;
}

.rotate-180 {
  transform: rotate(180deg);
}

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

@media (max-width: 768px) {
  .pagination-wrapper {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .pagination-controls {
    order: 1;
  }

  .page-size-selector {
    order: 2;
    justify-content: center;
  }

  .pagination-info {
    order: 3;
    text-align: center;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pagination-wrapper {
  animation: fadeIn 0.3s ease-out;
}

.table td,
.table th {
  vertical-align: middle;
}

tr:hover {
  background-color: rgba(0, 123, 255, 0.05);
}
</style>
