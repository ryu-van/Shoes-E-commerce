<template>
  <div class="reset-password-page min-vh-100 bg-light d-flex align-items-center justify-content-center p-4"
       style="background-color: #f8f9fa;">
    <div class="bg-white rounded-4 shadow-lg" style="max-width: 1200px; width: 100%;">
      <div class="row g-0">
        <!-- Left Side - Reset Password Form -->
        <div class="col-12 col-lg-6 p-4 p-lg-5">
          <!-- Back to Login -->
          <div class="mb-4">
            <button @click="goBack" class="btn btn-link text-dark text-decoration-none p-0 d-flex align-items-center">
              <i class="fas fa-arrow-left me-2"></i>
              <span class="fw-medium">Quay trở lại đăng nhập</span>
            </button>
          </div>

          <!-- Logo -->
          <div class="d-flex align-items-center mb-4">
            <div class="bg-dark rounded-circle d-flex align-items-center justify-content-center me-3"
                 style="width: 40px; height: 40px;">
              <span class="text-white fw-bold">S</span>
            </div>
            <span class="h5 mb-0 fw-bold text-dark">Shoozy Shop</span>
          </div>

          <!-- Reset Password Form -->
          <div>
            <h1 class="title h2 text-dark mb-2">Đặt lại mật khẩu</h1>
            <p class="text-muted mb-4">Mật khẩu trước đây của bạn đã được đặt lại. Vui lòng đặt mật khẩu mới cho tài khoản của bạn.</p>

            <!-- Success Message -->
            <div v-if="success" class="alert alert-success alert-custom mb-3">
              <div class="d-flex align-items-center">
                <i class="fas fa-check-circle me-2"></i>
                <span>{{ success }}</span>
              </div>
            </div>

            <!-- Error Message -->
            <div v-if="errors.general" class="alert alert-danger alert-custom mb-3">
              <div class="d-flex align-items-center">
                <i class="fas fa-exclamation-circle me-2"></i>
                <span>{{ errors.general }}</span>
              </div>
            </div>

            <form @submit.prevent="handleSubmit" novalidate>
              <!-- Password Field -->
              <div class="mb-3">
                <label for="password" class="form-label fw-medium text-dark">Tạo mật khẩu</label>
                <div class="input-wrapper position-relative">
                  <input
                      id="password"
                      v-model="form.password"
                      :type="showPassword ? 'text' : 'password'"
                      class="form-control form-control-lg pe-5 custom-input"
                      :class="{ 'has-error': errors.password, 'has-content': form.password }"
                      placeholder="Nhập mật khẩu mới"
                      @focus="clearFieldError('password')"
                      @input="clearFieldError('password')"
                      autocomplete="new-password"
                  />
                  <button
                      type="button"
                      @click="togglePasswordVisibility"
                      class="btn position-absolute end-0 top-50 translate-middle-y password-toggle"
                      style="border: none; background: none; z-index: 10;"
                      tabindex="-1"
                  >
                    <i :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                  </button>
                </div>
                <div v-if="errors.password" class="error-message">
                  {{ errors.password }}
                </div>
              </div>

              <!-- Confirm Password Field -->
              <div class="mb-4">
                <label for="confirmPassword" class="form-label fw-medium text-dark">Nhập lại mật khẩu</label>
                <div class="input-wrapper position-relative">
                  <input
                      id="confirmPassword"
                      v-model="form.confirmPassword"
                      :type="showConfirmPassword ? 'text' : 'password'"
                      class="form-control form-control-lg pe-5 custom-input"
                      :class="{ 'has-error': errors.confirmPassword, 'has-content': form.confirmPassword }"
                      placeholder="Xác nhận mật khẩu mới"
                      @focus="clearFieldError('confirmPassword')"
                      @input="clearFieldError('confirmPassword')"
                      autocomplete="new-password"
                  />
                  <button
                      type="button"
                      @click="toggleConfirmPasswordVisibility"
                      class="btn position-absolute end-0 top-50 translate-middle-y password-toggle"
                      style="border: none; background: none; z-index: 10;"
                      tabindex="-1"
                  >
                    <i :class="showConfirmPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                  </button>
                </div>
                <div v-if="errors.confirmPassword" class="error-message">
                  {{ errors.confirmPassword }}
                </div>
              </div>

              <!-- Submit Button -->
              <button
                  type="submit"
                  class="btn btn-dark btn-lg w-100 mb-4 rounded-pill fw-medium submit-btn"
                  style="height: 56px;"
                  :disabled="loading"
              >
                <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"
                      aria-hidden="true"></span>
                {{ loading ? 'Đang xử lý...' : 'Đặt lại mật khẩu' }}
              </button>
            </form>
          </div>
        </div>

        <!-- Right Side - Illustration -->
        <div class="col-lg-6 d-none d-lg-flex align-items-center justify-content-center p-5">
          <div class="login-image-box">
            <img src="@/assets/img/reset-pass.jpg" alt="Forgot Password Illustration" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/Auth.js';

