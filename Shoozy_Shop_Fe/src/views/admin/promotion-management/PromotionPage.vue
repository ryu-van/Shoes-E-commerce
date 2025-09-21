<script setup>
import { ref, onMounted, watch, computed } from "vue";
import { debounce } from "lodash-es";
import promotionApi from "@/service/PromotionApi";
import { useRouter } from "vue-router";
import ConfirmModal from "@/components/ConfirmModal.vue";
import ShowToastComponent from "@/components/ShowToastComponent.vue";

const router = useRouter();

/* ===== User & Role ===== */
const user = ref(JSON.parse(localStorage.getItem("user") || "{}"));
const roleId =
  localStorage.getItem("roleId") ||
  localStorage.getItem("userRole") ||
  user.value?.roleId ||
  user.value?.role?.id ||
  user.value?.role_id;

const isStaff = (role) => {
  if (!role) return false;
  const roleStr = String(role).toLowerCase();
  return roleStr === "staff" || roleStr === "2" || roleStr === "nhân viên";
};

/* ===== Constants ===== */
const PROMOTION_STATUS = { UPCOMING: 0, LAUNCHING: 1, EXPIRED: 2, DELETED: 3 };
const PAGE_SIZE_OPTIONS = [5, 10, 15, 20, 25];
const STATUS_OPTIONS = [
  { value: "", label: "Tất cả" },
  { value: PROMOTION_STATUS.UPCOMING, label: "Sắp diễn ra" },
  { value: PROMOTION_STATUS.LAUNCHING, label: "Đang áp dụng" },
  { value: PROMOTION_STATUS.EXPIRED, label: "Đã hết hạn" },
  { value: PROMOTION_STATUS.DELETED, label: "Đã xóa" },
];

/* ===== State ===== */
const promotions = ref([]);
const totalPages = ref(0);
const totalElements = ref(0);
const currentPage = ref(1);
const pageSize = ref(5);
const loading = ref(false);
const showFilters = ref(false);
const error = ref("");
const toastRef = ref(null);

/* Confirm modal */
const showConfirmModal = ref(false);
const confirmModalConfig = ref({
  title: "",
  message: "",
  type: "warning",
  confirmText: "Xác nhận",
  action: null,
});

/* ===== Filters =====
   FIX: đổi dateBefore/dateAfter -> startDate/endDate cho khớp Backend
   Giá trị nên là 'yyyy-MM-dd' nếu dùng <input type="date">.
*/
const filters = ref({
  keyword: "",
  startDate: "", // ISO yyyy-MM-dd
  endDate: "", // ISO yyyy-MM-dd
  status: "",
});

/* ===== Helpers ===== */
const showToast = (message, type = "success") => {
  toastRef.value?.showToast(message, type);
};

const toIsoDate = (val) => {
  if (!val) return "";
  // nếu đã ISO yyyy-MM-dd thì trả về luôn
  if (/^\d{4}-\d{2}-\d{2}$/.test(val)) return val;
  // cố gắng parse & format về yyyy-MM-dd
  const d = new Date(val);
  if (isNaN(d.getTime())) return "";
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

/* Build params đúng Backend */
const buildApiParams = () => {
  const params = {
    page: currentPage.value - 1, // server 0-based
    limit: pageSize.value,
  };

  if (filters.value.keyword?.trim())
    params.keyword = filters.value.keyword.trim();

  // FIX: gửi startDate/endDate (ISO)
  const s = toIsoDate(filters.value.startDate);
  const e = toIsoDate(filters.value.endDate);
  if (s) params.startDate = s;
  if (e) params.endDate = e;

  if (
    filters.value.status !== "" &&
    filters.value.status !== undefined &&
    filters.value.status !== null
  ) {
    params.status = parseInt(filters.value.status);
  }

  return params;
};

/* ===== Fetch ===== */
const fetchPromotions = async () => {
  loading.value = true;
  error.value = "";
  try {
    const params = buildApiParams();
    const response = await promotionApi.getPromotions(params);
    const apiData = response?.data?.data || response?.data || {};
    promotions.value = apiData.promotions || [];
    totalPages.value = apiData.totalPage || 0;
    totalElements.value = apiData.totalElements || 0;
  } catch (err) {
    if (err?.response?.status === 403) {
      error.value = "Bạn không có quyền truy cập trang này!";
      showToast("Bạn không có quyền truy cập trang này!", "error");
    } else {
      error.value = "Không thể tải danh sách khuyến mãi. Vui lòng thử lại.";
    }
    console.error("Error fetching promotions:", err);
  } finally {
    loading.value = false;
  }
};

const debouncedFetch = debounce(fetchPromotions, 300);

/* ===== Watchers ===== */

/* FIX: tự động hoán đổi nếu user nhập ngược; gọi fetch */
watch(
  filters,
  () => {
    // auto-swap nếu có cả 2 và endDate < startDate
    const s = toIsoDate(filters.value.startDate);
    const e = toIsoDate(filters.value.endDate);
    if (s && e && new Date(e) < new Date(s)) {
      const tmp = filters.value.startDate;
      filters.value.startDate = filters.value.endDate;
      filters.value.endDate = tmp;
    }
    currentPage.value = 1;
    debouncedFetch();
  },
  { deep: true }
);

watch([currentPage, pageSize], fetchPromotions);
onMounted(fetchPromotions);

/* Reset filters */
const resetFilters = () => {
  filters.value = { keyword: "", startDate: "", endDate: "", status: "" };
  currentPage.value = 1;
  showFilters.value = false;
};

/* Pagination */
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value && page !== "...") {
    currentPage.value = page;
  }
};

