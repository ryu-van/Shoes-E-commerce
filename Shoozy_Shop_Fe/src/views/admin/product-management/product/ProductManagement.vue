<script setup>
import { getAllCategories } from "@/service/CategoryApi.js";
import { getAllBrands } from "@/service/BrandApi.js";
import {deleteProduct, getAllProducts, restoreProduct} from "@/service/ProductApi.js";
import { ref, onMounted, watch, computed } from "vue";
import ShowToastComponent from "@/components/ShowToastComponent.vue";

const toastRef = ref(null);
const confirmModalRef = ref(null);

// Các method để hiển thị toast với các loại khác nhau
const showToast = (message, type) => {
  toastRef.value?.showToast(message, type);
};

const products = ref([]);
const categories = ref([]);
const brands = ref([]);

const filterName = ref("");
const filterCategory = ref("");
const filterBrand = ref("");
const filterStatus = ref("");

// Validation states
const keywordError = ref("");
const MAX_KEYWORD_LENGTH = 255;

const pageNo = ref(1);
const pageSize = ref(5);
const totalPages = ref(1);
const totalElements = ref(0);

// Modal confirm states
const showConfirmModal = ref(false);
const modalLoading = ref(false);
const pendingAction = ref(null);

// Các tùy chọn cho pageSize
const pageSizeOptions = [5, 10, 15, 20, 25];

// Computed để kiểm tra keyword có hợp lệ không
const isKeywordValid = computed(() => {
  return filterName.value.length <= MAX_KEYWORD_LENGTH;
});

// Computed để hiển thị số ký tự còn lại
const remainingChars = computed(() => {
  return MAX_KEYWORD_LENGTH - filterName.value.length;
});

// Computed để tính toán thông tin hiển thị
const paginationInfo = computed(() => {
  const start = (pageNo.value - 1) * pageSize.value + 1;
  const end = Math.min(pageNo.value * pageSize.value, totalElements.value);
  return {
    start,
    end,
    total: totalElements.value
  };
});

// Computed để tạo danh sách các trang hiển thị
const visiblePages = computed(() => {
  const current = pageNo.value;
  const total = totalPages.value;
  const pages = [];

  // Luôn hiển thị trang đầu
  if (current > 3) {
    pages.push(1);
    if (current > 4) {
      pages.push('...');
    }
  }

  // Hiển thị các trang xung quanh trang hiện tại
  const start = Math.max(1, current - 2);
  const end = Math.min(total, current + 2);

  for (let i = start; i <= end; i++) {
    if (!pages.includes(i)) {
      pages.push(i);
    }
  }

  // Luôn hiển thị trang cuối
  if (current < total - 2) {
    if (current < total - 3) {
      pages.push('...');
    }
    if (!pages.includes(total)) {
      pages.push(total);
    }
  }

  return pages;
});

// Validation cho keyword
const validateKeyword = () => {
  if (filterName.value.length > MAX_KEYWORD_LENGTH) {
    keywordError.value = `Từ khóa tìm kiếm không được vượt quá ${MAX_KEYWORD_LENGTH} ký tự`;
    showToast(`Từ khóa tìm kiếm quá dài! Tối đa ${MAX_KEYWORD_LENGTH} ký tự`, "error");
    return false;
  }
  keywordError.value = "";
  return true;
};

// Xử lý input keyword với validation
const handleKeywordInput = (event) => {
  const value = event.target.value;

  if (value.length > MAX_KEYWORD_LENGTH) {
    // Cắt chuỗi về đúng độ dài cho phép
    filterName.value = value.substring(0, MAX_KEYWORD_LENGTH);
    showToast(`Từ khóa đã được cắt xuống ${MAX_KEYWORD_LENGTH} ký tự`, "warning");
  } else {
    filterName.value = value;
  }

  validateKeyword();
};

const fetchDataProducts = async () => {
  // Kiểm tra validation trước khi gọi API
  if (!isKeywordValid.value) {
    showToast("Vui lòng nhập từ khóa hợp lệ để tìm kiếm", "error");
    return;
  }

  try {
    const res = await getAllProducts({
      keyword: filterName.value.trim() || null,
      category_id: filterCategory.value || null,
      brand_id: filterBrand.value || null,
      status: filterStatus.value !== "" ? filterStatus.value : null,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      sortBy: 'id',
      sortDirection: 'desc'
    });
    console.log("Products fetched:", res.data.data);
    products.value = res.data.data.content;
    totalPages.value = res.data.data.totalPages;
    totalElements.value = res.data.data.totalElements;
  } catch (error) {
    console.log("Error loading products:", error);
    showToast("Có lỗi xảy ra khi tải danh sách sản phẩm", "error");
  }
};

