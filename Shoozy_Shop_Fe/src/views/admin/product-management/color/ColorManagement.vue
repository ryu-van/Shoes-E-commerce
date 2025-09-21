<script setup>
import { ref, computed, onMounted } from 'vue';
import { Sketch } from '@ckpack/vue-color';
import { getAllColors, createColor, updateColor, deleteColor, restoreColor } from '@/service/ColorApi.js';
import ShowToastComponent from "@/components/ShowToastComponent.vue";

const toastRef = ref(null);

const colors = ref([]);
const currentColor = ref({ id: null, name: '', hexCode: '#000000', status: true });
const formErrors = ref({ name: '', hexCode: '' });
const searchKeyword = ref('');
const showModal = ref(false);
const isEditing = ref(false);

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
    title: isDelete ? 'Xác nhận xóa màu sắc' : 'Xác nhận khôi phục màu sắc',
    message: isDelete
        ? `Bạn có chắc chắn muốn xóa màu sắc "${pendingAction.value.name}"?`
        : `Bạn có chắc chắn muốn khôi phục màu sắc "${pendingAction.value.name}"?`,
    details: isDelete
        ? 'Màu sắc sẽ được chuyển sang trạng thái "Ngừng kinh doanh" và có thể được khôi phục sau.'
        : 'Màu sắc sẽ được chuyển sang trạng thái "Đang kinh doanh" và hiển thị trở lại.',
    type: isDelete ? 'danger' : 'success',
    confirmText: isDelete ? 'Xóa màu sắc' : 'Khôi phục',
    cancelText: 'Hủy bỏ'
  };
});

const fetchColors = async () => {
  try {
    const res = await getAllColors();
    colors.value = res.data.data;
  } catch (error) {
    console.log('Error when loading data colors: ', error);
    showToast('Có lỗi xảy ra khi tải dữ liệu màu sắc', 'error');
  }
};

onMounted(fetchColors);

// Modal confirm functions
const handleConfirmAction = async () => {
  if (!pendingAction.value) return;

  modalLoading.value = true;

  try {
    if (pendingAction.value.type === 'delete') {
      await deleteColor(pendingAction.value.id);
      showToast('Xóa màu sắc thành công!', 'success');
    } else if (pendingAction.value.type === 'restore') {
      await restoreColor(pendingAction.value.id);
      showToast('Khôi phục màu sắc thành công!', 'success');
    }

    await fetchColors();
    showConfirmModal.value = false;
    pendingAction.value = null;
  } catch (error) {
    console.log('Error delete/restore color: ', error);
    showToast('Có lỗi xảy ra khi xử lý màu sắc', 'error');
  } finally {
    modalLoading.value = false;
  }
};

const handleCancelDelete = () => {
  showConfirmModal.value = false;
  pendingAction.value = null;
  modalLoading.value = false;
};

const openAddModal = () => {
  currentColor.value = { name: '', hexCode: '#000000', status: true };
  formErrors.value = { name: '', hexCode: '' };
  isEditing.value = false;
  showModal.value = true;
};

const openEditModal = (color) => {
  currentColor.value = { ...color };
  formErrors.value = { name: '', hexCode: '' };
  isEditing.value = true;
  showModal.value = true;
};

const resetModal = () => {
  currentColor.value = { id: null, name: '', hexCode: '#000000', status: true };
  formErrors.value = { name: '', hexCode: '' };
};

const checkDuplicateName = (name, excludeId = null) => {
  if (!name || !name.trim()) return false;

  const trimmedName = name.trim().toLowerCase();
  return colors.value.some(color =>
      color.name.toLowerCase().trim() === trimmedName &&
      color.id !== excludeId
  );
};

const checkDuplicateHex = (hexCode, excludeId = null) => {
  if (!hexCode || !hexCode.trim()) return false;

  const trimmedHex = hexCode.trim().toLowerCase();
  return colors.value.some(color =>
      color.hexCode.toLowerCase().trim() === trimmedHex &&
      color.id !== excludeId
  );
};

