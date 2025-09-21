<script setup>
import { ref, onMounted, computed } from 'vue';
import {getAllBrands, createBrands, updateBrands, deleteBrands, restoreBrand} from "@/service/BrandApi.js";
import ShowToastComponent from "@/components/ShowToastComponent.vue";

const toastRef = ref(null);

const brands = ref([]);
const showModal = ref(false);
const isEditing = ref(false);
const searchKeyword = ref('');
const formErrors = ref({ name: '', description: '', country: '', status: '' });
const currentBrand = ref({
  id: '', name: '', description: '', country: '', status: ''
});

// Modal confirm states
const showConfirmModal = ref(false);
const modalLoading = ref(false);
const pendingAction = ref(null);

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type);
};

// modal
const openAddModal = () => {
  isEditing.value = false;
  currentBrand.value = { name: '', description: '', country: '' };
  showModal.value = true;
};

const openEditModal = (brand) => {
  isEditing.value = true;
  currentBrand.value = { ...brand };
  showModal.value = true;
};

const resetModal = () => {
  currentBrand.value = { id: '', name: '', description: '', country: '', status: '' }
  formErrors.value = { name: '', description: '', country: '', status: '' }
}

// Kiểm tra tên thương hiệu có bị trùng không
const checkDuplicateName = (name, excludeId = null) => {
  if (!name || !name.trim()) return false;

  const trimmedName = name.trim().toLowerCase();
  return brands.value.some(brand =>
      brand.name.toLowerCase().trim() === trimmedName &&
      brand.id !== excludeId
  );
};

// Real-time validation for brand name
const validateBrandName = () => {
  if (!currentBrand.value.name || !currentBrand.value.name.trim()) {
    formErrors.value.name = '';
    return;
  }

  if (currentBrand.value.name.length > 100) {
    formErrors.value.name = 'Tên thương hiệu không được vượt quá 100 ký tự';
    return;
  }

  const specialCharRegex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/;
  if (specialCharRegex.test(currentBrand.value.name)) {
    formErrors.value.name = 'Tên thương hiệu không được chứa ký tự đặc biệt';
    return;
  }

  // Kiểm tra trùng tên
  const excludeId = isEditing.value ? currentBrand.value.id : null;
  if (checkDuplicateName(currentBrand.value.name, excludeId)) {
    formErrors.value.name = 'Tên thương hiệu này đã tồn tại trong hệ thống';
    return;
  }

  formErrors.value.name = '';
};

// Real-time validation for description
const validateDescription = () => {
  if (currentBrand.value.description && currentBrand.value.description.length > 255) {
    formErrors.value.description = 'Mô tả không được vượt quá 255 ký tự';
  } else {
    formErrors.value.description = '';
  }
};

// Real-time validation for country
const validateCountry = () => {
  if (currentBrand.value.country && currentBrand.value.country.length > 100) {
    formErrors.value.country = 'Xuất xứ không được vượt quá 100 ký tự';
  } else {
    formErrors.value.country = '';
  }
};

// Character count computed properties
const nameCharCount = computed(() => currentBrand.value.name ? currentBrand.value.name.length : 0);
const descriptionCharCount = computed(() => currentBrand.value.description ? currentBrand.value.description.length : 0);
const countryCharCount = computed(() => currentBrand.value.country ? currentBrand.value.country.length : 0);

const fetchBrands = async () => {
  try {
    const res = await getAllBrands();
    brands.value = res.data.data;
  } catch (error) {
    console.log('Error when loading data brands: ', error)
    showToast('Có lỗi xảy ra khi tải dữ liệu thương hiệu', 'error');
  }
}

onMounted(fetchBrands);

