<script setup>
import { ref, onMounted } from "vue";
import apiClient from "@/auth/api.js";

const formData = ref({
  fullName: '',
  email: '',
  message: ''
});

const isSubmitting = ref(false);
const errors = ref({});
const showSuccess = ref(false);
const showError = ref(false);
const apiMessage = ref('');

// Validate form
const validateForm = () => {
  errors.value = {};

  if (!formData.value.fullName.trim()) {
    errors.value.fullName = 'Họ tên không được để trống';
  } else if (formData.value.fullName.length > 100) {
    errors.value.fullName = 'Họ tên không được quá 100 ký tự';
  }

  if (!formData.value.email.trim()) {
    errors.value.email = 'Email không được để trống';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.value.email)) {
    errors.value.email = 'Email không hợp lệ';
  }

  if (!formData.value.message.trim()) {
    errors.value.message = 'Tin nhắn không được để trống';
  } else if (formData.value.message.length > 1000) {
    errors.value.message = 'Tin nhắn không được quá 1000 ký tự';
  }

  return Object.keys(errors.value).length === 0;
};

const sendContactEmail = async (contactData) => {
  try {
    const response = await apiClient.post('/contact/send', contactData);
    console.log('Response:', response);

    const result = response.data;
    console.log('Result:', result);

    if (response.status === 200 && result && result.success) {
      return { success: true, message: result.message || 'Gửi tin nhắn thành công!' };
    } else {
      return { success: false, message: result.message || 'Có lỗi xảy ra khi gửi tin nhắn' };
    }
  } catch (error) {
    console.error('API Error:', error);
    console.log('Error response:', error.response);

    if (error.response && error.response.data) {
      return { success: false, message: error.response.data.message || 'Có lỗi xảy ra từ server' };
    }

    return { success: false, message: 'Không thể kết nối tới server. Vui lòng thử lại sau.' };
  }
};

const handleSubmit = async () => {
  console.log('Form submitted');

  // Reset messages
  showSuccess.value = false;
  showError.value = false;
  errors.value = {};

  // Validate form
  if (!validateForm()) {
    console.log('Form validation failed:', errors.value);
    return;
  }

  isSubmitting.value = true;

  try {
    console.log('Sending data:', formData.value);
    const result = await sendContactEmail(formData.value);
    console.log('Send result:', result);

    if (result.success) {
      console.log('Success - showing success message');
      showSuccess.value = true;
      showError.value = false;
      apiMessage.value = result.message;

      // Reset form after successful submission
      formData.value = {
        fullName: '',
        email: '',
        message: ''
      };

      // Hide success message after 5 seconds
      setTimeout(() => {
        showSuccess.value = false;
      }, 5000);

    } else {
      console.log('Failed - showing error message');
      showError.value = true;
      showSuccess.value = false;
      apiMessage.value = result.message;

      // Hide error message after 5 seconds
      setTimeout(() => {
        showError.value = false;
      }, 5000);
    }

  } catch (error) {
    console.error('Submit Error:', error);
    showError.value = true;
    showSuccess.value = false;
    apiMessage.value = 'Có lỗi không xác định xảy ra. Vui lòng thử lại sau.';

    setTimeout(() => {
      showError.value = false;
    }, 5000);
  } finally {
    isSubmitting.value = false;
  }
};

const animateOnScroll = () => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.style.opacity = '1';
        entry.target.style.transform = 'translateY(0)';
      }
    });
  });

  const elements = document.querySelectorAll('.animate-fade-in');
  elements.forEach(element => {
    element.style.opacity = '0';
    element.style.transform = 'translateY(30px)';
    element.style.transition = 'all 0.8s ease';
    observer.observe(element);
  });
};

onMounted(() => {
  animateOnScroll();
});
</script>

