<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-layout">
        <!-- Left Sidebar -->
        <div class="sidebar">
          <!-- Profile Avatar -->
          <div class="profile-section">
            <div class="photo-section">
              <h3 class="photo-title">Ảnh</h3>
              <div class="current-photo">
                <div class="avatar">
                  <img :src="formData.avatar" alt="Profile" />
                  <!-- Loading overlay for avatar upload -->
                  <div v-if="avatarUploading" class="avatar-loading">
                    <div class="spinner"></div>
                  </div>
                </div>
                <div class="photo-info">
                  <h4 class="user-name">{{ formData.fullname || 'User Name' }}</h4>
                </div>
              </div>
            </div>

            <!-- Upload Section -->
            <div class="upload-section">
              <div
                  class="upload-area"
                  @click="uploadAvatar"
                  @dragover.prevent
                  @drop="handleDrop"
                  :class="{ 'uploading': avatarUploading }"
              >
                <div class="upload-icon">
                  <svg v-if="!avatarUploading" width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M21 15V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M17 8L12 3L7 8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M12 3V15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  <div v-else class="spinner"></div>
                </div>
                <p class="upload-text">
                  <span class="upload-link">
                    {{ avatarUploading ? 'Đang tải...' : 'Click để tải lên' }}
                  </span>
                  {{ !avatarUploading ? ' hoặc kéo thả vào' : '' }}
                </p>
                <p class="upload-info" v-if="!avatarUploading">
                  SVG, PNG or JPG (max. 5MB)
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Main Form -->
        <div class="form-section">
          <h2 class="form-title">Thông tin cá nhân</h2>

          <form class="profile-form" @submit.prevent="saveForm">
            <!-- Hidden file input -->
            <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="handleFileSelect"/>

            <!-- Row 1 - Full Name & Gender -->
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Họ và tên *</label>
                <input
                    type="text"
                    class="form-input"
                    :class="{ 'error': errors.fullname }"
                    v-model="formData.fullname"
                    placeholder="Enter full name"
                    @blur="validateFullname"
                    @input="clearError('fullname')"
                />
                <span v-if="errors.fullname" class="error-message">{{ errors.fullname }}</span>
              </div>
              <div class="form-group">
                <label class="form-label">Giới tính *</label>
                <select class="form-select" :class="{ 'error': errors.gender }" v-model="formData.gender" @change="clearError('gender')">
                  <!--                  <option value="">Chọn giới tính</option>-->
                  <option :value="true">Nam</option>
                  <option :value="false">Nữ</option>
                </select>
                <span v-if="errors.gender" class="error-message">{{ errors.gender }}</span>
              </div>
            </div>

            <!-- Row 2 - Email & Phone -->
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Email *</label>
                <input
                    type="email"
                    class="form-input"
                    :class="{ 'error': errors.email }"
                    v-model="formData.email"
                    placeholder="example@email.com"
                    @blur="validateEmail"
                    @input="clearError('email')"
                />
                <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
              </div>
              <div class="form-group">
                <label class="form-label">Số điện thoại *</label>
                <input
                    type="tel"
                    class="form-input"
                    :class="{ 'error': errors.phoneNumber }"
                    v-model="formData.phoneNumber"
                    placeholder="0123456789"
                    maxlength="10"
                    @blur="validatePhone"
                    @input="formatPhoneNumber"
                />
                <span v-if="errors.phoneNumber" class="error-message">{{ errors.phoneNumber }}</span>
              </div>
            </div>

            <!-- Row 3 - Date of Birth & Password -->
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Ngày sinh *</label>
                <input
                    type="date"
                    class="form-input"
                    :class="{ 'error': errors.dateOfBirth }"
                    v-model="formData.dateOfBirth"
                    @blur="validateDateOfBirth"
                    @input="clearError('dateOfBirth')"
                />
                <span v-if="errors.dateOfBirth" class="error-message">{{ errors.dateOfBirth }}</span>
              </div>
              <div class="form-group">
                <label class="form-label">Mật khẩu </label>
                <div class="password-field">
                  <input
                      type="password"
                      class="form-input password-input"
                      value="••••••••••••••"
                      readonly
                  />
                  <button
                      type="button"
                      class="edit-password-btn"
                      @click="showPasswordModal = true"
                  >
                    Chỉnh sửa
                  </button>
                </div>
              </div>
            </div>

            <!-- Row 4 - Address (Full Width) -->
            <div class="form-row">
              <div class="form-group form-group-full">
                <label class="form-label">Địa chỉ *</label>
                <input type="text" class="form-input" v-model="formData.address" readonly>
                <button type="button" class="btn-address" @click="showAddressModal = true">Danh sách địa chỉ</button>
              </div>
            </div>

            <!-- Form Actions -->
            <div class="form-actions">
              <button type="button" class="btn-reset" @click="resetForm">
                Reset
              </button>
              <button type="submit" class="btn-save" :disabled="loading">
                {{ loading ? 'Đang lưu...' : 'Lưu lại' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Password Edit Modal -->
    <div v-if="showPasswordModal" class="modal-overlay" @click="closePasswordModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">Chỉnh sửa mật khẩu</h3>
          <button class="modal-close" @click="closePasswordModal">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>

        <form class="modal-form" @submit.prevent="updatePassword">
          <!-- Current Password -->
          <div class="form-group">
            <label class="form-label">Mật khẩu hiện tại *</label>
            <div class="password-input-wrapper">
              <input
                  :type="showOldPassword ? 'text' : 'password'"
                  class="form-input password-input-field"
                  :class="{ 'error': passwordErrors.oldPassword }"
                  v-model="passwordForm.oldPassword"
                  placeholder="Nhập mật khẩu hiện tại"
                  @input="clearPasswordError('oldPassword')"
              />
              <button
                  type="button"
                  class="password-toggle"
                  @click="showOldPassword = !showOldPassword"
              >
                <svg v-if="showOldPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M1 1l22 22" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M1 12s4-8 11-8 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M1 1l22 22" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </div>
            <span v-if="passwordErrors.oldPassword" class="error-message">{{ passwordErrors.oldPassword }}</span>
          </div>

          <!-- New Password -->
          <div class="form-group">
            <label class="form-label">Mật khẩu mới *</label>
            <div class="password-input-wrapper">
              <input
                  :type="showNewPassword ? 'text' : 'password'"
                  class="form-input password-input-field"
                  :class="{ 'error': passwordErrors.newPassword }"
                  v-model="passwordForm.newPassword"
                  placeholder="Nhập mật khẩu mới"
                  @input="clearPasswordError('newPassword')"
              />
              <button
                  type="button"
                  class="password-toggle"
                  @click="showNewPassword = !showNewPassword"
              >
                <svg v-if="showNewPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M1 12s4-8 11-8 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </div>
            <span v-if="passwordErrors.newPassword" class="error-message">{{ passwordErrors.newPassword }}</span>
          </div>

          <!-- Confirm Password -->
          <div class="form-group">
            <label class="form-label">Xác nhận mật khẩu mới *</label>
            <div class="password-input-wrapper">
              <input
                  :type="showConfirmPassword ? 'text' : 'password'"
                  class="form-input password-input-field"
                  :class="{ 'error': passwordErrors.confirmPassword }"
                  v-model="passwordForm.confirmPassword"
                  placeholder="Nhập lại mật khẩu mới"
                  @input="clearPasswordError('confirmPassword')"
              />
              <button
                  type="button"
                  class="password-toggle"
                  @click="showConfirmPassword = !showConfirmPassword"
              >
                <svg v-if="showConfirmPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </div>
            <span v-if="passwordErrors.confirmPassword" class="error-message">{{ passwordErrors.confirmPassword }}</span>
          </div>

          <div class="password-requirements">
            <p class="requirements-title">Yêu cầu mật khẩu:</p>
            <div class="requirement-item" :class="{ 'valid': passwordValidation.minLength }">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M13.854 4.146a.5.5 0 010 .708l-7 7a.5.5 0 01-.708 0l-3.5-3.5a.5.5 0 11.708-.708L6.5 10.793l6.646-6.647a.5.5 0 01.708 0z" fill="currentColor"/>
              </svg>
              <span>Tối thiểu 6 ký tự</span>
            </div>
            <div class="requirement-item" :class="{ 'valid': passwordValidation.hasRequiredChars }">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M13.854 4.146a.5.5 0 010 .708l-7 7a.5.5 0 01-.708 0l-3.5-3.5a.5.5 0 11.708-.708L6.5 10.793l6.646-6.647a.5.5 0 01.708 0z" fill="currentColor"/>
              </svg>
              <span>Tối thiểu 1 chữ hoa, chữ thường và một số</span>
            </div>
          </div>

          <div class="modal-actions">
            <button type="submit" class="btn-save" :disabled="passwordLoading || !isPasswordFormValid">
              {{ passwordLoading ? 'Đang lưu...' : 'Lưu lại' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Address Modal -->
    <ListAddressModal
        v-if="showAddressModal"
        :userId="formData.id"
        @close="showAddressModal = false"
        @save="handleSaveAddress"
    />

    <!-- Toast Component -->
    <ShowToastComponent ref="toastRef"/>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getProfile, updateProfile, updateUserAvatar, changePassword } from '@/service/UserApi.js';
import ShowToastComponent from '@/components/ShowToastComponent.vue';
import ListAddressModal from '@/components/ListAddressModal.vue';
import {getSelectedAddress} from '@/service/AddressApi.js';

// State using ref
const loading = ref(false);
const avatarUploading = ref(false);
const passwordLoading = ref(false);
const showPasswordModal = ref(false);
const showOldPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);
const fileInput = ref(null);
const toastRef = ref(null);
const showAddressModal = ref(false);

const formData = ref({
  id: '',
  role_id: '',
  avatar: '',
  fullname: '',
  gender: '',
  email: '',
  phoneNumber: '',
  address: '',
  password: '',
  dateOfBirth: '',
  is_active: true
});

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const errors = ref({});
const passwordErrors = ref({});
const originalData = ref({});

// Computed properties
const passwordValidation = computed(() => {
  const password = passwordForm.value.newPassword;
  return {
    minLength: password.length >= 6,
    hasRequiredChars: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)
  };
});

const isPasswordFormValid = computed(() => {
  return passwordValidation.value.minLength &&
      passwordValidation.value.hasRequiredChars &&
      passwordForm.value.oldPassword &&
      passwordForm.value.newPassword &&
      passwordForm.value.confirmPassword &&
      passwordForm.value.newPassword === passwordForm.value.confirmPassword;
});

// Router
const router = useRouter();

// Methods
const showToast = (message, type) => {
  toastRef.value?.showToast(message, type || "success");
};

const fetchProfile = async () => {
  try {
    loading.value = true;
    const response = await getProfile();
    formData.value = { ...response.data.data };
    originalData.value = { ...response.data.data };

    // Lấy địa chỉ mặc định nếu có ID người dùng
    if (formData.value.id) {
      try {
        const addressResponse = await getSelectedAddress(formData.value.id);
        if (addressResponse.data && addressResponse.data.data) {
          const selectedAddress = addressResponse.data.data;
          formData.value.address = selectedAddress.line ||
              `${selectedAddress.addressDetail}, ${selectedAddress.wardName}, ${selectedAddress.districtName}, ${selectedAddress.provinceName}`;
          originalData.value.address = formData.value.address;
        }
      } catch (addressError) {
        console.error('Error fetching selected address:', addressError);
        // Không hiển thị thông báo lỗi cho người dùng vì đây chỉ là thông tin bổ sung
      }
    }
  } catch (error) {
    console.error('Error fetching profile:', error);
    showToast('Failed to load profile data', 'error');
  } finally {
    loading.value = false;
  }
};

const saveProfile = async () => {
  try {
    loading.value = true;
    const updateData = {
      ...formData.value,
      updated_at: new Date().toISOString()
    };

    const emailChanged = formData.value.email !== originalData.value.email;
    const response = await updateProfile(updateData);
    formData.value = { ...response.data.data };
    originalData.value = { ...response.data.data };

    if (emailChanged) {
      showToast('Cập nhật thông tin và email thành công! Vui lòng đăng nhập lại.', 'success');
      setTimeout(() => {
        redirectToLogin();
      }, 3000);
    } else {
      showToast('Cập nhật thông tin cá nhân thành công!', 'success');
    }
  } catch (error) {
    console.error('Error updating profile:', error);
    showToast('Failed to update profile', 'error');
  } finally {
    loading.value = false;
  }
};

const updatePassword = async () => {
  // Clear previous errors
  passwordErrors.value = {};

  if (!validatePasswordForm()) {
    return;
  }

  try {
    passwordLoading.value = true;
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
      confirmPassword: passwordForm.value.confirmPassword
    });

    showToast('Đổi mật khẩu thành công! Vui lòng đăng nhập lại.', 'success');
    closePasswordModal();

    setTimeout(() => {
      redirectToLogin();
    }, 2000);
  } catch (error) {
    showToast('Đổi mật khẩu thất bại! Vui lòng thử lại.', 'error');
    if (error.response?.data) {
      const errorData = error.response.data;

      // Check for specific error messages from backend
      if (errorData.message) {
        const message = errorData.message.toLowerCase();

        if (message.includes('old password') || message.includes('mật khẩu cũ') || message.includes('incorrect') || message.includes('wrong')) {
          passwordErrors.value.oldPassword = 'Mật khẩu hiện tại không chính xác! Vui lòng kiểm tra lại';
        } else if (message.includes('same') || message.includes('trùng')) {
          passwordErrors.value.newPassword = 'Mật khẩu mới phải khác mật khẩu cũ';
        }else if(message.includes("khác")){
          passwordErrors.value.newPassword = 'Mật khẩu mới không đươc trùng với mật khẩu hiện tại';
        } else {
          // Generic error - show under old password field
          passwordErrors.value.oldPassword = errorData.message;
        }
      } else if (errorData.errors) {
        // Handle validation errors from backend
        Object.keys(errorData.errors).forEach(field => {
          if (field.includes('old') || field.includes('current')) {
            passwordErrors.value.oldPassword = errorData.errors[field][0] || errorData.errors[field];
          } else if (field.includes('new')) {
            passwordErrors.value.newPassword = errorData.errors[field][0] || errorData.errors[field];
          } else if (field.includes('confirm')) {
            passwordErrors.value.confirmPassword = errorData.errors[field][0] || errorData.errors[field];
          }
        });
      }
    } else {
      // Network or other errors
      passwordErrors.value.oldPassword = 'Có lỗi xảy ra, vui lòng thử lại';
    }
  } finally {
    passwordLoading.value = false;
  }
};