const validateForm = () => {
  let valid = true;
  formErrors.value = { name: '', hexCode: '' };

  // Validate color name
  if (!currentColor.value.name || !currentColor.value.name.trim()) {
    formErrors.value.name = 'Tên màu không được để trống';
    valid = false;
  } else if (currentColor.value.name.length > 50) {
    formErrors.value.name = 'Tên màu không được vượt quá 50 ký tự';
    valid = false;
  } else {
    // Check for special characters - only allow letters, numbers, spaces, and Vietnamese characters
    const specialCharRegex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/;
    if (specialCharRegex.test(currentColor.value.name)) {
      formErrors.value.name = 'Tên màu không được chứa ký tự đặc biệt';
      valid = false;
    } else {
      // Check for duplicate name
      const excludeId = isEditing.value ? currentColor.value.id : null;
      if (checkDuplicateName(currentColor.value.name, excludeId)) {
        formErrors.value.name = 'Tên màu này đã tồn tại trong hệ thống';
        valid = false;
      }
    }
  }

  // Validate hex code
  if (!currentColor.value.hexCode || !currentColor.value.hexCode.trim()) {
    formErrors.value.hexCode = 'Mã màu hex không được để trống';
    valid = false;
  } else if (!/^#[0-9A-Fa-f]{6}$/.test(currentColor.value.hexCode)) {
    formErrors.value.hexCode = 'Mã màu hex không hợp lệ (định dạng: #RRGGBB)';
    valid = false;
  } else {
    // Check for duplicate hex code
    const excludeId = isEditing.value ? currentColor.value.id : null;
    if (checkDuplicateHex(currentColor.value.hexCode, excludeId)) {
      formErrors.value.hexCode = 'Mã màu hex này đã tồn tại trong hệ thống';
      valid = false;
    }
  }

  return valid;
};

// Real-time validation for color name
const validateColorName = () => {
  if (!currentColor.value.name || !currentColor.value.name.trim()) {
    formErrors.value.name = '';
    return;
  }

  if (currentColor.value.name.length > 50) {
    formErrors.value.name = 'Tên màu không được vượt quá 50 ký tự';
    return;
  }

  const specialCharRegex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/;
  if (specialCharRegex.test(currentColor.value.name)) {
    formErrors.value.name = 'Tên màu không được chứa ký tự đặc biệt';
  } else {
    formErrors.value.name = '';
  }

  // Check for duplicate name
  const excludeId = isEditing.value ? currentColor.value.id : null;
  if (checkDuplicateName(currentColor.value.name, excludeId)) {
    formErrors.value.name = 'Tên màu này đã tồn tại trong hệ thống';
    return;
  }
};

// Real-time validation for hex code
const validateHexCode = () => {
  if (!currentColor.value.hexCode || !currentColor.value.hexCode.trim()) {
    formErrors.value.hexCode = '';
    return;
  }

  if (!/^#[0-9A-Fa-f]{6}$/.test(currentColor.value.hexCode)) {
    formErrors.value.hexCode = 'Mã màu hex không hợp lệ (định dạng: #RRGGBB)';
    return;
  }

  formErrors.value.hexCode = '';

  // Check for duplicate hex code
  const excludeId = isEditing.value ? currentColor.value.id : null;
  if (checkDuplicateHex(currentColor.value.hexCode, excludeId)) {
    formErrors.value.hexCode = 'Mã màu hex này đã tồn tại trong hệ thống';
    return;
  }
};

// Character count computed property
const nameCharCount = computed(() => currentColor.value.name ? currentColor.value.name.length : 0);

