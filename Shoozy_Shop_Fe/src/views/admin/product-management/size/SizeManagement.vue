<script setup>
import { ref, onMounted, computed } from 'vue';
import {getAllSizes, createSize, updateSize, deleteSize, restoreSize} from "@/service/SizeApi.js";
import ShowToastComponent from "@/components/ShowToastComponent.vue";

const toastRef = ref(null);

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type);
};

// Modal confirm variables
const showConfirmModal = ref(false);
const modalLoading = ref(false);
const pendingAction = ref(null);

const modalConfig = computed(() => {
  if (!pendingAction.value) return {};

  const isDelete = pendingAction.value.type === 'delete';

  return {
    title: isDelete ? 'Xác nhận xóa size' : 'Xác nhận khôi phục size',
    message: isDelete
        ? `Bạn có chắc chắn muốn xóa size "${pendingAction.value.value}"?`
        : `Bạn có chắc chắn muốn khôi phục size "${pendingAction.value.value}"?`,
    details: isDelete
        ? 'Size sẽ được chuyển sang trạng thái "Ngừng kinh doanh" và có thể được khôi phục sau.'
        : 'Size sẽ được chuyển sang trạng thái "Đang kinh doanh" và hiển thị trở lại.',
    type: isDelete ? 'danger' : 'success',
    confirmText: isDelete ? 'Xóa size' : 'Khôi phục',
    cancelText: 'Hủy bỏ'
  };
});

const sizes = ref([]);
const showModal = ref(false);
const isEditing = ref(false);
const searchKeyword = ref('');
const formErrors = ref({ value: '', status: '' });
const currentSize = ref({
  id: '', value: '', status: ''
});

// modal
const openAddModal = () => {
  isEditing.value = false;
  currentSize.value = { value: '', status: true };
  formErrors.value = { value: '', status: '' };
  showModal.value = true;
};

const openEditModal = (size) => {
  isEditing.value = true;
  currentSize.value = { ...size };
  formErrors.value = { value: '', status: '' };
  showModal.value = true;
};

const resetModal = () => {
  currentSize.value = { id: '', value: '', status: true };
  formErrors.value = { value: '', status: '' };
};

// fetch data
const fetchSizes = async () => {
  try {
    const res = await getAllSizes();
    sizes.value = res.data.data;
  } catch (error) {
    console.log('Error when loading data sizes: ', error);
    showToast('Có lỗi xảy ra khi tải dữ liệu size', 'error');
  }
};

onMounted(fetchSizes);

// Modal confirm functions
const handleConfirmAction = async () => {
  if (!pendingAction.value) return;

  modalLoading.value = true;

  try {
    if (pendingAction.value.type === 'delete') {
      await deleteSize(pendingAction.value.id);
      showToast('Xóa size thành công!', 'success');
    } else if (pendingAction.value.type === 'restore') {
      await restoreSize(pendingAction.value.id);
      showToast('Khôi phục size thành công!', 'success');
    }

    await fetchSizes();
    showConfirmModal.value = false;
    pendingAction.value = null;
  } catch (error) {
    console.log('Error delete/restore size: ', error);
    showToast('Có lỗi xảy ra khi xử lý size', 'error');
  } finally {
    modalLoading.value = false;
  }
};

const handleCancelDelete = () => {
  showConfirmModal.value = false;
  pendingAction.value = null;
  modalLoading.value = false;
};

const checkDuplicateValue = (value, excludeId = null) => {
  if (!value && value !== 0) return false;

  const numericValue = parseFloat(value);
  return sizes.value.some(size => {
    const sizeValue = parseFloat(size.value);
    return sizeValue === numericValue && size.id !== excludeId;
  });
};

const validateForm = () => {
  let valid = true;
  formErrors.value = { value: '', status: '' };

  // Validate size value
  if (!currentSize.value.value && currentSize.value.value !== 0) {
    formErrors.value.value = 'Giá trị của size không được để trống';
    valid = false;
  } else {
    const numericValue = parseInt(currentSize.value.value);

    if (isNaN(numericValue)) {
      formErrors.value.value = 'Giá trị size phải là số nguyên';
      valid = false;
    } else if (numericValue < 0) {
      formErrors.value.value = 'Giá trị của size phải lớn hơn hoặc bằng 0';
      valid = false;
    } else if (numericValue > 50) {
      formErrors.value.value = 'Giá trị của size không được lớn hơn 50';
      valid = false;
    } else if (!Number.isInteger(parseFloat(currentSize.value.value))) {
      formErrors.value.value = 'Giá trị size phải là số nguyên (ví dụ: 37, 38, 39)';
      valid = false;
    } else {
      // Check for duplicate value
      const excludeId = isEditing.value ? currentSize.value.id : null;
      if (checkDuplicateValue(currentSize.value.value, excludeId)) {
        formErrors.value.value = 'Size này đã tồn tại trong hệ thống';
        valid = false;
      }
    }
  }

  return valid;
};