const redirectToLogin = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  router.push('/login');
};

const validatePasswordForm = () => {
  let hasErrors = false;

  // Validate old password
  if (!passwordForm.value.oldPassword) {
    passwordErrors.value.oldPassword = 'Vui lòng nhập mật khẩu hiện tại';
    hasErrors = true;
  } else if (passwordForm.value.oldPassword.length < 6) {
    passwordErrors.value.oldPassword = 'Mật khẩu hiện tại phải có ít nhất 6 ký tự';
    hasErrors = true;
  }

  // Validate new password
  if (!passwordForm.value.newPassword) {
    passwordErrors.value.newPassword = 'Vui lòng nhập mật khẩu mới';
    hasErrors = true;
  } else if (!passwordValidation.value.minLength) {
    passwordErrors.value.newPassword = 'Mật khẩu phải có ít nhất 6 ký tự';
    hasErrors = true;
  } else if (!passwordValidation.value.hasRequiredChars) {
    passwordErrors.value.newPassword = 'Mật khẩu phải chứa chữ hoa, chữ thường và ít nhất 1 số';
    hasErrors = true;
  } else if (passwordForm.value.newPassword === passwordForm.value.oldPassword) {
    passwordErrors.value.newPassword = 'Mật khẩu mới phải khác mật khẩu hiện tại';
    hasErrors = true;
  }

  // Validate confirm password
  if (!passwordForm.value.confirmPassword) {
    passwordErrors.value.confirmPassword = 'Vui lòng xác nhận mật khẩu mới';
    hasErrors = true;
  } else if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordErrors.value.confirmPassword = 'Mật khẩu xác nhận không khớp';
    hasErrors = true;
  }

  return !hasErrors;
};

