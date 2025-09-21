<script setup>
import { ref, onMounted, computed } from 'vue';
import {getAllMaterials, createMaterial, deleteMaterial, updateMaterial, restoreMaterial} from "@/service/MaterialApi.js";
import ShowToastComponent from "@/components/ShowToastComponent.vue";

const toastRef = ref(null);

const materials = ref([]);
const showModal = ref(false);
const isEditing = ref(false);
const searchKeyword = ref('');
const formErrors = ref({ name: '', description: '', status: '' });
const currentMaterial = ref({
  id: '', name: '', description: '', status: ''
});

// message
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
    title: isDelete ? 'Xác nhận xóa chất liệu' : 'Xác nhận khôi phục chất liệu',
    message: isDelete
        ? `Bạn có chắc chắn muốn xóa chất liệu "${pendingAction.value.name}"?`
        : `Bạn có chắc chắn muốn khôi phục chất liệu "${pendingAction.value.name}"?`,
    details: isDelete
        ? 'Chất liệu sẽ được chuyển sang trạng thái "Ngừng kinh doanh" và có thể được khôi phục sau.'
        : 'Chất liệu sẽ được chuyển sang trạng thái "Đang kinh doanh" và hiển thị trở lại.',
    type: isDelete ? 'danger' : 'success',
    confirmText: isDelete ? 'Xóa chất liệu' : 'Khôi phục',
    cancelText: 'Hủy bỏ'
  };
});

// modal
const openAddModal = () => {
  isEditing.value = false;
  currentMaterial.value = {name: '', description: ''};
  showModal.value = true;
};

const openEditModal = (material) => {
  isEditing.value = true;
  currentMaterial.value = {...material};
  showModal.value = true;
};

const resetModal = () => {
  currentMaterial.value = {id: '', name: '', description: '', status: ''}
  formErrors.value = {name: '', description: '', status: ''}
}

// fetch data
const fetchMaterials = async () => {
  try {
    const res = await getAllMaterials();
    materials.value = res.data.data;
  } catch (error) {
    console.log('Error when loading data materials: ', error);
    showToast('Có lỗi xảy ra khi tải dữ liệu chất liệu', 'error');
  }
};

onMounted(fetchMaterials);

// Modal confirm functions
const handleConfirmAction = async () => {
  if (!pendingAction.value) return;

  modalLoading.value = true;

  try {
    if (pendingAction.value.type === 'delete') {
      await deleteMaterial(pendingAction.value.id);
      showToast('Xóa chất liệu thành công!', 'success');
    } else if (pendingAction.value.type === 'restore') {
      await restoreMaterial(pendingAction.value.id);
      showToast('Khôi phục chất liệu thành công!', 'success');
    }

    await fetchMaterials();
    showConfirmModal.value = false;
    pendingAction.value = null;
  } catch (error) {
    console.log('Error delete/restore material: ', error);
    showToast('Có lỗi xảy ra khi xử lý chất liệu', 'error');
  } finally {
    modalLoading.value = false;
  }
};

const handleCancelDelete = () => {
  showConfirmModal.value = false;
  pendingAction.value = null;
  modalLoading.value = false;
};

const checkDuplicateName = (name, excludeId = null) => {
  if (!name || !name.trim()) return false;

  const trimmedName = name.trim().toLowerCase();
  return materials.value.some(material =>
      material.name.toLowerCase().trim() === trimmedName &&
      material.id !== excludeId
  );
};