export default {
  name: 'ResetPassword',
  setup() {
    const router = useRouter();
    const authStore = useAuthStore();

    const form = ref({
      password: '',
      confirmPassword: ''
    });

    const errors = ref({
      password: '',
      confirmPassword: '',
      general: ''
    });

    const success = ref('');
    const loading = ref(false);
    const showPassword = ref(false);
    const showConfirmPassword = ref(false);

    const goBack = () => {
      router.push('/login');
    };

    const goToLogin = () => {
      router.push('/login');
    };

    const togglePasswordVisibility = () => {
      showPassword.value = !showPassword.value;
    };

    const toggleConfirmPasswordVisibility = () => {
      showConfirmPassword.value = !showConfirmPassword.value;
    };

    const clearFieldError = (field) => {
      if (errors.value[field]) {
        errors.value[field] = '';
      }
    };

    const clearErrors = () => {
      errors.value = {
        password: '',
        confirmPassword: '',
        general: ''
      };
    };

    const validateForm = () => {
      let isValid = true;
      clearErrors();

      // Regex: ít nhất 6 ký tự, 1 chữ hoa, 1 chữ thường, 1 số
      const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/;

      // Validate password
      if (!form.value.password) {
        errors.value.password = 'Vui lòng nhập mật khẩu mới';
        isValid = false;
      } else if (!passwordPattern.test(form.value.password)) {
        errors.value.password =
            'Mật khẩu phải có ít nhất 6 ký tự, chứa chữ hoa, chữ thường và một chữ số';
        isValid = false;
      }

      // Validate confirm password
      if (!form.value.confirmPassword) {
        errors.value.confirmPassword = 'Vui lòng xác nhận mật khẩu';
        isValid = false;
      } else if (form.value.password !== form.value.confirmPassword) {
        errors.value.confirmPassword = 'Mật khẩu xác nhận không khớp';
        isValid = false;
      }

      return isValid;
    };

    const handleSubmit = async (event) => {
      event.preventDefault();
      event.stopPropagation();

      if (loading.value) return;

      if (!validateForm()) return;

      try {
        loading.value = true;
        clearErrors();
        success.value = '';

        // Get token from URL params
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');

        if (!token) {
          errors.value.general = 'Token không hợp lệ hoặc đã hết hạn';
          return;
        }

        await authStore.resetPassword(token, form.value.password);

        success.value = 'Mật khẩu đã được đặt lại thành công. Bạn sẽ được chuyển hướng đến trang đăng nhập.';

        setTimeout(() => {
          router.push('/login');
        }, 2000);

      } catch (err) {
        console.error('Reset password error:', err);
        errors.value.general = err.response?.data.data?.message || 'Đã xảy ra lỗi, vui lòng thử lại sau.';
      } finally {
        loading.value = false;
      }
    };

    return {
      form,
      errors,
      success,
      loading,
      showPassword,
      showConfirmPassword,
      goBack,
      goToLogin,
      togglePasswordVisibility,
      toggleConfirmPasswordVisibility,
      clearFieldError,
      handleSubmit
    };
  }
};
</script>

<style scoped>
.reset-password-page {
  max-width: 100%;
}

.title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 32px !important;
}

/* Enhanced Input Styling - Matching Login Page */
.custom-input {
  border: 2px solid #e9ecef;
  border-radius: 16px;
  background-color: #fafbfc;
  padding: 16px 18px;
  height: 56px;
  font-size: 15px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.custom-input::placeholder {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 400;
  transition: all 0.2s ease;
}

.custom-input:focus {
  border-color: #343a40;
  background-color: #ffffff;
  box-shadow: 0 0 0 4px rgba(52, 58, 64, 0.1);
  outline: none;
}

.custom-input:focus::placeholder {
  font-size: 12px;
  color: #6b7280;
}

.custom-input.has-content {
  background-color: #ffffff;
}

.custom-input.has-content::placeholder {
  font-size: 12px;
  color: #6b7280;
}

/* Error State */
.custom-input.has-error {
  border-color: #ef4444;
  background-color: #fef2f2;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.1);
}

.custom-input.has-error:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 4px rgba(220, 38, 38, 0.15);
}

.custom-input.has-error::placeholder {
  color: #f87171;
}

/* Password Toggle - Matching Login Page */
.password-toggle {
  color: #6b7280 !important;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.password-toggle:hover {
  color: #374151 !important;
  background-color: rgba(0, 0, 0, 0.05);
}

/* Error Message */
.error-message {
  color: #ef4444;
  font-size: 0.875rem;
  margin-top: 0.5rem;
  font-weight: 500;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Alert Styling */
.alert-custom {
  border: none;
  border-radius: 12px;
  padding: 16px 20px;
  font-size: 0.875rem;
  animation: slideDown 0.3s ease-out;
  font-weight: 500;
}

.alert-danger {
  background-color: #fef2f2;
  color: #dc2626;
  border-left: 4px solid #ef4444;
}

.alert-success {
  background-color: #f0fdf4;
  color: #15803d;
  border-left: 4px solid #22c55e;
}

/* Button Styling - Matching Login Page */
.submit-btn {
  background-color: #212529;
  border-color: #212529;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  font-weight: 600;
  font-size: 16px;
}

.submit-btn:hover:not(:disabled) {
  background-color: #1c1f23;
  border-color: #1a1e21;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(33, 37, 41, 0.3);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  background-color: #6c757d;
  border-color: #6c757d;
  cursor: not-allowed;
  transform: none;
}

.submit-btn .spinner-border-sm {
  width: 1rem;
  height: 1rem;
}

.login-image-box {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #F4F4F4;
  border-radius: 20px;
}

.login-image-box img {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 130%;
  height: auto;
  transform: translate(-50%, -50%);
  object-fit: contain;
}

/* Border radius utilities */
.rounded-4 {
  border-radius: 1rem;
}

.rounded-3 {
  border-radius: 0.75rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  .reset-password-page {
    padding: 1rem;
  }

  .col-12 {
    padding: 2rem 1.5rem;
  }

  .title {
    font-size: 28px !important;
  }

  .custom-input {
    height: 52px;
    padding: 14px 16px;
    font-size: 14px;
  }

  .custom-input::placeholder {
    font-size: 12px;
  }
}
</style>