const closePasswordModal = () => {
  showPasswordModal.value = false;
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
  passwordErrors.value = {};
  showOldPassword.value = false;
  showNewPassword.value = false;
  showConfirmPassword.value = false;
};

const clearPasswordError = (field) => {
  const newErrors = {...passwordErrors.value};
  delete newErrors[field];
  passwordErrors.value = newErrors;
};

const uploadAvatarFile = async (file) => {
  // Validate file type (check extension and MIME type)
  const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp'];
  const fileName = file.name.toLowerCase();
  const fileExtension = fileName.split('.').pop();

  if (!allowedExtensions.includes(fileExtension)) {
    showToast('Chỉ được phép tải lên file ảnh (JPG, PNG, GIF, WEBP)', 'error');
    return;
  }

  if (!file.type.startsWith('image/')) {
    showToast('File phải là ảnh hợp lệ', 'error');
    return;
  }

  if (file.size > 5 * 1024 * 1024) {
    showToast('File không được vượt quá 5MB', 'error');
    return;
  }

  try {
    avatarUploading.value = true;
    const response = await updateUserAvatar(formData.value.id, file);

    if (response.success) {
      formData.value.avatar = response.avatarUrl;
      originalData.value.avatar = formData.value.avatar;
      await fetchProfile();
      showToast('Upload ảnh đại diện thành công!', 'success');
    } else {
      showToast(response.error || 'Upload thất bại', 'error');
    }
  } catch (error) {
    console.error('Avatar upload error:', error);
    showToast('Có lỗi xảy ra khi upload ảnh', 'error');
  } finally {
    avatarUploading.value = false;
  }
};