const validateForm = () => {
  let valid = true;
  formErrors.value = {name: '', description: '', status: ''};

  // Validate material name
  if (!currentMaterial.value.name || !currentMaterial.value.name.trim()) {
    formErrors.value.name = 'Tên chất liệu không được để trống';
    valid = false;
  } else if (currentMaterial.value.name.length > 100) {
    formErrors.value.name = 'Tên chất liệu không được vượt quá 100 ký tự';
    valid = false;
  } else {
    // Check for special characters - only allow letters, numbers, spaces, and Vietnamese characters
    const specialCharRegex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/;
    if (specialCharRegex.test(currentMaterial.value.name)) {
      formErrors.value.name = 'Tên chất liệu không được chứa ký tự đặc biệt';
      valid = false;
    } else {
      // Check for duplicate name
      const excludeId = isEditing.value ? currentMaterial.value.id : null;
      if (checkDuplicateName(currentMaterial.value.name, excludeId)) {
        formErrors.value.name = 'Chất liệu này đã tồn tại trong hệ thống';
        valid = false;
      }
    }
  }

  // Validate description
  if (!currentMaterial.value.description || !currentMaterial.value.description.trim()) {
    formErrors.value.description = 'Mô tả chất liệu không được để trống';
    valid = false;
  } else if (currentMaterial.value.description.length > 255) {
    formErrors.value.description = 'Mô tả không được vượt quá 255 ký tự';
    valid = false;
  }

  return valid;
};

// Real-time validation for material name
const validateMaterialName = () => {
  if (!currentMaterial.value.name || !currentMaterial.value.name.trim()) {
    formErrors.value.name = '';
    return;
  }

  if (currentMaterial.value.name.length > 100) {
    formErrors.value.name = 'Tên chất liệu không được vượt quá 100 ký tự';
    return;
  }

  const specialCharRegex = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/;
  if (specialCharRegex.test(currentMaterial.value.name)) {
    formErrors.value.name = 'Tên chất liệu không được chứa ký tự đặc biệt';
  } else {
    formErrors.value.name = '';
  }

  // Kiểm tra trùng tên
  const excludeId = isEditing.value ? currentMaterial.value.id : null;
  if (checkDuplicateName(currentMaterial.value.name, excludeId)) {
    formErrors.value.name = 'Chất liệu này đã tồn tại trong hệ thống';
    return;
  }
};

// Real-time validation for description
const validateDescription = () => {
  if (currentMaterial.value.description && currentMaterial.value.description.length > 255) {
    formErrors.value.description = 'Mô tả không được vượt quá 255 ký tự';
  } else {
    formErrors.value.description = '';
  }
};

// Character count computed properties
const nameCharCount = computed(() => currentMaterial.value.name ? currentMaterial.value.name.length : 0);
const descriptionCharCount = computed(() => currentMaterial.value.description ? currentMaterial.value.description.length : 0);