const paginationInfo = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value + 1;
  const end = Math.min(currentPage.value * pageSize.value, totalElements.value);
  return { start, end, total: totalElements.value };
});

const visiblePages = computed(() => {
  const current = currentPage.value;
  const total = totalPages.value;
  const pages = [];
  if (current > 3) {
    pages.push(1);
    if (current > 4) pages.push("...");
  }
  const start = Math.max(1, current - 2);
  const end = Math.min(total, current + 2);
  for (let i = start; i <= end; i++) if (!pages.includes(i)) pages.push(i);
  if (current < total - 2) {
    if (current < total - 3) pages.push("...");
    if (!pages.includes(total)) pages.push(total);
  }
  return pages;
});

const hasData = computed(() => promotions.value.length > 0);

/* Quyền */
const canEdit = computed(() => !isStaff(roleId));
const canDelete = computed(() => !isStaff(roleId));

/* ===== Status & Date utils ===== */

/* FIX: dùng camelCase từ API: startDate / expirationDate */
const getStatusInfo = (promo) => {
  const status = promo.status;
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const startDate = promo.startDate ? new Date(promo.startDate) : null;
  const expirationDate = promo.expirationDate
    ? new Date(promo.expirationDate)
    : null;
  if (startDate) startDate.setHours(0, 0, 0, 0);
  if (expirationDate) expirationDate.setHours(0, 0, 0, 0);

  let canRestore = false;
  let canDel = false;

  if (
    status !== PROMOTION_STATUS.DELETED &&
    status !== PROMOTION_STATUS.EXPIRED
  ) {
    canDel = true;
  }
  if (status === PROMOTION_STATUS.DELETED) {
    if (
      (startDate && startDate > today) ||
      (startDate &&
        expirationDate &&
        startDate <= today &&
        today <= expirationDate)
    ) {
      canRestore = true;
    }
  }

  switch (status) {
    case PROMOTION_STATUS.UPCOMING:
      return {
        class: "status-upcoming",
        text: "Sắp diễn ra",
        badgeClass: "badge bg-info",
        canDelete: canDel,
        canRestore,
      };
    case PROMOTION_STATUS.LAUNCHING:
      return {
        class: "status-active",
        text: "Đang áp dụng",
        badgeClass: "badge bg-success",
        canDelete: canDel,
        canRestore,
      };
    case PROMOTION_STATUS.EXPIRED:
      return {
        class: "status-expired",
        text: "Đã hết hạn",
        badgeClass: "badge bg-warning",
        canDelete: canDel,
        canRestore,
      };
    case PROMOTION_STATUS.DELETED:
      return {
        class: "status-deleted",
        text: "Đã xóa",
        badgeClass: "badge bg-secondary",
        canDelete: canDel,
        canRestore,
      };
    default:
      return {
        class: "status-unknown",
        text: "Không xác định",
        badgeClass: "badge bg-dark",
        canDelete: false,
        canRestore: false,
      };
  }
};

const formatDate = (dateString) => {
  if (!dateString) return "";
  try {
    return new Date(dateString).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  } catch {
    return "";
  }
};

/* FIX: logic khoảng ngày: hợp lệ khi endDate >= startDate */
const isValidDateRange = computed(() => {
  const s = toIsoDate(filters.value.startDate);
  const e = toIsoDate(filters.value.endDate);
  if (!s || !e) return true;
  return new Date(e) >= new Date(s);
});

/* Actions */
const editPromotion = (id) => {
  if (!canEdit.value)
    return showToast("Bạn không có quyền thực hiện thao tác này!", "error");
  router.push(`/admin/promotions/update/${id}`);
};
const viewPromotion = (id) => router.push(`/admin/promotions/${id}`);

const createPromotion = () => {
  if (!canEdit.value)
    return showToast("Bạn không có quyền thực hiện thao tác này!", "error");
  router.push("/admin/promotions/create");
};

