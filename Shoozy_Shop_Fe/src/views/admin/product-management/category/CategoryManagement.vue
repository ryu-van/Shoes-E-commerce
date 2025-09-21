<script setup>
import { ref, onMounted, computed } from 'vue';
import {
  getAllCategories,
  createCategory,
  deleteCategory,
  updateCategory, restoreCategory
} from "@/service/CategoryApi.js";
import ShowToastComponent from "@/components/ShowToastComponent.vue";

const toastRef = ref(null);
const categories = ref([]);
const showModal = ref(false);
const isEditing = ref(false);
const searchKeyword = ref('');
const formErrors = ref({ name: '', description: '', status: '' });
const currentCategory = ref({
  id: '', name: '', description: '', status: ''
});

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type);
};

const showConfirmModal = ref(false);
const modalLoading = ref(false);
const pendingAction = ref(null);

const modalConfig = computed(() => {
  if (!pendingAction.value) return {};

  const isDelete = pendingAction.value.type === 'delete';

  return {
    title: isDelete ? 'Xác nhận xóa danh mục' : 'Xác nhận khôi phục danh mục',
    message: isDelete
        ? `Bạn có chắc chắn muốn xóa danh mục "${pendingAction.value.name}"?`
        : `Bạn có chắc chắn muốn khôi phục danh mục "${pendingAction.value.name}"?`,
    details: isDelete
        ? 'Danh mục sẽ được chuyển sang trạng thái "Ngừng kinh doanh" và có thể được khôi phục sau.'
        : 'Danh mục sẽ được chuyển sang trạng thái "Đang kinh doanh" và hiển thị trở lại.',
    type: isDelete ? 'danger' : 'success',
    confirmText: isDelete ? 'Xóa danh mục' : 'Khôi phục',
    cancelText: 'Hủy bỏ'
  };
});

const showDeleteConfirm = (category) => {
  pendingAction.value = {
    type: 'delete',
    id: category.id,
    name: category.name
  };
  showConfirmModal.value = true;
};

const showRestoreConfirm = (category) => {
  pendingAction.value = {
    type: 'restore',
    id: category.id,
    name: category.name
  };
  showConfirmModal.value = true;
};

const handleConfirmAction = async () => {
  if (!pendingAction.value) return;

  modalLoading.value = true;

  try {
    if (pendingAction.value.type === 'delete') {
      await deleteCategory(pendingAction.value.id, { status: false });
      showToast('Xóa danh mục thành công!', 'success');
    } else if (pendingAction.value.type === 'restore') {
      await restoreCategory(pendingAction.value.id, { status: true });
      showToast('Khôi phục danh mục thành công!', 'success');
    }

    await fetchCategories();
    showConfirmModal.value = false;
    pendingAction.value = null;
  } catch (error) {
    console.log('Error delete/restore category: ', error);
    showToast('Có lỗi xảy ra khi xử lý danh mục', 'error');
  } finally {
    modalLoading.value = false;
  }
};

const handleCancelDelete = () => {
  showConfirmModal.value = false;
  pendingAction.value = null;
  modalLoading.value = false;
};

// modal
const openAddModal = () => {
  isEditing.value = false;
  currentCategory.value = {name: '', description: ''};
  showModal.value = true;
};

const openEditModal = (category) => {
  isEditing.value = true;
  currentCategory.value = {...category};
  showModal.value = true;
};

const resetModal = () => {
  currentCategory.value = {id: '', name: '', description: '', status: ''}
  formErrors.value = {name: '', description: '', status: ''}
}

// fetch data
const fetchCategories = async () => {
  try {
    const res = await getAllCategories();
    categories.value = res.data.data;
  } catch (error) {
    console.log('Error when loading data categories: ', error);
    showToast('Có lỗi xảy ra khi tải dữ liệu danh mục', 'error');
  }
};

onMounted(fetchCategories);

