<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { getAllUsers, lockUser } from '@/service/UserApis'
import ShowToastComponent from "@/components/ShowToastComponent.vue"
import { useAuthStore } from "@/stores/Auth.js"

const authStore = useAuthStore()

const props = defineProps({
  type: {
    type: String,
    required: true,
    validator: (value) => ['staff', 'customer'].includes(value)
  }
})

const users = ref([])
const loading = ref(false)
const toastRef = ref(null)

const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)

const filterName = ref('')
const filterStatus = ref('')

const PAGE_SIZE_OPTIONS = [5, 10, 15, 20, 25]

const fetchUsers = async (useFilter = false) => {
  loading.value = true
  try {
    let roleToQuery = props.type === 'staff' ? 'Staff' : 'Customer'
    let paramsName = useFilter ? filterName.value : ''
    let paramsStatus = useFilter ? filterStatus.value : ''

    // Thêm tham số name & status tuỳ BE của bạn xử lý
    const res = await getAllUsers(roleToQuery, currentPage.value - 1, pageSize.value, paramsName, paramsStatus)
    const data = res.data.data || {}
    users.value = data.content || []
    totalPages.value = data.totalPages || 1
    totalElements.value = data.totalElements || 0

    if (currentPage.value > totalPages.value) currentPage.value = totalPages.value
    if (currentPage.value < 1) currentPage.value = 1
  } catch (e) {
    showToast('Không tải được danh sách người dùng.', 'error')
  } finally {
    loading.value = false
  }
}

const searchUsers = () => {
  currentPage.value = 1
  fetchUsers(true)
}

const clearFilter = () => {
  filterName.value = ''
  filterStatus.value = ''
  currentPage.value = 1
  fetchUsers(false)
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

const handleDeleteUser = async (id) => {
  const user = users.value.find(u => u.id === id)
  if (!user) return

  if (!user.isActive) {
    showToast('Tài khoản này đã bị khoá rồi.', 'warning')
    return
  }

  if (!confirm('Bạn có chắc muốn khoá người dùng này?')) return

  try {
    await lockUser(id)
    showToast('Đã khoá tài khoản!', 'success')
    await fetchUsers(true)
  } catch (e) {
    showToast('Không thể khoá tài khoản.', 'error')
  }
}



// Watch props.type để reload role
watch(() => props.type, () => {
  currentPage.value = 1
  fetchUsers(false)
})

// Watch tự động gọi khi đổi page & size
watch([currentPage, pageSize], () => {
  const useFilter = !!(filterName.value || filterStatus.value)
  fetchUsers(useFilter)
})

onMounted(() => fetchUsers(false))
</script>


<template>
  <div class="py-4 px-4">
    <h2 class="fw-extrabold mb-4" style="font-size: 2rem">
      <span style="font-size:2rem;vertical-align:middle;">👥</span>
      Quản lý {{ props.type === 'staff' ? 'nhân viên' : 'khách hàng' }}
    </h2>
  <div class="row gy-2 gx-3 align-items-center mb-3 flex-wrap">
  <div class="col-md-3">
    <input type="text" class="form-control" placeholder="🔍 Tìm theo tên " v-model="filterName">
  </div>
  
  <div class="col-md-3">
    <select v-model="filterStatus" class="form-select">
      <option value="">-- Tất cả trạng thái --</option>
      <option :value="true">Hoạt động</option>
      <option :value="false">Đã khoá</option>
    </select>
  </div>

  <div class="col-md-3 d-flex gap-2 flex-wrap">
    <button class="btn btn-dark flex-fill" style="height: 38px" @click="searchUsers">🔍 Tìm kiếm</button>
    <button class="btn btn-secondary flex-fill" style="height: 38px" @click="clearFilter">Clear</button>
  </div>

  <div class="col-md-3 text-end">
    <router-link
      :to="`/admin/users/${props.type}/create`"
      class="btn btn-primary w-100"
      style="height: 38px"
    >
      Thêm {{ props.type === 'staff' ? 'nhân viên' : 'khách hàng' }}
    </router-link>
  </div>
</div>

    <div class="table-responsive mt-4">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
          <tr>
            <th class="text-center" style="width: 5%">STT</th>
            <th class="text-center">Họ tên</th>
            <th class="text-center">Email</th>
            <th class="text-center">SĐT</th>
            <th class="text-center">Trạng thái</th>
            <th v-if="authStore.userRole === 'Admin'" class="text-center" style="width: 16%">Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!users.length && !loading">
            <td colspan="6" class="text-center">Không có người dùng nào.</td>
          </tr>
          <tr v-for="(u, idx) in users" :key="u.id">
            <td class="text-center">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
            <td>{{ u.fullname }}</td>
            <td>{{ u.email }}</td>
            <td class="text-center">{{ u.phoneNumber }}</td>
            <td class="text-center">
              <span :class="u.isActive ? 'badge bg-success' : 'badge bg-secondary'"
                    style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
                {{ u.isActive ? 'Hoạt động' : 'Đã khoá' }}
              </span>
            </td>
            <td v-if="authStore.userRole === 'Admin'" class="text-center">
              <router-link
                :to="`/admin/users/${props.type}/update?id=${u.id}`"
                class="btn btn-sm btn-warning me-2"
              >
                Sửa
              </router-link>
              <button class="btn btn-sm btn-danger" @click="handleDeleteUser(u.id)">
  Khoá
  </button>

            </td>
          </tr>
        </tbody>
      </table>
    </div>

  <div v-if="totalElements > 0" class="pagination-wrapper">
  <div class="page-size-selector">
    <span class="me-2 text-muted">Hiển thị</span>
    <select class="form-select form-select-sm me-2 custom-select" v-model="pageSize"
      @change="changePageSize($event.target.value)">
      <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="size">{{ size }}</option>
    </select>
    <span class="text-muted">người dùng</span>
  </div>

  <div v-if="totalPages > 1" class="pagination-controls">
    <nav aria-label="Page navigation">
      <ul class="pagination pagination-sm mb-0 custom-pagination">
        <li class="page-item" :class="{ disabled: currentPage === 1 }">
          <button class="page-link custom-page-link" @click="goToPage(currentPage - 1)"
            :disabled="currentPage === 1">«</button>
        </li>
        <li v-for="page in visiblePages" :key="page" class="page-item"
          :class="{ active: page === currentPage, disabled: page === '...' }">
          <button v-if="page !== '...'" class="page-link custom-page-link"
            :class="{ 'active-page': page === currentPage }" @click="goToPage(page)">{{ page }}</button>
          <span v-else class="page-link custom-page-link disabled">...</span>
        </li>
        <li class="page-item" :class="{ disabled: currentPage === totalPages }">
          <button class="page-link custom-page-link" @click="goToPage(currentPage + 1)"
            :disabled="currentPage === totalPages">»</button>
        </li>
      </ul>
    </nav>
  </div>

  <div class="pagination-info">
    <span class="text-muted">
      Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong tổng số {{ paginationInfo.total }} người dùng
    </span>
  </div>
</div>


    <ShowToastComponent ref="toastRef" />
  </div>
</template>

<style scoped>
/* COPY từ file mẫu */
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
</style>