const uploadAvatar = () => {
  fileInput.value.click();
};

const handleFileSelect = (event) => {
  const file = event.target.files[0];
  if (file) {
    uploadAvatarFile(file);
  }
  event.target.value = '';
};

const handleDrop = (e) => {
  e.preventDefault();
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    uploadAvatarFile(files[0]);
  }
};
const validateFullname = () => {
  const fullname = formData.value.fullname?.trim();

  if (!fullname) {
    errors.value.fullname = 'Họ và tên không được để trống';
  } else if (fullname.length < 2) {
    errors.value.fullname = 'Họ và tên phải có từ 2-100 ký tự';
  } else if (fullname.length > 100) {
    errors.value.fullname = 'Họ và tên phải có từ 2-100 ký tự';
  } else if (!/^[a-zA-ZÀ-ỹ\s]+$/.test(fullname)) {
    errors.value.fullname = 'Họ và tên chỉ được chứa chữ cái và khoảng trắng';
  } else {
    const newErrors = { ...errors.value };
    delete newErrors.fullname;
    errors.value = newErrors;
  }
};

const validateEmail = () => {
  const email = formData.value.email?.trim();
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!email) {
    errors.value.email = 'Email không được để trống';
  } else if (!emailRegex.test(email)) {
    errors.value.email = 'Email không đúng định dạng';
  } else if (email.length > 100) {
    errors.value.email = 'Email không được vượt quá 100 ký tự';
  } else {
    const newErrors = {...errors.value};
    delete newErrors.email;
    errors.value = newErrors;
  }
};