const validateForm = () => {
  let valid = true;
  formErrors.value = {name: '', description: '', country: ''};

  // Validate brand name
  if (!currentBrand.value.name || !currentBrand.value.name.trim()) {
    formErrors.value.name = 'Tên thương hiệu không được để trống';
    valid = false;
  } else if (currentBrand.value.name.length > 100) {
    formErrors.value.name = 'Tên thương hiệu không được vượt quá 100 ký tự';
    valid = false;
  } else {
    // Check for special characters - only allow letters, numbers, spaces, and Vietnamese characters
    const specialCharRegex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/;
    if (specialCharRegex.test(currentBrand.value.name)) {
      formErrors.value.name = 'Tên thương hiệu không được chứa ký tự đặc biệt';
      valid = false;
    } else {
      // Kiểm tra trùng tên - loại trừ ID hiện tại nếu đang sửa
      const excludeId = isEditing.value ? currentBrand.value.id : null;
      if (checkDuplicateName(currentBrand.value.name, excludeId)) {
        formErrors.value.name = 'Tên thương hiệu này đã tồn tại trong hệ thống';
        valid = false;
      }
    }
  }

  // Validate description
  if (!currentBrand.value.description || !currentBrand.value.description.trim()) {
    formErrors.value.description = 'Mô tả của thương hiệu không được để trống';
    valid = false;
  } else if (currentBrand.value.description.length > 255) {
    formErrors.value.description = 'Mô tả không được vượt quá 255 ký tự';
    valid = false;
  }

  // Validate country
  if (!currentBrand.value.country || !currentBrand.value.country.trim()) {
    formErrors.value.country = 'Xuất xứ của thương hiệu không được để trống';
    valid = false;
  } else if (currentBrand.value.country.length > 100) {
    formErrors.value.country = 'Xuất xứ không được vượt quá 100 ký tự';
    valid = false;
  }

  return valid;
}

const saveBrand = async () => {
  if (!validateForm()) {
    return;
  }

  // Reset form errors before making API call
  formErrors.value = {name: '', description: '', country: '', status: ''};

  try {
    if (isEditing.value) {
      await updateBrands(currentBrand.value.id, currentBrand.value);
      showToast('Cập nhật thương hiệu thành công!', 'success');
    } else {
      await createBrands(currentBrand.value);
      showToast('Thêm thương hiệu thành công!', 'success');
    }
    showModal.value = false;
    resetModal();
    await fetchBrands();
  } catch (error) {
    console.log('Error saving brand: ', error);

    // Handle different types of errors
    if (error.response) {
      // Server responded with error status
      const status = error.response.status;
      const errorData = error.response.data;

      // Check if it's our custom API response format
      if (errorData && errorData.message) {
        const errorMessage = errorData.message;

        // Check for duplicate brand error
        if (errorMessage.includes('already exists!') || status === 409) {
          formErrors.value.name = 'Thương hiệu này đã tồn tại trong hệ thống';
          showToast('Thương hiệu đã tồn tại!', 'error');
        } else {
          // Other server errors
          showToast(errorMessage, 'error');
        }
      } else if (errorData && errorData.data && errorData.data.message) {
        // Alternative error format
        const errorMessage = errorData.data.message;
        if (errorMessage.includes('already exists!')) {
          formErrors.value.name = 'Thương hiệu này đã tồn tại trong hệ thống';
        } else {
          showToast(errorMessage, 'error');
        }
      } else {
        // Generic server error
        showToast('Có lỗi xảy ra từ máy chủ', 'error');
      }
    }
  }
}

// Modal confirm handlers
const showDeleteConfirm = (brandId, brandName) => {
  pendingAction.value = {
    type: 'delete',   // thêm dòng này
    id: brandId,
    name: brandName
  };
  showConfirmModal.value = true;
};

const showRestoreConfirm = (brandId, brandName) => {
  pendingAction.value = {
    type: 'restore',
    id: brandId,
    name: brandName
  };
  showConfirmModal.value = true;
};

