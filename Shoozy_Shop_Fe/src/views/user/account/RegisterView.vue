<template>
  <div class="register-page min-vh-100 bg-light d-flex align-items-center justify-content-center p-4"
       style="background-color: #f8f9fa;">
    <div class="bg-white rounded-4 shadow-lg" style="max-width: 1200px; width: 100%;">
      <div class="row g-0">
        <!-- Left Side - Illustration -->
        <div class="col-lg-6 d-none d-lg-flex align-items-center justify-content-center p-5">
          <div class="login-image-box">
            <img src="@/assets/img/register.jpg" alt="Forgot Password Illustration" />
          </div>
        </div>

        <!-- Right Side - Register Form -->
        <div class="col-12 col-lg-6 p-4 p-lg-5">
          <!-- Logo -->
          <div class="d-flex align-items-center mb-3">
            <div class="bg-dark rounded-circle d-flex align-items-center justify-content-center me-3"
                 style="width: 40px; height: 40px;">
              <span class="text-white fw-bold">S</span>
            </div>
            <span class="h5 mb-0 fw-bold text-dark">Shoozy Shop</span>
          </div>

          <!-- Register Form -->
          <div>
            <h1 class="title h2 text-center text-dark">Đăng ký tài khoản</h1>

            <!-- Thông báo lỗi chung -->
            <div v-if="errors.general" class="alert alert-danger alert-custom mb-3">
              {{ errors.general }}
            </div>

            <!-- Thông báo thành công -->
            <div v-if="success" class="alert alert-success alert-custom mb-3">
              {{ success }}
            </div>

            <form @submit.prevent="handleRegister" novalidate>
              <!-- Row 1: Full Name and Email -->
              <div class="row g-3 mb-3">
                <!-- Full Name Field -->
                <div class="col-md-6">
                  <label for="fullname" class="form-label fw-medium text-dark">Họ và tên</label>
                  <input
                      id="fullname"
                      v-model="form.fullname"
                      type="text"
                      class="form-control form-control-lg custom-input"
                      :class="{ 'has-error': errors.fullname, 'has-content': form.fullname }"
                      placeholder="Nguyễn Văn A"
                      @focus="clearFieldError('fullname')"
                      @input="clearFieldError('fullname')"
                      autocomplete="name"
                  />
                  <div v-if="errors.fullname" class="error-message">
                    {{ errors.fullname }}
                  </div>
                </div>

                <!-- Email Field -->
                <div class="col-md-6">
                  <label for="email" class="form-label fw-medium text-dark">Email</label>
                  <input
                      id="email"
                      v-model="form.email"
                      type="email"
                      class="form-control form-control-lg custom-input"
                      :class="{ 'has-error': errors.email, 'has-content': form.email }"
                      placeholder="a123@example.com"
                      @focus="clearFieldError('email')"
                      @input="clearFieldError('email')"
                      autocomplete="email"
                  />
                  <div v-if="errors.email" class="error-message">
                    {{ errors.email }}
                  </div>
                </div>
              </div>

              <!-- Row 2: Phone Number and Password -->
              <div class="row g-3 mb-3">
                <!-- Phone Number Field -->
                <div class="col-md-6">
                  <label for="phoneNumber" class="form-label fw-medium text-dark">Số điện thoại</label>
                  <input
                      id="phoneNumber"
                      v-model="form.phoneNumber"
                      type="tel"
                      class="form-control form-control-lg custom-input"
                      :class="{ 'has-error': errors.phoneNumber, 'has-content': form.phoneNumber }"
                      placeholder="0123456789"
                      @focus="clearFieldError('phoneNumber')"
                      @input="clearFieldError('phoneNumber')"
                      autocomplete="tel"
                  />
                  <div v-if="errors.phoneNumber" class="error-message">
                    {{ errors.phoneNumber }}
                  </div>
                </div>

                <!-- Password Field -->
                <div class="col-md-6">
                  <label for="password" class="form-label fw-medium text-dark">Mật khẩu</label>
                  <div class="position-relative">
                    <input
                        id="password"
                        v-model="form.password"
                        :type="showPassword ? 'text' : 'password'"
                        class="form-control form-control-lg pe-5 custom-input"
                        :class="{ 'has-error': errors.password, 'has-content': form.password }"
                        placeholder="Nhập mật khẩu của bạn..."
                        @focus="clearFieldError('password')"
                        @input="clearFieldError('password')"
                        autocomplete="new-password"
                    />
                    <button
                        type="button"
                        @click="togglePassword"
                        class="btn position-absolute end-0 top-50 translate-middle-y password-toggle-btn"
                        tabindex="-1"
                    >
                      <i :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                    </button>
                  </div>
                  <div v-if="errors.password" class="error-message">
                    {{ errors.password }}
                  </div>
                </div>
              </div>

              <!-- Confirm Password Field -->
              <div class="mb-3">
                <label for="confirmPassword" class="form-label fw-medium text-dark">Xác nhận mật khẩu</label>
                <div class="position-relative">
                  <input
                      id="confirmPassword"
                      v-model="form.confirmPassword"
                      :type="showConfirmPassword ? 'text' : 'password'"
                      class="form-control form-control-lg pe-5 custom-input"
                      :class="{ 'has-error': errors.confirmPassword, 'has-content': form.confirmPassword }"
                      placeholder="Xác nhận lại mật khẩu của bạn..."
                      @focus="clearFieldError('confirmPassword')"
                      @input="clearFieldError('confirmPassword')"
                      autocomplete="new-password"
                  />
                  <button
                      type="button"
                      @click="toggleConfirmPassword"
                      class="btn position-absolute end-0 top-50 translate-middle-y password-toggle-btn"
                      tabindex="-1"
                  >
                    <i :class="showConfirmPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                  </button>
                </div>
                <div v-if="errors.confirmPassword" class="error-message">
                  {{ errors.confirmPassword }}
                </div>
              </div>
              <!-- Register Button -->
              <button
                  type="submit"
                  class="btn btn-dark btn-lg w-100 mb-4 rounded-pill fw-medium register-btn"
                  style="height: 56px;"
                  :disabled="loading"
              >
                <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"
                      aria-hidden="true"></span>
                {{ loading ? 'Đang tạo tài khoản...' : 'Đăng ký' }}
              </button>

              <!-- Login Link -->
              <div class="text-center mb-4">
                <span class="text-muted">Đã có tài khoản? </span>
                <router-link to="/login" class="text-dark text-decoration-none fw-bold">Đăng nhập</router-link>
              </div>

            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/Auth.js';