const saveColor = async () => {
  if (!validateForm()) {
    return;
  }

  // Reset form errors before making API call
  formErrors.value = { name: '', hexCode: '' };

  try {
    if (isEditing.value) {
      await updateColor(currentColor.value.id, currentColor.value);
      showToast('Cập nhật màu thành công!', 'success');
    } else {
      await createColor(currentColor.value);
      showToast('Thêm màu mới thành công!', 'success');
    }
    showModal.value = false;
    resetModal();
    await fetchColors();
  } catch (error) {
    console.error('Error saving color: ', error);

    // Handle different types of errors
    if (error.response) {
      // Server responded with error status
      const status = error.response.status;
      const errorData = error.response.data;

      // Check if it's our custom API response format
      if (errorData && errorData.message) {
        const errorMessage = errorData.message;

        // Check for duplicate color error
        if (errorMessage.includes('already exists!') || status === 409) {
          if (errorMessage.toLowerCase().includes('name')) {
            formErrors.value.name = 'Tên màu này đã tồn tại trong hệ thống';
          } else if (errorMessage.toLowerCase().includes('hex')) {
            formErrors.value.hexCode = 'Mã màu hex này đã tồn tại trong hệ thống';
          } else {
            formErrors.value.name = 'Màu này đã tồn tại trong hệ thống';
          }
        } else {
          // Other server errors
          showToast(errorMessage, 'error');
        }
      } else if (errorData && errorData.data && errorData.data.message) {
        // Alternative error format
        const errorMessage = errorData.data.message;
        if (errorMessage.includes('already exists!')) {
          formErrors.value.name = 'Màu này đã tồn tại trong hệ thống';
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

const deleteColorById = async (id) => {
  // Tìm thông tin màu sắc để hiển thị trong modal
  const color = colors.value.find(c => c.id === id);
  if (!color) {
    showToast('Không tìm thấy thông tin màu sắc!', 'error');
    return;
  }
  
  // Hiển thị modal xác nhận xóa
  pendingAction.value = {
    type: 'delete',
    id: color.id,
    name: color.name
  };
  showConfirmModal.value = true;
};

const restoreColorById = async (id) => {
  // Tìm thông tin màu sắc để hiển thị trong modal
  const color = colors.value.find(c => c.id === id);
  if (!color) {
    showToast('Không tìm thấy thông tin màu sắc!', 'error');
    return;
  }
  
  // Hiển thị modal xác nhận khôi phục
  pendingAction.value = {
    type: 'restore',
    id: color.id,
    name: color.name
  };
  showConfirmModal.value = true;
};

const filteredColors = computed(() => {
  if (!searchKeyword.value) return colors.value;
  const keyword = searchKeyword.value.toLowerCase().trim();
  return colors.value.filter(c =>
      c.name.toLowerCase().includes(keyword) ||
      c.hexCode.toLowerCase().includes(keyword)
  );
});
</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">🎨 Quản lý màu sắc</h2>
    <div class="row mb-3">
      <div class="col-8">
        <input type="text" class="form-control" placeholder="🔍 Tìm theo tên màu sắc, mã hex..." v-model="searchKeyword" />
      </div>
      <div class="col-4">
        <button class="btn btn-primary" @click="openAddModal">
          <i class="fas fa-plus me-2"></i> Thêm màu sắc
        </button>
      </div>
    </div>
    <div class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
        <tr>
          <th style="width: 5%">STT</th>
          <th>Tên màu sắc</th>
          <th>Mã màu</th>
          <th>Trạng thái</th>
          <th style="width: 20%">Hành động</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(color, index) in filteredColors" :key="color.id">
          <td>{{ index + 1 }}</td>
          <td>{{ color.name }}</td>
          <td>
            <div class="d-flex align-items-center">
              <div :style="{ backgroundColor: color.hexCode, width: '20px', height: '20px', borderRadius: '50%', marginRight: '10px', border: '1px solid #ccc' }"></div>
              {{ color.hexCode }}
            </div>
          </td>
          <td>
              <span :class="color.status ? 'badge bg-success' : 'badge bg-secondary'" style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
                {{ color.status ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
              </span>
          </td>
          <td>
            <button class="btn btn-sm btn-warning me-2" @click="openEditModal(color)">Sửa</button>
            <button v-if="color.status" class="btn btn-sm btn-danger"
                    @click="deleteColorById(color.id)">
              Xóa
            </button>
            <button v-else class="btn btn-sm btn-success"
                    @click="restoreColorById(color.id)">
              Khôi phục
            </button>
          </td>
        </tr>
        <tr v-if="filteredColors.length === 0">
          <td colspan="5" class="text-center">
            {{ colors.length === 0 ? 'Không có màu sắc nào.' : 'Không tìm thấy màu sắc phù hợp.' }}
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ isEditing ? 'Cập nhật màu sắc' : 'Thêm màu sắc' }}</h5>
            <button type="button" class="btn-close" @click="showModal = false; resetModal()"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <div class="d-flex justify-content-between align-items-center">
                <label class="form-label">Tên màu sắc <span class="text-danger">*</span></label>
                <small class="text-muted">
                  {{ nameCharCount }}/50
                </small>
              </div>
              <input
                  type="text"
                  class="form-control"
                  :class="{'is-invalid': formErrors.name}"
                  v-model="currentColor.name"
                  @input="validateColorName"
                  maxlength="50"
                  placeholder="Nhập tên màu sắc"
              />
              <div class="invalid-feedback" v-if="formErrors.name">
                {{ formErrors.name }}
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label">Mã màu (hex) <span class="text-danger">*</span></label>
              <div class="mb-2">
                <input
                    type="text"
                    class="form-control mb-2"
                    :class="{'is-invalid': formErrors.hexCode}"
                    v-model="currentColor.hexCode"
                    @input="validateHexCode"
                    placeholder="#000000"
                    pattern="^#[0-9A-Fa-f]{6}$"
                />
                <div class="invalid-feedback" v-if="formErrors.hexCode">
                  {{ formErrors.hexCode }}
                </div>
              </div>
              <Sketch
                  :modelValue="{ hex: currentColor.hexCode }"
                  @update:modelValue="val => { currentColor.hexCode = val.hex; validateHexCode(); }"
                  :disable-alpha="true"
                  style="width: 100%; border-radius: 12px; overflow: hidden;"
              />
            </div>

            <div class="mb-3" v-if="isEditing">
              <label class="form-label">Trạng thái</label>
              <select class="form-select" v-model="currentColor.status">
                <option :value="true">Đang kinh doanh</option>
                <option :value="false">Ngừng kinh doanh</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="showModal = false; resetModal()">Hủy</button>
            <button class="btn btn-primary" @click="saveColor">
              {{ isEditing ? 'Cập nhật' : 'Thêm mới' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <ShowToastComponent ref="toastRef"/>

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