const handleConfirmDelete = async () => {
  if (!pendingAction.value) return;

  modalLoading.value = true;

  try {
    if (pendingAction.value.type === 'delete') {
      await deleteBrands(pendingAction.value.id);
      showToast('Xóa thương hiệu thành công!', 'success');
    } else if (pendingAction.value.type === 'restore') {
      await restoreBrand(pendingAction.value.id);
      showToast('Khôi phục thương hiệu thành công!', 'success');
    }

    await fetchBrands();
    showConfirmModal.value = false;
    pendingAction.value = null;
  } catch (error) {
    console.log('Error deleting/restoring brand: ', error);
    showToast('Có lỗi xảy ra khi xử lý thương hiệu', 'error');
  } finally {
    modalLoading.value = false;
  }
};

const handleCancelDelete = () => {
  showConfirmModal.value = false;
  pendingAction.value = null;
  modalLoading.value = false;
};

// Computed properties for modal content
const modalConfig = computed(() => {
  if (!pendingAction.value) return {};

  const isDelete = pendingAction.value.type === 'delete';

  return {
    title: isDelete ? 'Xác nhận xóa thương hiệu' : 'Xác nhận khôi phục thương hiệu',
    message: isDelete
        ? `Bạn có chắc chắn muốn xóa thương hiệu "${pendingAction.value.name}"?`
        : `Bạn có chắc chắn muốn khôi phục thương hiệu "${pendingAction.value.name}"?`,
    details: isDelete
        ? 'Thương hiệu sẽ được chuyển sang trạng thái "Ngừng kinh doanh" và có thể được khôi phục sau.'
        : 'Thương hiệu sẽ được chuyển sang trạng thái "Đang kinh doanh" và hiển thị trở lại.',
    type: isDelete ? 'danger' : 'success',
    confirmText: isDelete ? 'Xóa sản phẩm' : 'Khôi phục',
    cancelText: 'Hủy bỏ'
  };
});