const fetchDataFilters = async () => {
  try {
    const [catRes, brandRes] = await Promise.all([
      getAllCategories(),
      getAllBrands(),
    ]);
    categories.value = catRes.data.data;
    brands.value = brandRes.data.data;
  } catch (error) {
    console.log("Error loading filters:", error);
  }
};

const clearFilter = () => {
  filterName.value = "";
  filterCategory.value = "";
  filterBrand.value = "";
  filterStatus.value = "";
  keywordError.value = "";
  pageNo.value = 1;
  fetchDataProducts();
};

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value && page !== '...') {
    pageNo.value = page;
    fetchDataProducts();
  }
};

// Watch cho pageSize để reset về trang 1 khi thay đổi
watch(pageSize, () => {
  pageNo.value = 1;
  fetchDataProducts();
});

// Modal confirm handlers
const showDeleteConfirm = (productId, productName) => {
  pendingAction.value = {
    type: 'delete',
    id: productId,
    name: productName
  };
  showConfirmModal.value = true;
};

const showRestoreConfirm = (productId, productName) => {
  pendingAction.value = {
    type: 'restore',
    id: productId,
    name: productName
  };
  showConfirmModal.value = true;
};

const handleConfirmAction = async () => {
  if (!pendingAction.value) return;

  modalLoading.value = true;

  try {
    if (pendingAction.value.type === 'delete') {
      await deleteProduct(pendingAction.value.id);
      showToast("Xóa sản phẩm thành công!", "success");
    } else if (pendingAction.value.type === 'restore') {
      await restoreProduct(pendingAction.value.id);
      showToast("Khôi phục sản phẩm thành công!", "success");
    }

    await fetchDataProducts();
    showConfirmModal.value = false;
    pendingAction.value = null;
  } catch (error) {
    console.error(`Lỗi khi ${pendingAction.value.type === 'delete' ? 'xóa' : 'khôi phục'} sản phẩm:`, error);
    showToast(`Có lỗi xảy ra khi ${pendingAction.value.type === 'delete' ? 'xóa' : 'khôi phục'} sản phẩm`, "error");
  } finally {
    modalLoading.value = false;
  }
};

const handleCancelAction = () => {
  showConfirmModal.value = false;
  pendingAction.value = null;
  modalLoading.value = false;
};

// Computed properties for modal content
const modalConfig = computed(() => {
  if (!pendingAction.value) return {};

  const isDelete = pendingAction.value.type === 'delete';

  return {
    title: isDelete ? 'Xác nhận xóa sản phẩm' : 'Xác nhận khôi phục sản phẩm',
    message: isDelete
        ? `Bạn có chắc chắn muốn xóa sản phẩm "${pendingAction.value.name}"?`
        : `Bạn có chắc chắn muốn khôi phục sản phẩm "${pendingAction.value.name}"?`,
    details: isDelete
        ? 'Sản phẩm sẽ được chuyển sang trạng thái "Ngừng kinh doanh" và có thể được khôi phục sau.'
        : 'Sản phẩm sẽ được chuyển sang trạng thái "Đang kinh doanh" và hiển thị trở lại.',
    type: isDelete ? 'danger' : 'success',
    confirmText: isDelete ? 'Xóa sản phẩm' : 'Khôi phục',
    cancelText: 'Hủy bỏ'
  };
});

// Load data khi component mount
onMounted(async () => {
  try {
    await fetchDataFilters();
    await fetchDataProducts();
  } catch (error) {
    showToast("Lỗi khi khởi tạo trang!", "error");
  }
});

// Watch filters và gọi lại API khi filter thay đổi (với debounce cho keyword)
let keywordTimeout = null;

watch(filterName, () => {
  // Debounce cho keyword search
  clearTimeout(keywordTimeout);
  keywordTimeout = setTimeout(() => {
    if (isKeywordValid.value) {
      pageNo.value = 1;
      fetchDataProducts();
    }
  }, 500); // Delay 500ms
});