<template>
  <div class="contact-page">
    <!-- Hero Section -->
    <section class="contact-hero">
      <div class="container">
        <div class="hero-wrapper">
          <div class="hero-content">
            <div class="content-left animate-fade-in">
              <h1>Bạn cần hỗ trợ ?</h1>
              <p>Chúng tôi luôn sẵn sàng được hỗ trợ bạn, hãy để lại thông tin cho chúng tôi nhé. Yêu cầu của bạn sẽ được xử lý và phản hồi trong thời gian sớm nhất.</p>
            </div>

            <div class="content-right animate-fade-in">
              <!-- Success Message - Hiển thị ở đầu form -->
              <div v-if="showSuccess" class="alert alert-success">
                <div class="alert-content">
                  <span>{{ apiMessage }}</span>
                </div>
              </div>

              <!-- Error Message - Hiển thị ở đầu form -->
              <div v-if="showError" class="alert alert-error">
                <div class="alert-content">
                  <span>{{ apiMessage }}</span>
                </div>
              </div>

              <div class="contact-form">
                <form @submit.prevent="handleSubmit">
                  <div class="form-row">
                    <div class="form-group">
                      <label for="fullName">Họ tên*</label>
                      <input
                          type="text"
                          id="fullName"
                          v-model="formData.fullName"
                          placeholder="Tên đầy đủ"
                          :class="{ 'error': errors.fullName }"
                          maxlength="100"
                      />
                      <span v-if="errors.fullName" class="error-message">{{ errors.fullName }}</span>
                    </div>
                    <div class="form-group">
                      <label for="email">Email*</label>
                      <input
                          type="email"
                          id="email"
                          v-model="formData.email"
                          placeholder="Địa chỉ email"
                          :class="{ 'error': errors.email }"
                      />
                      <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
                    </div>
                  </div>

                  <div class="form-group">
                    <label for="message">Tin nhắn*</label>
                    <textarea
                        id="message"
                        v-model="formData.message"
                        placeholder="Đừng ngại hỏi về đơn hàng của bạn"
                        rows="5"
                        :class="{ 'error': errors.message }"
                        maxlength="1000"
                    ></textarea>
                    <div class="character-count">
                      {{ formData.message.length }}/1000
                    </div>
                    <span v-if="errors.message" class="error-message">{{ errors.message }}</span>
                  </div>

                  <button
                      type="submit"
                      class="submit-btn"
                      :disabled="isSubmitting"
                  >
                    <span v-if="isSubmitting" class="loading-spinner"></span>
                    {{ isSubmitting ? 'Đang gửi...' : 'Gửi' }}
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Company Info Section -->
    <section class="company-info">
      <div class="container">
        <div class="info-wrapper">
          <div class="info-left animate-fade-in">
            <div class="info-card">
              <h3>CÔNG TY TNHH SHOOZY SHOP VIỆT NAM</h3>
              <p class="company-description">
                <strong>Shoozy Shop</strong> là kênh mua sắm giày dép hàng đầu Việt Nam
              </p>

              <div class="company-details">
                <div class="detail-item">
                  <strong>TP.HCM:</strong> Lầu 10, Tòa nhà HD Bank, 25Bis Nguyễn Thị Minh Khai, P. Bến Nghé, Q1, TP HCM
                </div>
                <div class="detail-item">
                  <strong>Hà Nội:</strong> Ngõ 18, Phố Kiều Mai, Phú Diễn, Quận Bắc Từ Liêm, TP Hà Nội
                </div>
                <div class="detail-item">
                  <strong>Giấy chứng nhận đăng ký doanh nghiệp số:</strong> 0316376254
                </div>
              </div>
            </div>
          </div>

          <div class="info-right animate-fade-in">
            <div class="info-card">
              <h3>LIÊN HỆ VỚI CHÚNG TÔI</h3>

              <div class="contact-details">
                <div class="detail-item">
                  <strong>Email:</strong> shopshoozy@gmail.com
                </div>
                <div class="detail-item">
                  <strong>CSKH:</strong> 0358850836
                </div>
                <div class="detail-item">
                  <strong>Thời gian làm việc:</strong> 8h - 21h (Thứ 2 - Thứ 7)
                </div>
                <div class="detail-item">
                  <strong>Fanpage:</strong> https://www.facebook.com/shoozy.shop
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.contact-page {
  font-family: 'Arial', sans-serif;
  background-color: #fff;
  color: #333;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Alert Messages - Giống login nhưng đẹp hơn */
.alert {
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  animation: slideDown 0.3s ease-out;
  font-weight: 500;
  border: none;
}

.alert-success {
  background-color: #f0fdf4;
  color: #15803d;
  border-left: 4px solid #22c55e;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.1);
}

.alert-error {
  background-color: #fef2f2;
  color: #dc2626;
  border-left: 4px solid #ef4444;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.1);
}

.alert-content {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.success-icon,
.error-icon {
  font-size: 16px;
  margin-right: 2px;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Hero Section */
.contact-hero {
  padding: 60px 0;
  background: #fff;
}

.hero-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 60px;
}

.content-left {
  flex: 1;
  padding-right: 40px;
  margin-bottom: 20px;
}

.content-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.hero-content h1 {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 20px;
  color: #333;
}

.hero-content p {
  font-size: 16px;
  color: #666;
  margin-bottom: 0;
  line-height: 1.6;
}

/* Form Styles */
.contact-form {
  background: #fff;
  padding: 0;
  border-radius: 0;
  border: none;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  flex: 1;
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #fff;
  color: #333;
  font-size: 14px;
  font-family: inherit;
  transition: all 0.3s ease;
}

.form-group input.error,
.form-group textarea.error {
  border-color: #dc3545;
  box-shadow: 0 0 0 2px rgba(220, 53, 69, 0.1);
}

.error-message {
  color: #dc3545;
  font-size: 12px;
  margin-top: 4px;
  display: block;
}

.character-count {
  font-size: 12px;
  color: #666;
  text-align: right;
  margin-top: 4px;
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #999;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #333;
  box-shadow: 0 0 0 2px rgba(51, 51, 51, 0.1);
}

.submit-btn {
  background: #000;
  color: #fff;
  border: none;
  padding: 12px 60px;
  border-radius: 35px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  min-width: 140px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid #fff;
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

.submit-btn:hover:not(:disabled) {
  background: #333;
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Company Info Section */
.company-info {
  padding: 60px 0;
  background: #f8f8f8;
  border-top: 1px solid #eee;
}

.info-wrapper {
  display: flex;
  gap: 60px;
  align-items: flex-start;
}

.info-left,
.info-right {
  flex: 1;
}

.info-card {
  background: transparent;
  padding: 0;
  border-radius: 0;
  height: auto;
  border: none;
}

.info-card h3 {
  font-size: 1.3rem;
  font-weight: 700;
  margin-bottom: 20px;
  color: #333;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.company-description {
  margin-bottom: 30px;
  line-height: 1.6;
  color: #666;
}

.company-details,
.contact-details {
  margin-bottom: 0;
}

.detail-item {
  margin-bottom: 12px;
  line-height: 1.6;
  color: #666;
  font-size: 14px;
}

.detail-item strong {
  color: #333;
}

/* Responsive Design */
@media (max-width: 768px) {
  .hero-wrapper,
  .info-wrapper {
    flex-direction: column;
    gap: 40px;
  }

  .content-left {
    padding-right: 0;
  }

  .hero-content h1 {
    font-size: 2rem;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .social-links {
    justify-content: flex-start;
  }

  .contact-hero,
  .company-info {
    padding: 40px 0;
  }
}

@media (max-width: 480px) {
  .hero-content h1 {
    font-size: 1.8rem;
  }

  .container {
    padding: 0 15px;
  }

  .hero-wrapper,
  .info-wrapper {
    gap: 30px;
  }
}

/* Animation for fade in effect */
.animate-fade-in {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s ease;
}
</style>