const filteredBrands = computed(() => {
  if (!searchKeyword.value) return brands.value;
  const keyword = searchKeyword.value.toLowerCase().trim();
  return brands.value.filter(b =>
      b.name.toLowerCase().includes(keyword) ||
      (b.description && b.description.toLowerCase().includes(keyword)) ||
      (b.country && b.country.toLowerCase().includes(keyword))
  );
});
</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">🏷️ Quản lý thương hiệu</h2>
    <div class="row mb-3">
      <div class="col-8">
        <form class="mb-3">
          <input type="text" class="form-control" placeholder="🔍 Tìm theo tên thương hiệu, mô tả, xuất xứ..."
                 v-model="searchKeyword"/>
        </form>
      </div>
      <div class="col-4">
        <button class="btn btn-primary" @click="openAddModal()">
          <i class="fas fa-plus me-2"></i> Thêm thương hiệu
        </button>
      </div>
    </div>
    <div class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
        <tr>
          <th style="width: 5%">STT</th>
          <th>Tên thương hiệu</th>
          <th>Mô tả</th>
          <th>Xuất xứ</th>
          <th>Trạng thái</th>
          <th style="width: 20%">Hành động</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(brand, index) in filteredBrands" :key="brand.id">
          <td>{{ index + 1 }}</td>
          <td>{{ brand.name }}</td>
          <td>{{ brand.description }}</td>
          <td>{{ brand.country }}</td>
          <td>
            <span :class="brand.status ? 'badge bg-success' : 'badge bg-secondary'"
                  style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
              {{ brand.status === true ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
            </span>
          </td>
          <td>
            <button class="btn btn-sm btn-warning me-2" @click="openEditModal(brand)">
              Sửa
            </button>
            <button
                v-if="brand.status === true"
                class="btn btn-sm btn-danger"
                @click="showDeleteConfirm(brand.id, brand.name)"
            >
              Xóa
            </button>
            <button
                v-else
                class="btn btn-sm btn-success"
                @click="showRestoreConfirm(brand.id, brand.name)"
            >
              Khôi phục
            </button>
          </td>
        </tr>
        <tr v-if="filteredBrands.length === 0">
          <td colspan="6" class="text-center">
            {{ brands.length === 0 ? 'Không có thương hiệu nào.' : 'Không tìm thấy thương hiệu phù hợp.' }}
          </td>
        </tr>
        </tbody>
      </table>

      <!-- Add/Edit Modal -->
      <div v-if="showModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">{{ isEditing ? 'Cập nhật thương hiệu' : 'Thêm thương hiệu' }}</h5>
              <button type="button" class="btn-close" @click="showModal = false; resetModal()"></button>
            </div>
            <div class="modal-body">
              <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center">
                  <label class="form-label">Tên thương hiệu <span class="text-danger">*</span></label>
                  <small class="text-muted">
                    {{ nameCharCount }}/100
                  </small>
                </div>
                <input
                    type="text"
                    class="form-control"
                    :class="{'is-invalid': formErrors.name}"
                    v-model="currentBrand.name"
                    @input="validateBrandName"
                    maxlength="100"
                    placeholder="Nhập tên thương hiệu"
                />
                <div class="invalid-feedback" v-if="formErrors.name">
                  {{ formErrors.name }}
                </div>
              </div>
              <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center">
                  <label class="form-label">Mô tả <span class="text-danger">*</span></label>
                  <small class="text-muted">
                    {{ descriptionCharCount }}/255
                  </small>
                </div>
                <textarea
                    class="form-control"
                    :class="{'is-invalid': formErrors.description}"
                    v-model="currentBrand.description"
                    @input="validateDescription"
                    maxlength="255"
                    placeholder="Nhập mô tả thương hiệu"
                    rows="3"
                ></textarea>
                <div class="invalid-feedback" v-if="formErrors.description">
                  {{ formErrors.description }}
                </div>
              </div>
              <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center">
                  <label class="form-label">Xuất xứ <span class="text-danger">*</span></label>
                  <small class="text-muted">
                    {{ countryCharCount }}/100
                  </small>
                </div>
                <input
                    type="text"
                    class="form-control"
                    :class="{'is-invalid': formErrors.country}"
                    v-model="currentBrand.country"
                    @input="validateCountry"
                    maxlength="100"
                    placeholder="Nhập xuất xứ thương hiệu"
                />
                <div class="invalid-feedback" v-if="formErrors.country">
                  {{ formErrors.country }}
                </div>
              </div>
              <div class="mb-3" v-if="isEditing">
                <label class="form-label">Trạng thái</label>
                <select class="form-select" v-model="currentBrand.status">
                  <option :value="true">Đang kinh doanh</option>
                  <option :value="false">Ngừng kinh doanh</option>
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showModal = false; resetModal()">Hủy</button>
              <button class="btn btn-primary" @click="saveBrand">
                {{ isEditing ? 'Cập nhật' : 'Thêm mới' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Modal Confirm Delete Component -->
  <div v-if="showConfirmModal" class="modal-overlay" @click="handleCancelDelete">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h5 class="modal-title">
          <i class="fas fa-exclamation-triangle text-danger me-2"></i>
          {{ modalConfig.title }}
        </h5>
        <button type="button" class="btn-close" @click="handleCancelDelete" :disabled="modalLoading"></button>
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
            @click="handleCancelDelete"
            :disabled="modalLoading"
        >
          {{ modalConfig.cancelText }}
        </button>
        <button
            type="button"
            class="btn btn-danger"
            @click="handleConfirmDelete"
            :disabled="modalLoading"
        >
          <span v-if="modalLoading" class="spinner-border spinner-border-sm me-2" role="status"></span>
          {{ modalConfig.confirmText }}
        </button>
      </div>
    </div>
  </div>

  <ShowToastComponent ref="toastRef"/>
</template>

<style scoped>
table th,
table td {
  vertical-align: middle;
}

.modal {
  display: block;
}

.form-label .text-danger {
  font-size: 0.875rem;
}

.invalid-feedback {
  display: block;
}

/* Character count styling */
.text-muted small {
  font-size: 0.8rem;
  font-weight: 500;
}

/* Input field styling when approaching limit */
.form-control:focus {
  border-color: #80bdff;
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

/* Modal Confirm Styles */
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
@media (max-width: 576px) {
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

/* Animation keyframes */
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
</style>