watch([filterCategory, filterBrand, filterStatus], () => {
  pageNo.value = 1;
  fetchDataProducts();
});
</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">🛒 Quản lý sản phẩm</h2>
    <div class="row gy-2 gx-3 align-items-center mb-3 flex-wrap">
      <div class="col-md-3">
        <div class="keyword-input-wrapper">
          <input
              type="text"
              class="form-control"
              :class="{ 'is-invalid': !isKeywordValid }"
              placeholder="🔍 Tìm theo tên sản phẩm, mã sản phẩm..."
              :value="filterName"
              @input="handleKeywordInput"
              :maxlength="MAX_KEYWORD_LENGTH"
          >
          <div class="keyword-counter" :class="{ 'text-danger': remainingChars < 20 }">
            {{ filterName.length }}/{{ MAX_KEYWORD_LENGTH }}
          </div>
          <div v-if="keywordError" class="invalid-feedback">
            {{ keywordError }}
          </div>
        </div>
      </div>
      <div class="col-md-2">
        <select class="form-select" v-model="filterCategory">
          <option value="">Danh mục</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>
      </div>
      <div class="col-md-2">
        <select class="form-select" v-model="filterBrand">
          <option value="">Thương hiệu</option>
          <option v-for="brand in brands" :key="brand.id" :value="brand.id">
            {{ brand.name }}
          </option>
        </select>
      </div>
      <div class="col-md-2">
        <select class="form-select" v-model="filterStatus">
          <option value="">Trạng thái</option>
          <option :value="true">Đang kinh doanh</option>
          <option :value="false">Ngừng kinh doanh</option>
        </select>
      </div>
      <div class="col-md-3 col-12 d-flex gap-3 flex-wrap">
        <button class="btn btn-secondary flex-fill" style="height: 38px" @click="clearFilter">Clear</button>
        <router-link to="/admin/products/new" class="btn btn-primary flex-fill" style="height: 38px">
          Thêm sản phẩm
        </router-link>
      </div>
    </div>

    <div style="margin-top: 32px" class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
        <tr>
          <th class="text-center" style="width: 5%">STT</th>
          <th class="text-center">Mã SP</th>
          <th class="text-center">Ảnh</th>
          <th class="text-center">Tên sản phẩm</th>
          <th class="text-center">Số lượng</th>
          <th class="text-center">Danh mục</th>
          <th class="text-center">Thương hiệu</th>
          <th class="text-center">Trạng thái</th>
          <th class="text-center" style="width: 20%">Hành động</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(product, index) in products" :key="product.id || index">
          <td class="text-center">{{ (pageNo - 1) * pageSize + index + 1 }}</td>
          <td class="text-center">{{ product.sku }}</td>
          <td class="text-center">
            <img style="object-fit: cover; border-radius: 4px;" width="80px" height="80px" :src="product.thumbnail || 'http://localhost:9000/product-variant-images/10969556-0fcd-43ff-b7ee-3af88423d16d_488px-No-Image-Placeholder.svg_.png'" :alt="product.name"/>
          </td>
          <td>{{ product.name }}</td>
          <td class="text-center">{{ product.totalQuantity }}</td>
          <td>{{ product.category?.name || 'Chưa có danh mục' }}</td>
          <td>{{ product.brand?.name || 'Chưa có thương hiệu' }}</td>
          <td class="text-center">
            <span :class="product.status ? 'badge bg-success' : 'badge bg-secondary'"
                  style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
              {{ product.status === true ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
            </span>
          </td>
          <td class="text-center vertical-mid">
            <router-link :to="`/admin/products/${product.id}`" class="btn btn-sm btn-warning me-2">
              Chi tiết
            </router-link>
            <button
                v-if="product.status === true"
                class="btn btn-sm btn-danger"
                @click="showDeleteConfirm(product.id, product.name)"
            >
              Xóa
            </button>
            <button
                v-else
                class="btn btn-sm btn-success"
                @click="showRestoreConfirm(product.id, product.name)"
            >
              Khôi phục
            </button>
          </td>
        </tr>
        <tr v-if="products.length === 0">
          <td colspan="12" class="text-center">Không có sản phẩm nào.</td>
        </tr>
        </tbody>
      </table>
    </div>

    <!--Pagination -->
    <div v-if="products.length" class="mt-4">
      <!-- Pagination wrapper với style đẹp -->
      <div class="pagination-wrapper">
        <!-- Left side: Page size selector -->
        <div class="page-size-selector">
          <span class="me-2 text-muted">Hiển thị</span>
          <select class="form-select form-select-sm me-2 custom-select" v-model="pageSize">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">
              {{ size }}
            </option>
          </select>
          <span class="text-muted">sản phẩm</span>
        </div>

        <!-- Center: Pagination controls -->
        <div class="pagination-controls">
          <nav aria-label="Page navigation">
            <ul class="pagination pagination-sm mb-0 custom-pagination">
              <!-- Previous button -->
              <li class="page-item" :class="{ disabled: pageNo === 1 }">
                <button class="page-link custom-page-link" @click="goToPage(pageNo - 1)" :disabled="pageNo === 1">
                  «
                </button>
              </li>

              <!-- Page numbers -->
              <li v-for="page in visiblePages" :key="page" class="page-item"
                  :class="{active: page === pageNo, disabled: page === '...'}">
                <button
                    v-if="page !== '...'"
                    class="page-link custom-page-link"
                    :class="{ 'active-page': page === pageNo }"
                    @click="goToPage(page)"
                >
                  {{ page }}
                </button>
                <span v-else class="page-link custom-page-link disabled">...</span>
              </li>

              <!-- Next button -->
              <li class="page-item" :class="{ disabled: pageNo === totalPages }">
                <button class="page-link custom-page-link" @click="goToPage(pageNo + 1)"
                        :disabled="pageNo === totalPages">
                  »
                </button>
              </li>
            </ul>
          </nav>
        </div>

        <!-- Right side: Pagination info -->
        <div class="pagination-info">
        <span class="text-muted">
          Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong tổng số {{ paginationInfo.total }} sản phẩm
        </span>
        </div>
      </div>
    </div>
  </div>

  <!-- Modal Confirm Component -->
  <div v-if="showConfirmModal" class="modal-overlay" @click="handleCancelAction">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h5 class="modal-title">
          <i :class="modalConfig.type === 'danger' ? 'fas fa-exclamation-triangle text-danger' : 'fas fa-check-circle text-success'" class="me-2"></i>
          {{ modalConfig.title }}
        </h5>
        <button type="button" class="btn-close" @click="handleCancelAction" :disabled="modalLoading"></button>
      </div>

      <div class="modal-body">
        <p class="modal-message">{{ modalConfig.message }}</p>
        <div v-if="modalConfig.details" class="modal-details">
          {{ modalConfig.details }}
        </div>
      </div>

      <div class="modal-footer">
        <button
            type="button"
            class="btn btn-secondary"
            @click="handleCancelAction"
            :disabled="modalLoading"
        >
          {{ modalConfig.cancelText }}
        </button>
        <button
            type="button"
            class="btn"
            :class="modalConfig.type === 'danger' ? 'btn-danger' : 'btn-success'"
            @click="handleConfirmAction"
            :disabled="modalLoading"
        >
          <span v-if="modalLoading" class="spinner-border spinner-border-sm me-2" role="status"></span>
          {{ modalConfig.confirmText }}
        </button>
      </div>
    </div>
  </div>

  <!-- Toast Component -->
  <ShowToastComponent ref="toastRef"/>
</template>

<style scoped>
/* Keyword input wrapper styles */
.keyword-input-wrapper {
  position: relative;
}

.keyword-counter {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 11px;
  color: #6c757d;
  background: rgba(255, 255, 255, 0.9);
  padding: 2px 6px;
  border-radius: 4px;
  pointer-events: none;
  z-index: 5;
}

.keyword-counter.text-danger {
  color: #dc3545 !important;
  font-weight: 600;
}

/* Adjust input padding to make room for counter */
.keyword-input-wrapper input {
  padding-right: 60px;
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

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}

.modal-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  animation: slideIn 0.3s ease-out;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e9ecef;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
}

.modal-title {
  margin: 0;
  font-weight: 600;
  color: #212529;
  font-size: 1.1rem;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #6c757d;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #e9ecef;
  color: #495057;
}

.modal-body {
  padding: 24px;
}

.modal-message {
  margin: 0 0 12px 0;
  color: #495057;
  line-height: 1.5;
  font-size: 1rem;
}

.modal-details {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 12px;
  color: #6c757d;
  font-size: 0.9rem;
  line-height: 1.4;
}

.modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 20px 24px;
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
}

.modal-footer .btn {
  min-width: 100px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.2s;
}

.modal-footer .btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
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

  .keyword-counter {
    font-size: 10px;
    right: 6px;
    padding: 1px 4px;
  }

  .keyword-input-wrapper input {
    padding-right: 50px;
  }

  .modal-container {
    width: 95%;
    margin: 10px;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 16px;
  }

  .modal-footer {
    flex-direction: column;
  }

  .modal-footer .btn {
    width: 100%;
  }
}

/* Animation for smooth transitions */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.pagination-wrapper {
  animation: fadeIn 0.3s ease-out;
}
</style>