const showDeleteConfirm = (id, name) => {
  if (!canDelete.value)
    return showToast("Bạn không có quyền thực hiện thao tác này!", "error");
  confirmModalConfig.value = {
    title: "Xóa khuyến mãi",
    message: `Bạn có chắc chắn muốn xóa khuyến mãi "${name}"?`,
    type: "danger",
    confirmText: "Xóa",
    action: () => deletePromotion(id),
  };
  showConfirmModal.value = true;
};

const deletePromotion = async (id) => {
  if (!id) return;
  try {
    await promotionApi.disableStatus(id);
    await fetchPromotions();
    showToast("Xóa khuyến mãi thành công!", "success");
  } catch (err) {
    showToast(
      err?.response?.status === 403
        ? "Bạn không có quyền thực hiện thao tác này!"
        : "Không thể xóa khuyến mãi. Vui lòng thử lại.",
      "error"
    );
  }
};

const showRestoreConfirm = (id, name) => {
  if (!canDelete.value)
    return showToast("Bạn không có quyền thực hiện thao tác này!", "error");
  confirmModalConfig.value = {
    title: "Khôi phục khuyến mãi",
    message: `Bạn có chắc chắn muốn khôi phục khuyến mãi "${name}"?`,
    type: "success",
    confirmText: "Khôi phục",
    action: () => restorePromotion(id),
  };
  showConfirmModal.value = true;
};

const restorePromotion = async (id) => {
  if (!id) return;
  try {
    await promotionApi.activeStatus(id);
    await fetchPromotions();
    showToast("Khôi phục khuyến mãi thành công!", "success");
  } catch (err) {
    showToast(
      err?.response?.status === 403
        ? "Bạn không có quyền thực hiện thao tác này!"
        : "Không thể khôi phục khuyến mãi. Vui lòng thử lại.",
      "error"
    );
  }
};