const router = useRouter();
const authStore = useAuthStore();

const form = ref({
  fullname: '',
  email: '',
  phoneNumber: '',
  password: '',
  confirmPassword: '',
});

const showPassword = ref(false);
const showConfirmPassword = ref(false);
const errors = ref({
  fullname: '',
  email: '',
  phoneNumber: '',
  password: '',
  confirmPassword: '',
  general: ''
});

const success = ref('');
const loading = ref(false);

const togglePassword = () => {
  showPassword.value = !showPassword.value;
};

const toggleConfirmPassword = () => {
  showConfirmPassword.value = !showConfirmPassword.value;
};

const clearErrors = () => {
  errors.value = {
    fullname: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
    general: ''
  };
};

const clearFieldError = (field) => {
  if (errors.value[field]) {
    errors.value[field] = '';
  }
};

const validateFullname = () => {
  const name = form.value.fullname.trim();
  if (!name) {
    errors.value.fullname = 'Vui lòng nhập họ và tên';
    return false;
  } else if (name.length < 2) {
    errors.value.fullname = 'Họ và tên phải có ít nhất 2 ký tự';
    return false;
  }
  return true;
};

const validateEmail = () => {
  if (!form.value.email) {
    errors.value.email = 'Vui lòng nhập email';
    return false;
  } else if (!/\S+@\S+\.\S+/.test(form.value.email)) {
    errors.value.email = 'Email không hợp lệ';
    return false;
  }
  return true;
};

const validatePhoneNumber = () => {
  if (!form.value.phoneNumber) {
    errors.value.phoneNumber = 'Vui lòng nhập số điện thoại';
    return false;
  } else if (!/^0\d{9}$/.test(form.value.phoneNumber)) {
    errors.value.phoneNumber = 'Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số';
    return false;
  }
  return true;
};

