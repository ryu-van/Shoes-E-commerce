<template>
  <div class="login-page min-vh-100 bg-light d-flex align-items-center justify-content-center p-4"
       style="background-color: #f8f9fa;">
    <div class="bg-white rounded-4 shadow-lg" style="max-width: 1200px; width: 100%;">
      <div class="row g-0">
        <!-- Left Side - Login Form -->
        <div class="col-12 col-lg-6 p-4 p-lg-5">
          <!-- Logo -->
          <div class="d-flex align-items-center mb-4">
            <div class="bg-dark rounded-circle d-flex align-items-center justify-content-center me-3"
                 style="width: 40px; height: 40px;">
              <span class="text-white fw-bold">S</span>
            </div>
            <span class="h5 mb-0 fw-bold text-dark">Shoozy Shop</span>
          </div>

          <!-- Login Form -->
          <div>
            <h1 class="title h2 text-dark mb-2">Chào mừng quay trở lại!</h1>
            <p class="text-muted mb-4">Vui lòng nhập thông tin để đăng nhập</p>

            <!-- Thông báo lỗi chung -->
            <div v-if="errors.general" class="alert alert-danger alert-custom mb-3">
              <div class="d-flex align-items-center">
                <i class="fas fa-exclamation-circle me-2"></i>
                <span>{{ errors.general }}</span>
              </div>
            </div>

            <!-- Thông báo thành công -->
            <div v-if="success" class="alert alert-success alert-custom mb-3">
              <div class="d-flex align-items-center">
                <i class="fas fa-check-circle me-2"></i>
                <span>{{ success }}</span>
              </div>
            </div>

            <form @submit.prevent="handleLogin" novalidate>
              <!-- Email Field -->
              <div class="mb-3">
                <label for="email" class="form-label fw-medium text-dark">Email</label>
                <input
                    id="email"
                    v-model="form.email"
                    type="email"
                    class="form-control form-control-lg custom-input"
                    :class="{ 'has-error': errors.email, 'has-content': form.email }"
                    placeholder="john@example.com"
                    @focus="clearFieldError('email')"
                    @input="clearFieldError('email')"
                    autocomplete="email"
                />
                <div v-if="errors.email" class="error-message">
                  {{ errors.email }}
                </div>
              </div>

              <!-- Password Field -->
              <div class="mb-3">
                <label for="password" class="form-label fw-medium text-dark">Mật khẩu</label>
                <div class="input-wrapper position-relative">
                  <input
                      id="password"
                      v-model="form.password"
                      :type="showPassword ? 'text' : 'password'"
                      class="form-control form-control-lg pe-5 custom-input"
                      :class="{ 'has-error': errors.password, 'has-content': form.password }"
                      placeholder="Nhập mật khẩu của bạn..."
                      @focus="clearFieldError('password')"
                      @input="clearFieldError('password')"
                      autocomplete="current-password"
                  />
                  <button
                      type="button"
                      @click="togglePassword"
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

              <!-- Forgot Password -->
              <div class="d-flex justify-content-end mb-4">
                <router-link to="/forgot-password" class="text-dark text-decoration-none small fw-medium">
                  Quên mật khẩu?
                </router-link>
              </div>

              <!-- Login Button -->
              <button
                  type="submit"
                  class="btn btn-dark btn-lg w-100 mb-4 rounded-pill fw-medium login-btn"
                  style="height: 56px;"
                  :disabled="loading"
              >
                <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"
                      aria-hidden="true"></span>
                {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
              </button>

              <!-- Sign Up Link -->
              <div class="text-center mb-4">
                <span class="text-muted">Chưa có tài khoản? </span>
                <router-link to="/register" class="text-dark text-decoration-none fw-medium">Đăng ký</router-link>
              </div>
            </form>
          </div>
        </div>

        <!-- Right Side - Image -->
        <div class="col-lg-6 d-none d-lg-flex align-items-center justify-content-center p-5">
          <div class="login-image-box">
            <img src="@/assets/img/loginn.jpg" alt="Login Illustration" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/Auth.js';

export default {
  name: 'Login',
  setup() {
    const router = useRouter();
    const authStore = useAuthStore();
    const form = ref({
      email: '',
      password: '',
    });
    const showPassword = ref(false);
    const errors = ref({
      email: '',
      password: '',
      general: ''
    });
    const success = ref('');
    const loading = ref(false);

    const togglePassword = () => {
      showPassword.value = !showPassword.value;
    };

    const clearErrors = () => {
      errors.value = {
        email: '',
        password: '',
        general: ''
      };
    };

    const clearFieldError = (field) => {
      if (errors.value[field]) {
        errors.value[field] = '';
      }
    };

    const validateEmail = () => {
      if (!form.value.email) {
        errors.value.email = 'Vui lòng nhập email';
        return false;
      } else if (!/\S+@\S+\.\S+/.test(form.value.email)) {
        errors.value.email = 'Email không hợp lệ';
        return false;
      } else {
        errors.value.email = '';
        return true;
      }
    };

    const validatePassword = () => {
      if (!form.value.password) {
        errors.value.password = 'Vui lòng nhập mật khẩu';
        return false;
      } else if (form.value.password.length < 6) {
        errors.value.password = 'Mật khẩu phải có ít nhất 6 ký tự';
        return false;
      } else {
        errors.value.password = '';
        return true;
      }
    };

    const validateForm = () => {
      clearErrors();
      const isEmailValid = validateEmail();
      const isPasswordValid = validatePassword();
      return isEmailValid && isPasswordValid;
    };

    const handleLogin = async (event) => {
      event.preventDefault();
      event.stopPropagation();

      if (loading.value) {
        return;
      }

      if (!validateForm()) {
        return;
      }

      try {
        loading.value = true;
        clearErrors();
        success.value = '';

        await authStore.login(form.value.email, form.value.password);
        success.value = 'Đăng nhập thành công! Đang chuyển hướng...';

        setTimeout(() => {
          const role = authStore.getUserRole;
          if (role === 'Admin' || role === 'Staff') {
            router.push('/admin');
          } else {
            router.push('/');
          }
        }, 1000);
      } catch (err) {
  let errorMessage = 'Email hoặc mật khẩu không đúng! Vui lòng kiểm tra lại.'
  const code = err?.response?.data?.code
  const msg  = err?.response?.data?.message

  if (code === 'ACCOUNT_LOCKED') {
    errorMessage = msg || 'Tài khoản đã bị khoá'
  } else if (msg) {
    errorMessage = msg
  } else if (err?.message) {
    errorMessage = err.message
  } else if (typeof err === 'string') {
    errorMessage = err
  }

  errors.value.general = errorMessage
} finally {
        loading.value = false;
      }
    };

    const socialLogin = (provider) => {
      console.log(`Đăng nhập bằng ${provider}`);
    };
     onMounted(() => {
      const reason = sessionStorage.getItem('logoutReason')
      const message = sessionStorage.getItem('logoutMessage')
      if (reason) {
        errors.value.general = message || 'Phiên đăng nhập đã kết thúc'
        // clear ngay để refresh lại trang không hiển thị lặp lại
        sessionStorage.removeItem('logoutReason')
        sessionStorage.removeItem('logoutMessage')
      }
    })

    return {
      form,
      showPassword,
      togglePassword,
      handleLogin,
      errors,
      success,
      loading,
      clearFieldError,
      validateEmail,
      validatePassword,
    };
  },
};
</script>

<style scoped>
.login-page {
  max-width: 100%;
}

.title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 32px !important;
}

/* Enhanced Input Styling */
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

/* Password Toggle */
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

/* Button Styling */
.login-btn {
  background-color: #212529;
  border-color: #212529;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  font-weight: 600;
  font-size: 16px;
}

.login-btn:hover:not(:disabled) {
  background-color: #1c1f23;
  border-color: #1a1e21;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(33, 37, 41, 0.3);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  background-color: #6c757d;
  border-color: #6c757d;
  cursor: not-allowed;
  transform: none;
}

.login-btn .spinner-border-sm {
  width: 1rem;
  height: 1rem;
}

/* Updated Image Container - Larger Image */
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
  width: 140%;
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

@media (max-width: 768px) {
  .login-page {
    padding: 1rem;
  }

  .col-12 {
    padding: 2rem 1.5rem;
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