const validatePhone = () => {
  const phone = formData.value.phoneNumber?.trim();

  if (!phone) {
    errors.value.phoneNumber = 'Số điện thoại không được để trống';
  } else if (!/^0\d{9}$/.test(phone)) {
    errors.value.phoneNumber = 'Số điện thoại phải có 10 chữ số và bắt đầu bằng số 0';
  } else {
    const newErrors = {...errors.value};
    delete newErrors.phoneNumber;
    errors.value = newErrors;
  }
};

const validateDateOfBirth = () => {
  const value = formData.value.dateOfBirth;
  const birthDate = new Date(value);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  if (birthDate >= today) {
    errors.value.dateOfBirth = 'Ngày sinh phải là ngày trong quá khứ';
  } else {
    const newErrors = { ...errors.value };
    delete newErrors.dateOfBirth;
    errors.value = newErrors;
  }
};

const validateAddress = () => {
  const address = formData.value.address?.trim();

  if (!address) {
    errors.value.address = 'Địa chỉ không được để trống';
  } else if (address.length > 255){
    errors.value.address = 'Địa chỉ không được vượt quá 255 ký tự';
  } else {
    const newErrors = {...errors.value};
    delete newErrors.address;
    errors.value = newErrors;
  }
};

const validateAll = () => {
  validateFullname();
  validateEmail();
  validatePhone();
  validateDateOfBirth();
  validateAddress();

  return Object.keys(errors.value).length === 0;
};

const clearError = (field) => {
  const newErrors = {...errors.value};
  delete newErrors[field];
  errors.value = newErrors;
};

const formatPhoneNumber = (event) => {
  let value = event.target.value.replace(/\D/g, '');
  if (value.length > 10) {
    value = value.substring(0, 10);
  }
  formData.value.phoneNumber = value;
  clearError('phoneNumber');
};

const saveForm = async () => {
  console.log('Save form called');
  if (validateAll()) {
    console.log('Validation passed, calling saveProfile');
    await saveProfile();
  } else {
    console.log('Validation failed:', errors.value);
  }
};

const resetForm = () => {
  formData.value = {...originalData.value};
  errors.value = {};
  showToast('Thông tin form đã được khôi phục', 'success');
};

// Lifecycle
onMounted(() => {
  fetchProfile();
});