const checkDuplicateName = (name, excludeId = null) => {
  if (!name || !name.trim()) return false;

  const trimmedName = name.trim().toLowerCase();
  return categories.value.some(category =>
      category.name.toLowerCase().trim() === trimmedName &&
      category.id !== excludeId
  );
};

const validateForm = () => {
  let valid = true;
  formErrors.value = {name: '', description: '', status: ''};

  // Validate category name
  if (!currentCategory.value.name || !currentCategory.value.name.trim()) {
    formErrors.value.name = 'Tên danh mục không được để trống';
    valid = false;
  } else if (currentCategory.value.name.length > 100) {
    formErrors.value.name = 'Tên danh mục không được vượt quá 100 ký tự';
    valid = false;
  } else {
    // Check for special characters - only allow letters, numbers, spaces, and Vietnamese characters
    const specialCharRegex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/;
    if (specialCharRegex.test(currentCategory.value.name)) {
      formErrors.value.name = 'Tên danh mục không được chứa ký tự đặc biệt';
      valid = false;
    } else {
      // Check for duplicate name
      const excludeId = isEditing.value ? currentCategory.value.id : null;
      if (checkDuplicateName(currentCategory.value.name, excludeId)) {
        formErrors.value.name = 'Danh mục này đã tồn tại trong hệ thống';
        valid = false;
      }
    }
  }

  // Validate description
  if (!currentCategory.value.description || !currentCategory.value.description.trim()) {
    formErrors.value.description = 'Mô tả danh mục không được để trống';
    valid = false;
  } else if (currentCategory.value.description.length > 255) {
    formErrors.value.description = 'Mô tả không được vượt quá 255 ký tự';
    valid = false;
  }

  return valid;
};

// Real-time validation for category name
const validateCategoryName = () => {
  if (!currentCategory.value.name || !currentCategory.value.name.trim()) {
    formErrors.value.name = '';
    return;
  }

  if (currentCategory.value.name.length > 100) {
    formErrors.value.name = 'Tên danh mục không được vượt quá 100 ký tự';
    return;
  }

  const specialCharRegex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/;
  if (specialCharRegex.test(currentCategory.value.name)) {
    formErrors.value.name = 'Tên danh mục không được chứa ký tự đặc biệt';
  } else {
    formErrors.value.name = '';
  }

  // Kiểm tra trùng tên - FIX: Sử dụng currentCategory.value.id thay vì currentBrand.value.id
  const excludeId = isEditing.value ? currentCategory.value.id : null;
  if (checkDuplicateName(currentCategory.value.name, excludeId)) {
    formErrors.value.name = 'Danh mục này đã tồn tại trong hệ thống';
    return;
  }
};

// Real-time validation for description
const validateDescription = () => {
  if (currentCategory.value.description && currentCategory.value.description.length > 255) {
    formErrors.value.description = 'Mô tả không được vượt quá 255 ký tự';
  } else {
    formErrors.value.description = '';
  }
};

// Character count computed properties
const nameCharCount = computed(() => currentCategory.value.name ? currentCategory.value.name.length : 0);
const descriptionCharCount = computed(() => currentCategory.value.description ? currentCategory.value.description.length : 0);

