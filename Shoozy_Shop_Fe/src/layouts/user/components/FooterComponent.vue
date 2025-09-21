
<script setup>
import { ref, computed } from 'vue'

const hoveredIcon = ref(null)

const currentYear = computed(() => {
  return new Date().getFullYear()
})

const openSocial = (platform) => {
  const urls = {
    twitter: 'https://twitter.com/shoozyshop',
    facebook: 'https://facebook.com/shoozyshop',
    instagram: 'https://instagram.com/shoozyshop',
    github: 'https://github.com/shoozyshop'
  }

  if (urls[platform]) {
    window.open(urls[platform], '_blank')
  }
}

// Emit events if parent wants to handle social media clicks
const emit = defineEmits(['social-click'])

const handleSocialClick = (platform) => {
  emit('social-click', platform)
}
</script>

<template>
  <footer class="footer">
    <div class="footer-content">
      <div class="footer-top">
        <div class="footer-brand">
          <h2 class="brand-name">Shoozy Shop</h2>
          <p class="brand-description">
            Chúng tôi có những trang phục phù hợp với phong cách của bạn và khiến bạn tự hào khi mặc.
            Dành cho cả nam và nữ.
          </p>
          <div class="social-icons">
            <a href="#" @click.prevent="openSocial('twitter')">
              <i class="fab fa-twitter"></i>
            </a>
            <a href="#" @click.prevent="openSocial('facebook')">
              <i class="fab fa-facebook"></i>
            </a>
            <a href="#" @click.prevent="openSocial('instagram')">
              <i class="fab fa-instagram"></i>
            </a>
            <a href="#" @click.prevent="openSocial('github')">
              <i class="fab fa-github"></i>
            </a>
          </div>
        </div>

        <div class="footer-section">
          <h5>CÔNG TY</h5>
          <ul>
            <li><a href="#">Về chúng tôi</a></li>
            <li><a href="#">Tính năng</a></li>
            <li><a href="#">Hoạt động</a></li>
            <li><a href="#">Tuyển dụng</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h5>HỖ TRỢ</h5>
          <ul>
            <li><a href="#">Hỗ trợ khách hàng</a></li>
            <li><a href="#">Chi tiết giao hàng</a></li>
            <li><a href="#">Điều khoản & Điều kiện</a></li>
            <li><a href="#">Chính sách bảo mật</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h5>HỎI ĐÁP</h5>
          <ul>
            <li><a href="#">Tài khoản</a></li>
            <li><a href="#">Quản lý giao hàng</a></li>
            <li><a href="#">Đơn hàng</a></li>
            <li><a href="#">Thanh toán</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h5>TÀI NGUYÊN</h5>
          <ul>
            <li><a href="#">Ebook miễn phí</a></li>
            <li><a href="#">Hướng dẫn phát triển</a></li>
            <li><a href="#">Hướng dẫn sử dụng - Blog</a></li>
            <li><a href="#">Danh sách Youtube</a></li>
          </ul>
        </div>
      </div>

      <div class="footer-bottom">
        <div class="footer-bottom-left">
          <p>Shoozy Shop © {{ currentYear }}, Đã đăng ký bản quyền</p>
        </div>
        <div class="payment-icons">
          <i
              class="fab fa-cc-visa"
              :class="{ 'payment-hover': hoveredIcon === 'visa' }"
              @mouseenter="hoveredIcon = 'visa'"
              @mouseleave="hoveredIcon = null"
          ></i>
          <i
              class="fab fa-cc-mastercard"
              :class="{ 'payment-hover': hoveredIcon === 'mastercard' }"
              @mouseenter="hoveredIcon = 'mastercard'"
              @mouseleave="hoveredIcon = null"
          ></i>
          <i
              class="fab fa-paypal"
              :class="{ 'payment-hover': hoveredIcon === 'paypal' }"
              @mouseenter="hoveredIcon = 'paypal'"
              @mouseleave="hoveredIcon = null"
          ></i>
          <i
              class="fab fa-apple-pay"
              :class="{ 'payment-hover': hoveredIcon === 'apple' }"
              @mouseenter="hoveredIcon = 'apple'"
              @mouseleave="hoveredIcon = null"
          ></i>
          <i
              class="fab fa-google-pay"
              :class="{ 'payment-hover': hoveredIcon === 'google' }"
              @mouseenter="hoveredIcon = 'google'"
              @mouseleave="hoveredIcon = null"
          ></i>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
/* Footer Section */
.footer {
  background-color: #f0f0f0;
  padding: 60px 0 30px;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-top {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr 1fr;
  gap: 40px;
  margin-bottom: 50px;
}

.footer-brand {
  max-width: 300px;
}

.footer-brand .brand-name {
  font-size: 2rem;
  font-weight: 900;
  color: #000;
  margin-bottom: 20px;
  letter-spacing: -1px;
}

.footer-brand .brand-description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 25px;
  font-size: 14px;
}

.social-icons {
  display: flex;
  gap: 15px;
}

.social-icons a {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background-color: white;
  border-radius: 50%;
  color: #000;
  text-decoration: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.social-icons a:hover {
  background-color: #000;
  color: white;
  transform: translateY(-2px);
}

.footer-section h5 {
  font-weight: 700;
  margin-bottom: 20px;
  color: #000;
  font-size: 16px;
  letter-spacing: 0.5px;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-section ul li {
  margin-bottom: 12px;
}

.footer-section ul li a {
  color: #666;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.footer-section ul li a:hover {
  color: #000;
}

.footer-bottom {
  border-top: 1px solid #ddd;
  padding-top: 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: -20px;
}

.footer-bottom-left {
  color: #666;
  font-size: 14px;
}

.payment-icons {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-top: -20px;
}

.payment-icons i {
  font-size: 28px;
  color: #666;
  transition: all 0.3s ease;
  cursor: pointer;
}

.payment-icons i:hover,
.payment-icons i.payment-hover {
  color: #000;
  transform: scale(1.1);
}

/* Responsive Design for Footer */
@media (max-width: 992px) {
  .footer-top {
    grid-template-columns: 1fr 1fr;
    gap: 30px;
  }

  .footer-brand {
    grid-column: 1 / -1;
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .footer-top {
    grid-template-columns: 1fr;
    gap: 30px;
    text-align: center;
  }

  .footer-brand {
    text-align: center;
  }

  .social-icons {
    justify-content: center;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .payment-icons {
    justify-content: center;
  }
}
</style>