const handleSaveAddress = (addressObj) => {
  // Cập nhật địa chỉ từ ListAddressModal
  if (addressObj && addressObj.data) {
    const address = addressObj.data;
    // Kiểm tra và đảm bảo các trường dữ liệu tồn tại trước khi sử dụng
    if (address.line) {
      formData.value.address = address.line;
    } else if (address.addressDetail && address.wardName && address.districtName && address.provinceName) {
      formData.value.address = `${address.addressDetail}, ${address.wardName}, ${address.districtName}, ${address.provinceName}`;
    } else {
      console.log('Dữ liệu địa chỉ không đầy đủ:', address);
      // Nếu không có đủ thông tin, giữ nguyên địa chỉ hiện tại
    }
  }
  showAddressModal.value = false;
}
</script>

<style scoped>
/* Profile Container & Layout */
.profile-container {
  min-height: 90vh;
  margin-top: 20px;
  padding: 20px;
}

.profile-card {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.profile-layout {
  display: flex;
  min-height: 600px;
}

.sidebar {
  width: 300px;
  background: white;
  border-right: 1px solid #e2e8f0;
  padding: 40px 30px;
  display: flex;
  flex-direction: column;
}

.profile-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.photo-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 20px;
}

.current-photo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid #e2e8f0;
  transition: all 0.3s ease;
}

.avatar:hover {
  border-color: #3b82f6;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-info {
  text-align: center;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

/* Upload Section */
.upload-section {
  margin-top: 24px;
}

.upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafafa;
}

.upload-area:hover {
  border-color: #202020;
  background: #f8fafc;
}

.upload-area.uploading {
  pointer-events: none;
  opacity: 0.7;
}

.upload-icon {
  margin-bottom: 12px;
  color: #1F2937;
  display: flex;
  justify-content: center;
}

.upload-text {
  font-size: 14px;
  color: #1F2937;
  margin-bottom: 4px;
}

.upload-link {
  color: #1F2937;
  font-weight: 500;
}

.upload-info {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

/* Form Section */
.form-section {
  flex: 1;
  padding: 40px;
  background: #fafafa;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 32px;
}

.profile-form {
  max-width: 800px;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-group-full {
  flex: 1 1 100%;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.form-input, .form-select, .form-textarea {
  padding: 14px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: white;
}

.form-input:focus, .form-select:focus, .form-textarea:focus {
  outline: none;
  border-color: #1F2937;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder, .form-textarea::placeholder {
  color: #9ca3af;
}

.form-input.error, .form-select.error, .form-textarea.error {
  border-color: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.error-message {
  color: #ef4444;
  font-size: 12px;
  margin-top: 6px;
  font-weight: 500;
  display: block;
}

/* Address Button */
.btn-address {
  background: #1F2937;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 8px;
}

.btn-address:hover {
  background: #252e3e;
}

/* Form Actions */
.form-actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}

.btn-reset, .btn-save {
  padding: 10px 18px;
  border-radius: 35px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.btn-reset {
  background: white;
  color: #6b7280;
  border: 2px solid #e2e8f0;
}

.btn-reset:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.btn-save {
  background: #1F2937;
  color: white;
}

.btn-save:hover {
  background: #2a3543;
  transform: translateY(-1px);
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* Responsive Design */
@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e2e8f0;
  }

  .form-section {
    padding: 30px 20px;
  }

  .form-row {
    flex-direction: column;
    gap: 16px;
  }

  .form-actions {
    flex-direction: column;
  }

  .btn-reset, .btn-save {
    width: 100%;
  }
}

.password-field {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input {
  flex: 1;
  padding-right: 60px;
}

.edit-password-btn {
  position: absolute;
  right: 12px;
  background: #1F2937;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.edit-password-btn:hover {
  background: #252e3e;
}

/* Avatar Loading */
.avatar-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f4f6;
  border-top: 2px solid #1F2937;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 0;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0 24px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 24px;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-form {
  padding: 0 24px 24px 24px;
}

.modal-form .form-group {
  margin-bottom: 20px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input-field {
  width: 100%;
  padding-right: 48px;
}

.password-toggle {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #9ca3af;
  transition: color 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.password-toggle:hover {
  color: #d1d5db;
}

.password-toggle svg {
  width: 20px;
  height: 20px;
}

/* Password Requirements */
.password-requirements {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  margin: 16px 0;
}

.requirements-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 12px 0;
}

.requirement-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #6b7280;
}

.requirement-item:last-child {
  margin-bottom: 0;
}

.requirement-item.valid {
  color: #059669;
}

.requirement-item svg {
  flex-shrink: 0;
}
</style>