// Real-time validation for size value
const validateSizeValue = () => {
  if (!currentSize.value.value && currentSize.value.value !== 0) {
    formErrors.value.value = '';
    return;
  }

  const numericValue = parseFloat(currentSize.value.value);

  if (isNaN(numericValue)) {
    formErrors.value.value = 'Giá trị size phải là số';
    return;
  }

  if (numericValue < 0) {
    formErrors.value.value = 'Giá trị của size phải lớn hơn hoặc bằng 0';
    return;
  }

  if (numericValue > 50) {
    formErrors.value.value = 'Giá trị của size không được lớn hơn 50';
    return;
  }

  formErrors.value.value = '';

  // Check for duplicate value
  const excludeId = isEditing.value ? currentSize.value.id : null;
  if (checkDuplicateValue(currentSize.value.value, excludeId)) {
    formErrors.value.value = 'Size này đã tồn tại trong hệ thống';
    return;
  }
};

const saveSize = async () => {
  if (!validateForm()) {
    return;
  }

  // Reset form errors before making API call
  formErrors.value = { value: '', status: '' };

  try {
    if (isEditing.value) {
      await updateSize(currentSize.value.id, currentSize.value);
      showToast('Cập nhật size thành công!', 'success');
    } else {
      await createSize(currentSize.value);
      showToast('Thêm size thành công!', 'success');
    }
    showModal.value = false;
    resetModal();
    await fetchSizes();
  } catch (error) {
    console.error('Error saving size:', error);

    // Handle different types of errors
    if (error.response) {
      // Server responded with error status
      const status = error.response.status;
      const errorData = error.response.data;

      // Check if it's our custom API response format
      if (errorData && errorData.message) {
        const errorMessage = errorData.message;

        // Check for duplicate size error
        if (errorMessage.includes('already exists!') || status === 409) {
          formErrors.value.value = 'Size này đã tồn tại trong hệ thống';
        } else {
          // Other server errors
          showToast(errorMessage, 'error');
        }
      } else if (errorData && errorData.data && errorData.data.message) {
        // Alternative error format
        const errorMessage = errorData.data.message;
        if (errorMessage.includes('already exists!')) {
          formErrors.value.value = 'Size này đã tồn tại trong hệ thống';
        } else {
          showToast(errorMessage, 'error');
        }
      } else {
        // Generic server error
        showToast('Có lỗi xảy ra từ máy chủ', 'error');
      }
    } else if (error.request) {
      // Network error
      showToast('Không thể kết nối đến máy chủ', 'error');
    } else {
      // Other errors
      showToast('Có lỗi không xác định xảy ra', 'error');
    }
  }
};

const deleteSizeById = async (id) => {
  // Tìm thông tin size để hiển thị trong modal
  const size = sizes.value.find(s => s.id === id);
  if (!size) {
    showToast('Không tìm thấy thông tin size!', 'error');
    return;
  }
  
  // Hiển thị modal xác nhận xóa
  pendingAction.value = {
    type: 'delete',
    id: size.id,
    value: size.value
  };
  showConfirmModal.value = true;
};

const restoreSizeById = async (id) => {
  // Tìm thông tin size để hiển thị trong modal
  const size = sizes.value.find(s => s.id === id);
  if (!size) {
    showToast('Không tìm thấy thông tin size!', 'error');
    return;
  }
  
  // Hiển thị modal xác nhận khôi phục
  pendingAction.value = {
    type: 'restore',
    id: size.id,
    value: size.value
  };
  showConfirmModal.value = true;
};

const filteredSize = computed(() => {
  if (!searchKeyword.value) return sizes.value;
  const keyword = searchKeyword.value.toLowerCase().trim();
  return sizes.value.filter(s =>
      s.value.toString().toLowerCase().includes(keyword)
  );
});