const validatePassword = () => {
  const password = form.value.password;

  if (!password) {
    errors.value.password = 'Vui lòng nhập mật khẩu';
    return false;
  } else if (password.length < 6) {
    errors.value.password = 'Mật khẩu phải có ít nhất 6 ký tự';
    return false;
  } else if (!/(?=.*[a-z])/.test(password)) {
    errors.value.password = 'Mật khẩu phải chứa ít nhất 1 chữ thường';
    return false;
  } else if (!/(?=.*[A-Z])/.test(password)) {
    errors.value.password = 'Mật khẩu phải chứa ít nhất 1 chữ hoa';
    return false;
  } else if (!/(?=.*\d)/.test(password)) {
    errors.value.password = 'Mật khẩu phải chứa ít nhất 1 chữ số';
    return false;
  }

  errors.value.password = '';
  return true;
};

const validateConfirmPassword = () => {
  const confirmPassword = form.value.confirmPassword;
  const password = form.value.password;

  if (!confirmPassword) {
    errors.value.confirmPassword = 'Vui lòng xác nhận mật khẩu';
    return false;
  } else if (password !== confirmPassword) {
    errors.value.confirmPassword = 'Mật khẩu xác nhận không khớp';
    return false;
  }

  errors.value.confirmPassword = '';
  return true;
};

const validateForm = () => {
  clearErrors();
  const isFullnameValid = validateFullname();
  const isEmailValid = validateEmail();
  const isPhoneValid = validatePhoneNumber();
  const isPasswordValid = validatePassword();
  const isConfirmPasswordValid = validateConfirmPassword();

  return isFullnameValid && isEmailValid && isPhoneValid &&
      isPasswordValid && isConfirmPasswordValid
};

const handleRegister = async (event) => {
  event.preventDefault();
  event.stopPropagation();

  if (loading.value || !validateForm()) return;

  try {
    loading.value = true;
    clearErrors();
    success.value = '';

    await authStore.register({
      fullname: form.value.fullname,
      email: form.value.email,
      phoneNumber: form.value.phoneNumber,
      password: form.value.password,
      confirmPassword: form.value.confirmPassword
    });

    success.value = 'Đăng ký thành công! Đang chuyển hướng...';

    setTimeout(() => {
      const role = authStore.getUserRole;
      if (role === 'Admin' || role === 'Staff') {
        router.push('/admin');
      } else {
        router.push('/');
      }
    }, 1000);
  } catch (err) {
    let errorMessage = 'Đăng ký thất bại!';
    if (typeof err === 'string') {
      errorMessage = err;
    } else if (err?.message) {
      errorMessage = err.message;
    }
    errors.value.general = errorMessage;
  } finally {
    loading.value = false;
  }
};

const socialRegister = (provider) => {
  console.log(`Đăng ký bằng ${provider}`);
};
</script>

<style scoped>
.register-page {
  max-width: 100%;
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
  width: 162%;
  height: auto;
  transform: translate(-50%, -50%);
  object-fit: contain;
}

.title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 40px !important;
  padding-bottom: 20px;
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

/* Password Toggle Button */
.password-toggle-btn {
  border: none;
  background: none;
  color: #6b7280;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
  margin-right: 8px;
}

.password-toggle-btn:hover {
  background-color: #f3f4f6;
  color: #374151;
}

.password-toggle-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(52, 58, 64, 0.1);
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
.register-btn {
  background-color: #212529;
  border-color: #212529;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  font-weight: 600;
  font-size: 16px;
  margin-top: 10px;
}

.register-btn:hover:not(:disabled) {
  background-color: #1c1f23;
  border-color: #1a1e21;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(33, 37, 41, 0.3);
}

.register-btn:disabled {
  background-color: #6c757d;
  border-color: #6c757d;
  cursor: not-allowed;
  transform: none;
}

.register-btn .spinner-border-sm {
  width: 1rem;
  height: 1rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  .register-page {
    padding: 1rem;
  }

  .col-12 {
    padding: 2rem 1.5rem;
  }

  .row.g-3 .col-md-6 {
    margin-bottom: 1rem;
  }
}
</style>