const saveCategory = async () => {
  if (!validateForm()) {
    return;
  }

  // Reset form errors before making API call
  formErrors.value = {name: '', description: '', status: ''};

  try {
    if (isEditing.value) {
      await updateCategory(currentCategory.value.id, currentCategory.value);
      showToast('Cập nhật danh mục thành công!', 'success');
    } else {
      await createCategory(currentCategory.value);
      showToast('Thêm danh mục thành công!', 'success');
    }
    showModal.value = false;
    resetModal();
    await fetchCategories();
  } catch (error) {
    console.log('Error saving category: ', error);

    // Handle different types of errors
    if (error.response) {
      // Server responded with error status
      const status = error.response.status;
      const errorData = error.response.data;

      // Check if it's our custom API response format
      if (errorData && errorData.message) {
        const errorMessage = errorData.message;

        // Check for duplicate category error
        if (errorMessage.includes('already exists!') || status === 409) {
          formErrors.value.name = 'Danh mục này đã tồn tại trong hệ thống';
        } else {
          // Other server errors
          showToast(errorMessage, 'error');
        }
      } else if (errorData && errorData.data && errorData.data.message) {
        // Alternative error format
        const errorMessage = errorData.data.message;
        if (errorMessage.includes('already exists!')) {
          formErrors.value.name = 'Danh mục này đã tồn tại trong hệ thống';
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

const filteredCategories = computed(() => {
  if (!searchKeyword.value) return categories.value;
  const keyword = searchKeyword.value.toLowerCase().trim();
  return categories.value.filter(c =>
      c.name.toLowerCase().includes(keyword) ||
      (c.description && c.description.toLowerCase().includes(keyword))
  );
});

</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">📋 Quản lý danh mục</h2>
    <div class="row mb-3">
      <div class="col-8">
        <form class="mb-3">
          <input type="text" class="form-control" placeholder="🔍 Tìm theo tên danh mục, mô tả..."
                 v-model="searchKeyword"/>
        </form>
      </div>
      <div class="col-4">
        <button class="btn btn-primary" @click="openAddModal">
          <i class="fas fa-plus me-2"></i> Thêm danh mục
        </button>
      </div>
    </div>
    <div class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
        <tr>
          <th style="width: 5%">STT</th>
          <th>Tên danh mục</th>
          <th>Mô tả</th>
          <th>Trạng thái</th>
          <th style="width: 20%">Hành động</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(category, index) in filteredCategories" :key="category.id">
          <td>{{ index + 1 }}</td>
          <td>{{ category.name }}</td>
          <td>{{ category.description }}</td>
          <td>
            <span :class="category.status ? 'badge bg-success' : 'badge bg-secondary'"
                  style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
              {{ category.status === true ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
            </span>
          </td>
          <td>
            <button class="btn btn-sm btn-warning me-2" @click="openEditModal(category)">
              Sửa
            </button>
            <button v-if="category.status" class="btn btn-sm btn-danger"
                    @click="showDeleteConfirm(category)">
              Xóa
            </button>
            <button v-else class="btn btn-sm btn-success"
                    @click="showRestoreConfirm(category)">
              Khôi phục
            </button>
          </td>
        </tr>
        <tr v-if="filteredCategories.length === 0">
          <td colspan="5" class="text-center">
            {{ categories.length === 0 ? 'Không có danh mục nào.' : 'Không tìm thấy danh mục phù hợp.' }}
          </td>
        </tr>
        </tbody>
      </table>

      <!-- Modal -->
      <div v-if="showModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">{{ isEditing ? 'Cập nhật danh mục' : 'Thêm danh mục' }}</h5>
              <button type="button" class="btn-close" @click="showModal = false; resetModal()"></button>
            </div>
            <div class="modal-body">
              <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center">
                  <label class="form-label">Tên danh mục <span class="text-danger">*</span></label>
                  <small class="text-muted">
                    {{ nameCharCount }}/100
                  </small>
                </div>
                <input
                    type="text"
                    class="form-control"
                    :class="{'is-invalid': formErrors.name}"
                    v-model="currentCategory.name"
                    @input="validateCategoryName"
                    maxlength="100"
                    placeholder="Nhập tên danh mục"
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
                    v-model="currentCategory.description"
                    @input="validateDescription"
                    maxlength="255"
                    placeholder="Nhập mô tả danh mục"
                    rows="3"
                ></textarea>
                <div class="invalid-feedback" v-if="formErrors.description">
                  {{ formErrors.description }}
                </div>
              </div>
              <div class="mb-3" v-if="isEditing">
                <label class="form-label">Trạng thái</label>
                <select class="form-select" v-model="currentCategory.status">
                  <option :value="true">Đang kinh doanh</option>
                  <option :value="false">Ngừng kinh doanh</option>
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showModal = false; resetModal()">Hủy</button>
              <button class="btn btn-primary" @click="saveCategory">
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
            @click="handleConfirmAction"
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