const saveMaterial = async () => {
  if (!validateForm()) {
    return;
  }

  // Reset form errors before making API call
  formErrors.value = {name: '', description: '', status: ''};

  try {
    if (isEditing.value) {
      await updateMaterial(currentMaterial.value.id, currentMaterial.value);
      showToast('Cập nhật chất liệu thành công!', 'success');
    } else {
      await createMaterial(currentMaterial.value);
      showToast('Thêm chất liệu thành công!', 'success');
    }
    showModal.value = false;
    resetModal();
    await fetchMaterials();
  } catch (error) {
    console.log('Error saving material: ', error);

    // Handle different types of errors
    if (error.response) {
      // Server responded with error status
      const status = error.response.status;
      const errorData = error.response.data;

      // Check if it's our custom API response format
      if (errorData && errorData.message) {
        const errorMessage = errorData.message;

        // Check for duplicate material error
        if (errorMessage.includes('already exists!') || status === 409) {
          formErrors.value.name = 'Chất liệu này đã tồn tại trong hệ thống';
        } else {
          // Other server errors
          showToast(errorMessage, 'error');
        }
      } else if (errorData && errorData.data && errorData.data.message) {
        // Alternative error format
        const errorMessage = errorData.data.message;
        if (errorMessage.includes('already exists!')) {
          formErrors.value.name = 'Chất liệu này đã tồn tại trong hệ thống';
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

const deleteMaterialById = async (id) => {
  // Tìm thông tin chất liệu để hiển thị trong modal
  const material = materials.value.find(m => m.id === id);
  if (!material) {
    showToast('Không tìm thấy thông tin chất liệu!', 'error');
    return;
  }
  
  // Hiển thị modal xác nhận xóa
  pendingAction.value = {
    type: 'delete',
    id: material.id,
    name: material.name
  };
  showConfirmModal.value = true;
};

const restoreMaterialById = async (id) => {
  // Tìm thông tin chất liệu để hiển thị trong modal
  const material = materials.value.find(m => m.id === id);
  if (!material) {
    showToast('Không tìm thấy thông tin chất liệu!', 'error');
    return;
  }
  
  // Hiển thị modal xác nhận khôi phục
  pendingAction.value = {
    type: 'restore',
    id: material.id,
    name: material.name
  };
  showConfirmModal.value = true;
};

const filteredMaterials = computed(() => {
  if (!searchKeyword.value) return materials.value;
  const keyword = searchKeyword.value.toLowerCase().trim();
  return materials.value.filter(m =>
      m.name.toLowerCase().includes(keyword) ||
      (m.description && m.description.toLowerCase().includes(keyword))
  );
});

</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">👟 Quản lý chất liệu</h2>
    <div class="row mb-3">
      <div class="col-8">
        <form class="mb-3">
          <input type="text" class="form-control" placeholder="🔍 Tìm theo tên chất liệu, mô tả..."
                 v-model="searchKeyword"/>
        </form>
      </div>
      <div class="col-4">
        <button class="btn btn-primary" @click="openAddModal">
          <i class="fas fa-plus me-2"></i> Thêm chất liệu
        </button>
      </div>
    </div>
    <div class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
        <tr>
          <th style="width: 5%">STT</th>
          <th>Tên chất liệu</th>
          <th>Mô tả</th>
          <th>Trạng thái</th>
          <th style="width: 20%">Hành động</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(material, index) in filteredMaterials" :key="material.id">
          <td>{{ index + 1 }}</td>
          <td>{{ material.name }}</td>
          <td>{{ material.description }}</td>
          <td>
            <span :class="material.status ? 'badge bg-success' : 'badge bg-secondary'"
                  style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
              {{ material.status === true ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
            </span>
          </td>
                     <td>
             <button class="btn btn-sm btn-warning me-2" @click="openEditModal(material)">
               Sửa
             </button>
             <button v-if="material.status" class="btn btn-sm btn-danger"
                     @click="deleteMaterialById(material.id)">
               Xóa
             </button>
             <button v-else class="btn btn-sm btn-success"
                     @click="restoreMaterialById(material.id)">
               Khôi phục
             </button>
           </td>
        </tr>
        <tr v-if="filteredMaterials.length === 0">
          <td colspan="5" class="text-center">
            {{ materials.length === 0 ? 'Không có chất liệu nào.' : 'Không tìm thấy chất liệu phù hợp.' }}
          </td>
        </tr>
        </tbody>
      </table>

      <!-- Modal -->
      <div v-if="showModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">{{ isEditing ? 'Cập nhật chất liệu' : 'Thêm chất liệu' }}</h5>
              <button type="button" class="btn-close" @click="showModal = false; resetModal()"></button>
            </div>
            <div class="modal-body">
              <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center">
                  <label class="form-label">Tên chất liệu <span class="text-danger">*</span></label>
                  <small class="text-muted">
                    {{ nameCharCount }}/100
                  </small>
                </div>
                <input
                    type="text"
                    class="form-control"
                    :class="{'is-invalid': formErrors.name}"
                    v-model="currentMaterial.name"
                    @input="validateMaterialName"
                    maxlength="100"
                    placeholder="Nhập tên chất liệu"
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
                    v-model="currentMaterial.description"
                    @input="validateDescription"
                    maxlength="255"
                    placeholder="Nhập mô tả chất liệu"
                    rows="3"
                ></textarea>
                <div class="invalid-feedback" v-if="formErrors.description">
                  {{ formErrors.description }}
                </div>
              </div>
              <div class="mb-3" v-if="isEditing">
                <label class="form-label">Trạng thái</label>
                <select class="form-select" v-model="currentMaterial.status">
                  <option :value="true">Đang kinh doanh</option>
                  <option :value="false">Ngừng kinh doanh</option>
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showModal = false; resetModal()">Hủy</button>
              <button class="btn btn-primary" @click="saveMaterial">
                {{ isEditing ? 'Cập nhật' : 'Thêm mới' }}
              </button>
            </div>
          </div>
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