const handleConfirmAction = () => {
  if (confirmModalConfig.value.action) confirmModalConfig.value.action();
  showConfirmModal.value = false;
};
</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <div
      style="
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1.5rem;
      "
    >
      <h2 style="margin: 0; font-weight: bold">🎯 Quản lý khuyến mãi</h2>
      <button type="button" class="btn btn-primary" @click="createPromotion">
        Thêm khuyến mãi
      </button>
    </div>

    <div
      v-if="error"
      class="alert alert-danger alert-dismissible fade show mb-3"
      role="alert"
    >
      <i class="fas fa-exclamation-triangle me-2"></i>
      {{ error }}
      <button
        type="button"
        class="btn-close"
        @click="error = ''"
        aria-label="Close"
      ></button>
    </div>

    <!-- Filters -->
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <form class="row g-3" @submit.prevent>
          <div class="col-md-4">
            <label class="form-label">
              <i class="fas fa-search me-1"></i>
              Tìm kiếm
            </label>
            <input
              type="text"
              class="form-control filter-input"
              placeholder="🔍 Tên hoặc mã khuyến mãi..."
              v-model.trim="filters.keyword"
              maxlength="100"
            />
          </div>
          <div class="col-md-2">
            <label class="form-label">
              <i class="fas fa-calendar-alt me-1"></i>
              Từ ngày
            </label>
            <input
              type="date"
              class="form-control"
              v-model="filters.startDate"
              :max="filters.endDate || undefined"
            />
          </div>

          <div class="col-md-2">
            <label class="form-label">
              <i class="fas fa-calendar-alt me-1"></i>
              Đến ngày
            </label>
            <input
              type="date"
              class="form-control"
              v-model="filters.endDate"
              :min="filters.startDate || undefined"
            />
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
              <option
                v-for="option in STATUS_OPTIONS"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>
          </div>
          <div class="col-md-2 d-flex align-items-end gap-3">
            <button
              type="button"
              class="btn btn-secondary flex-fill"
              @click="resetFilters"
            >
              Xóa lọc
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Main Content Table -->
    <div style="margin-top: 32px" class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
          <tr>
            <th class="text-center" style="width: 5%">STT</th>
            <th class="text-center">Tên khuyến mãi</th>
            <th class="text-center">Mã</th>
            <th class="text-center">Giá trị</th>
            <th class="text-center">Thời gian</th>
            <th class="text-center">Trạng thái</th>
            <th class="text-center" style="width: 20%">Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!hasData && !loading">
            <td colspan="8" class="text-center">Không có khuyến mãi nào.</td>
          </tr>
          <tr v-if="loading">
            <td colspan="8" class="text-center py-5">
              <div class="spinner-border text-primary mb-3" role="status">
                <span class="visually-hidden">Đang tải...</span>
              </div>
              <p class="text-muted">Đang tải dữ liệu...</p>
            </td>
          </tr>
          <tr v-for="(promo, index) in promotions" :key="promo.id">
            <td class="text-center">
              {{ (currentPage - 1) * pageSize + index + 1 }}
            </td>
            <td>
              <div class="fw-bold text-dark">
                {{ promo.name || "Chưa có tên" }}
              </div>
              <div v-if="promo.description" class="small text-muted mt-1">
                {{ promo.description }}
              </div>
            </td>
            <td class="text-center">
              <span class="badge bg-primary">
                {{ promo.code || "N/A" }}
              </span>
            </td>
            <td class="text-center">
              <span class="fw-bold text-primary fs-6">
                {{ promo.value || 0 }}%
              </span>
            </td>
            <td class="text-center">
              <div class="small">
                <div class="mb-1">
                  <i class="fas fa-play-circle text-success me-1"></i>
                  {{ formatDate(promo.start_date) || "Chưa đặt" }}
                </div>
                <div>
                  <i class="fas fa-stop-circle text-danger me-1"></i>
                  {{ formatDate(promo.expiration_date) || "Không hạn" }}
                </div>
              </div>
            </td>
            <td class="text-center">
              <span
                :class="getStatusInfo(promo).badgeClass"
                style="font-size: 14px; padding: 5px 10px; border-radius: 20px"
              >
                {{ getStatusInfo(promo).text }}
              </span>
            </td>
            <td class="text-center vertical-mid">
              <button
                class="btn btn-sm btn-warning me-2"
                @click="viewPromotion(promo.id)"
                title="Chi tiết"
              >
                Chi tiết
              </button>
              <button
                v-if="promo.status !== PROMOTION_STATUS.EXPIRED"
                class="btn btn-sm btn-primary me-2"
                @click="editPromotion(promo.id)"
                title="Chỉnh sửa"
              >
                Sửa
              </button>
              <button
                v-if="getStatusInfo(promo).canDelete"
                class="btn btn-sm btn-danger"
                @click="showDeleteConfirm(promo.id, promo.name)"
                title="Xóa khuyến mãi"
              >
                Xóa
              </button>
              <button
                v-if="getStatusInfo(promo).canRestore"
                class="btn btn-sm btn-success"
                @click="showRestoreConfirm(promo.id, promo.name)"
                title="Khôi phục khuyến mãi"
              >
                Khôi phục
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!--Pagination -->
    <div v-if="promotions.length && !loading" class="mt-4">
      <div class="pagination-wrapper">
        <div class="page-size-selector">
          <span class="me-2 text-muted">Hiển thị</span>
          <select
            class="form-select form-select-sm me-2 custom-select"
            v-model="pageSize"
          >
            <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="size">
              {{ size }}
            </option>
          </select>
          <span class="text-muted">khuyến mãi</span>
        </div>
        <div class="pagination-controls">
          <nav aria-label="Page navigation">
            <ul class="pagination pagination-sm mb-0 custom-pagination">
              <li class="page-item" :class="{ disabled: currentPage === 1 }">
                <button
                  class="page-link custom-page-link"
                  @click="goToPage(currentPage - 1)"
                  :disabled="currentPage === 1"
                >
                  «
                </button>
              </li>
              <li
                v-for="page in visiblePages"
                :key="page"
                class="page-item"
                :class="{
                  active: page === currentPage,
                  disabled: page === '...',
                }"
              >
                <button
                  v-if="page !== '...'"
                  class="page-link custom-page-link"
                  :class="{ 'active-page': page === currentPage }"
                  @click="goToPage(page)"
                >
                  {{ page }}
                </button>
                <span v-else class="page-link custom-page-link disabled"
                  >...</span
                >
              </li>
              <li
                class="page-item"
                :class="{ disabled: currentPage === totalPages }"
              >
                <button
                  class="page-link custom-page-link"
                  @click="goToPage(currentPage + 1)"
                  :disabled="currentPage === totalPages"
                >
                  »
                </button>
              </li>
            </ul>
          </nav>
        </div>
        <div class="pagination-info">
          <span class="text-muted">
            Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong
            tổng số {{ paginationInfo.total }}
            khuyến mãi
          </span>
        </div>
      </div>
    </div>
  </div>

  <!-- Toast Component -->
  <ShowToastComponent ref="toastRef" />

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
</template>

<style scoped>
.transition-transform {
  transition: transform 0.2s ease;
}

.rotate-180 {
  transform: rotate(180deg);
}

/* Pagination wrapper styles */
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

/* Page size selector styles */
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

/* Pagination controls - center */
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

/* Pagination info styles */
.pagination-info {
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}

/* Responsive design */
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

/* Animation for smooth transitions */
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

.spinner-border {
  width: 3rem;
  height: 3rem;
}

.alert {
  border: none;
  border-left: 4px solid;
}

.alert-danger {
  border-left-color: #dc3545;
  background-color: #f8d7da;
  color: #721c24;
}
</style>