// Format size value for display
const formatSizeValue = (value) => {
  const num = parseFloat(value);
  return num % 1 === 0 ? num.toString() : num.toFixed(1);
};
</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">📏 Quản lý Size</h2>
    <div class="row mb-3">
      <div class="col-8">
        <input type="text" class="form-control" placeholder="🔍 Tìm theo size..."
               v-model="searchKeyword" />
      </div>
      <div class="col-4">
        <button class="btn btn-primary" @click="openAddModal">
          <i class="fas fa-plus me-2"></i> Thêm size
        </button>
      </div>
    </div>
    <div class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
        <tr>
          <th style="width: 5%">STT</th>
          <th>Size</th>
          <th>Trạng thái</th>
          <th style="width: 20%">Hành động</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(size, index) in filteredSize" :key="size.id">
          <td>{{ index + 1 }}</td>
          <td>{{ formatSizeValue(size.value) }}</td>
          <td>
            <span :class="size.status ? 'badge bg-success' : 'badge bg-secondary'"
                  style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
              {{ size.status === true ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
            </span>
          </td>
          <td>
            <button class="btn btn-sm btn-warning me-2" @click="openEditModal(size)">
              Sửa
            </button>
            <button v-if="size.status" class="btn btn-sm btn-danger"
                    @click="deleteSizeById(size.id)">
              Xóa
            </button>
            <button v-else class="btn btn-sm btn-success"
                    @click="restoreSizeById(size.id)">
              Khôi phục
            </button>
          </td>
        </tr>
        <tr v-if="filteredSize.length === 0">
          <td colspan="4" class="text-center">
            {{ sizes.length === 0 ? 'Không có size nào.' : 'Không tìm thấy size phù hợp.' }}
          </td>
        </tr>
        </tbody>
      </table>

      <!-- Modal -->
      <div v-if="showModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">{{ isEditing ? 'Cập nhật size' : 'Thêm size' }}</h5>
              <button type="button" class="btn-close" @click="showModal = false; resetModal()"></button>
            </div>
            <div class="modal-body">
              <div class="mb-3">
                <label class="form-label">Giá trị size <span class="text-danger">*</span></label>
                <input
                    type="number"
                    class="form-control"
                    :class="{'is-invalid': formErrors.value}"
                    v-model="currentSize.value"
                    @input="validateSizeValue"
                    min="0"
                    max="50"
                    step="0.1"
                    placeholder="Nhập giá trị size (0-50)"
                />
                <div class="form-text">
                  Nhập giá trị từ 0 đến 50. Ví dụ: 36, 37.5, 38, v.v.
                </div>
                <div class="invalid-feedback" v-if="formErrors.value">
                  {{ formErrors.value }}
                </div>
              </div>
              <div class="mb-3" v-if="isEditing">
                <label class="form-label">Trạng thái</label>
                <select class="form-select" v-model="currentSize.status">
                  <option :value="true">Đang kinh doanh</option>
                  <option :value="false">Ngừng kinh doanh</option>
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showModal = false; resetModal()">Hủy</button>
              <button class="btn btn-primary" @click="saveSize">
                {{ isEditing ? 'Cập nhật' : 'Thêm mới' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <ShowToastComponent ref="toastRef" />

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
            :class="['btn', pendingAction?.type === 'delete' ? 'btn-danger' : 'btn-success']"
            @click="handleConfirmAction"
            :disabled="modalLoading"
        >
          <span v-if="modalLoading" class="spinner-border spinner-border-sm me-2" role="status"></span>
          {{ modalConfig.confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
table th,
table td {
  vertical-align: middle;
}

.modal {
  display: block;
}

.modal-content {
  animation: fadeInUp 0.3s ease-in-out;
  border-radius: 16px;
}

@keyframes fadeInUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0px);
    opacity: 1;
  }
}

.form-label .text-danger {
  font-size: 0.875rem;
}

.invalid-feedback {
  display: block;
}

/* Input field styling when approaching limit */
.form-control:focus {
  border-color: #80bdff;
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

.btn-primary:hover {
  background-color: #0e0e0e !important;
  border: none;
}

.btn-primary:active {
  background-color: #0e0e0e !important;
}

/* Modal confirm styles */
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