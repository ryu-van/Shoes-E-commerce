<script setup>
import {ref, onMounted, onUnmounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from "@/stores/Auth.js";
import {getAllCategories} from "@/service/CategoryApi.js";
import {getAllBrands} from "@/service/BrandApi.js";
import {getProfile} from "@/service/UserApi.js";

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore(); // Initialize auth store
const isMobileMenuOpen = ref(false)
const isDropdownOpen = ref(false)
const isUserDropdownOpen = ref(false)
const navbarCollapse = ref(null)

const categories = ref([]);
const brands = ref([]);
const userProfile = ref(null);

const fetchCategories = async () => {
  try {
    const response = await getAllCategories();
    categories.value = response.data.data
  } catch (error) {
    console.error('Lỗi lấy categories:', error)
  }
}

const fetchBrands = async () => {
  try {
    const response = await getAllBrands();
    brands.value = response.data.data
  } catch (error) {
    console.error('Lỗi lấy brands:', error)
  }
}

const fetchUserProfile = async () => {
  try {
    const response = await getProfile();
    userProfile.value = response.data.data
  } catch (error) {
    console.error('Lỗi lấy user profile:', error)
  }
}

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
  setTimeout(() => {
    if (isMobileMenuOpen.value) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = 'auto'
    }
  }, 300)
}

const showDropdown = () => {
  isDropdownOpen.value = true
}

const hideDropdown = () => {
  isDropdownOpen.value = false
}

// Updated user dropdown functions - now using click instead of hover
const toggleUserDropdown = () => {
  isUserDropdownOpen.value = !isUserDropdownOpen.value
}

const closeUserDropdown = () => {
  isUserDropdownOpen.value = false
}

const setActive = () => {
  if (isMobileMenuOpen.value) {
    isMobileMenuOpen.value = false
    document.body.style.overflow = 'auto'
  }
}

const handleClickOutside = (e) => {
  if (navbarCollapse.value &&
      !navbarCollapse.value.contains(e.target) &&
      !e.target.closest('.navbar-toggler')) {
    if (isMobileMenuOpen.value) {
      isMobileMenuOpen.value = false
      document.body.style.overflow = 'auto'
    }
  }

  // Close user dropdown when clicking outside
  if (!e.target.closest('.user-dropdown-wrapper')) {
    isUserDropdownOpen.value = false
  }
}

// Helper function để check active state cho các menu giày
const isShoeMenuActive = (gender) => {
  return route.path === '/products' && route.query.gender === gender
}

// Updated logout function using auth store
const handleLogout = async () => {
  try {
    await authStore.logout();
    userProfile.value = null;
    closeUserDropdown();
    // Redirect to login page after successful logout
    router.push('/login');
  } catch (error) {
    console.error('Đăng xuất thất bại:', error);
    // Even if logout API fails, clear auth and redirect
    authStore.clearAuth();
    userProfile.value = null;
    closeUserDropdown();
    router.push('/login');
  }
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  fetchCategories();
  fetchBrands();

  // Check authentication status when component mounts
  await authStore.checkAuth();

  // Fetch user profile if authenticated
  if (authStore.isAuthenticated) {
    await fetchUserProfile();
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.body.style.overflow = 'auto'
})
</script>

<template>
  <nav class="navbar navbar-expand-lg">
    <div class="container">
      <router-link class="navbar-brand" to="/">Shoozy</router-link>
      <button class="navbar-toggler" type="button" @click="toggleMobileMenu" :class="{ collapsed: !isMobileMenuOpen }">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" :class="{ show: isMobileMenuOpen }" ref="navbarCollapse">
        <ul class="navbar-nav mx-auto">
          <li class="nav-item">
            <router-link class="nav-link" to="/" @click="setActive" :class="{ active: $route.path === '/' }">
              Trang chủ
            </router-link>
          </li>
          <li class="nav-item dropdown" @mouseenter="showDropdown" @mouseleave="hideDropdown">
            <a class="nav-link dropdown-toggle" href="#" id="shopDropdown" role="button" @click.prevent>
              Sản phẩm
            </a>
            <div class="dropdown-menu" :class="{ show: isDropdownOpen }">
              <div class="row">
                <div class="col-md-5">
                  <div class="dropdown-column">
                    <h6>Danh mục</h6>
                    <router-link v-for="category in categories" :key="category.id" class="dropdown-item"
                                 :to="`/products?category=${category.id}`">
                      {{ category.name }}
                    </router-link>
                  </div>
                </div>
                <div class="col-md-4">
                  <div class="dropdown-column">
                    <h6>Thương hiệu</h6>
                    <router-link v-for="brand in brands" :key="brand.id" class="dropdown-item"
                                 :to="`/products?brand=${brand.id}`">
                      {{ brand.name }}
                    </router-link>
                  </div>
                </div>
              </div>
            </div>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/products?gender=Male" @click="setActive"
                         :class="{ active: isShoeMenuActive('Male') }">
              Giày Nam
            </router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/products?gender=Female" @click="setActive"
                         :class="{ active: isShoeMenuActive('Female') }">
              Giày Nữ
            </router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/products?gender=Unisex" @click="setActive"
                         :class="{ active: isShoeMenuActive('Unisex') }">
              Giày Unisex
            </router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/products?gender=Kids" @click="setActive"
                         :class="{ active: isShoeMenuActive('Kids') }">
              Giày Trẻ Em
            </router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/contact" @click="setActive"
                         :class="{ active: $route.path === '/contact' }">
              Liên hệ
            </router-link>
          </li>
        </ul>
        <div class="nav-icons d-flex align-items-center">
          <router-link to="/carts" class="text-dark">
            <svg aria-hidden="true" fill="none" focusable="false" width="24"
                 class="header__nav-icon icon icon-cart" viewBox="0 0 24 24">
              <path
                  d="M4.75 8.25A.75.75 0 0 0 4 9L3 19.125c0 1.418 1.207 2.625 2.625 2.625h12.75c1.418 0 2.625-1.149 2.625-2.566L20 9a.75.75 0 0 0-.75-.75H4.75Zm2.75 0v-1.5a4.5 4.5 0 0 1 4.5-4.5v0a4.5 4.5 0 0 1 4.5 4.5v1.5"
                  stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              </path>
            </svg>
          </router-link>

          <!-- User Profile Dropdown - Changed to click trigger -->
          <div v-if="authStore.isAuthenticated" class="user-dropdown-wrapper ms-3">
            <button class="user-avatar-button" type="button" @click.prevent="toggleUserDropdown">
              <div class="user-avatar-container">
                <img class="avatar" alt="User Avatar"
                     :src="userProfile?.avatar || '@/assets/img/default-avatar.jpg'"/>
                <div class="user-info d-none d-lg-block">
                  <span class="user-name">
                    {{ userProfile?.fullname || authStore.user?.fullname || 'User' }}
                  </span>
                </div>
                <!--                <div class="dropdown-arrow" :class="{ 'rotated': isUserDropdownOpen }">-->
                <!--                  <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">-->
                <!--                    <path d="M6 8.5L2.5 5h7L6 8.5z"/>-->
                <!--                  </svg>-->
                <!--                </div>-->
              </div>
            </button>
            <div class="custom-dropdown-menu" :class="{ 'show': isUserDropdownOpen }">
              <router-link class="custom-dropdown-item" to="/profile" @click="closeUserDropdown">
                <svg class="dropdown-icon" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd"
                        d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-6-3a2 2 0 11-4 0 2 2 0 014 0zm-2 4a5 5 0 00-4.546 2.916A5.986 5.986 0 0010 16a5.986 5.986 0 004.546-2.084A5 5 0 0010 11z"
                        clip-rule="evenodd"></path>
                </svg>
                <span>Thông tin cá nhân</span>
              </router-link>
              <router-link class="custom-dropdown-item" to="/orders" @click="closeUserDropdown">
                <svg class="dropdown-icon" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd"
                        d="M5 3a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2V5a2 2 0 00-2-2H5zm0 2h10v7h-2l-1 2H8l-1-2H5V5z"
                        clip-rule="evenodd">
                  </path>
                </svg>
                <span>Đơn hàng của tôi</span>
              </router-link>
              <div class="custom-divider"></div>
              <button class="custom-dropdown-item logout-item" type="button" @click="handleLogout">
                <svg style="margin-left: 2px" class="dropdown-icon" fill="none" stroke="currentColor"
                     viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1">
                  </path>
                </svg>
                <span>Đăng xuất</span>
              </button>
            </div>
          </div>

          <!-- Login/Register links - Only show if not authenticated -->
          <div v-else class="nav-item ms-3">
            <router-link to="/login" class="text-dark">
              <svg aria-hidden="true" fill="none" focusable="false" width="24"
                   class="header__nav-icon icon icon-account" viewBox="0 0 24 24">
                <path
                    d="M16.125 8.75c-.184 2.478-2.063 4.5-4.125 4.5s-3.944-2.021-4.125-4.5c-.187-2.578 1.64-4.5 4.125-4.5 2.484 0 4.313 1.969 4.125 4.5Z"
                    stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                </path>
                <path d="M3.017 20.747C3.783 16.5 7.922 14.25 12 14.25s8.217 2.25 8.984 6.497"
                      stroke="currentColor" stroke-width="1.5" stroke-miterlimit="10"></path>
              </svg>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  font-family: 'Nunito', sans-serif !important;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  border-bottom: 1px solid #eee;
  padding: 8px 0;
}

.container {
  max-width: 1300px !important;
}

.navbar-toggler {
  border: 0 !important;
  outline: none !important;
  box-shadow: none !important;
}

.navbar-toggler:focus,
.navbar-toggler:active {
  border: 0 !important;
  outline: none !important;
  box-shadow: none !important;
  background-color: transparent !important;
}

.navbar-brand {
  font-family: 'Montserrat', sans-serif;
  font-weight: 900;
  font-size: 40px;
  color: #000 !important;
  text-decoration: none;
}

.navbar-nav .nav-link {
  color: #333 !important;
  font-weight: 500;
  margin: 0 15px;
  position: relative;
  transition: all 0.3s ease;
  text-decoration: none;
  padding: 15px 10px !important;
}

.navbar-nav .nav-link:hover {
  color: #3a3a3a !important;
}

/* Active state with underline */
.navbar-nav .nav-link.active::after,
.navbar-nav .nav-link:hover::after {
  content: '';
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  height: 2px;
  background-color: #3a3a3a;
  transition: all 0.3s ease;
}

.navbar-nav .nav-link::after {
  content: '';
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background-color: #3a3a3a;
  transition: all 0.3s ease;
}

.navbar-nav .nav-link:hover::after {
  width: 100%;
}

/* Dropdown styles */
.dropdown {
  position: relative;
}

.dropdown-menu {
  display: none;
  position: absolute;
  top: calc(100% + 15px);
  left: -50px;
  background-color: white;
  min-width: 600px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  border: none;
  border-radius: 8px;
  padding: 40px;
  margin-top: 0;
  opacity: 0;
  transform: translateY(-10px);
  transition: all 0.4s ease;
  z-index: 1000;
  pointer-events: none;
}

.dropdown-menu.show {
  display: block;
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.dropdown::before {
  content: '';
  position: absolute;
  top: 0;
  left: -20px;
  right: -20px;
  bottom: -20px;
  background: transparent;
  z-index: 998;
}

.dropdown-toggle::after {
  display: none !important;
}

.dropdown-column {
  margin-bottom: 0;
}

.dropdown-column h6 {
  font-weight: 600;
  margin-bottom: 25px;
  color: #000;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 1px;
  border-bottom: 1px solid #000;
  padding-bottom: 8px;
}

.dropdown-item {
  padding: 12px 0;
  border: none;
  background: none;
  color: #999;
  font-size: 13px;
  transition: all 0.2s ease;
  text-decoration: none;
  display: block;
  width: 100%;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-radius: 4px;
}

.dropdown-item:hover {
  color: #000;
  background: rgba(0, 123, 255, 0.05);
  padding-left: 10px;
  text-decoration: none;
}

.dropdown-item:focus {
  background: rgba(0, 123, 255, 0.1);
  color: #000;
  outline: none;
}

/* Enhanced User Dropdown Styles - Fixed positioning and z-index */
.user-dropdown-wrapper {
  position: relative;
}

.user-avatar-button {
  background: transparent;
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  outline: none;
  color: #666;
}

.user-avatar-button:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #333;
}

.user-avatar-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 35px;
  height: 35px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e0e0e0;
  transition: all 0.3s ease;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  line-height: 1.2;
  margin: 0;
  white-space: nowrap;
}

.dropdown-arrow {
  transition: transform 0.3s ease;
  color: #999;
  margin-left: 4px;
}

.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

/* Enhanced User Dropdown Menu */
.custom-dropdown-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  min-width: 205px;
  background: white;
  border-radius: 8px;
  padding: 8px 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(0, 0, 0, 0.1);
  z-index: 1050;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px) scale(0.95);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.custom-dropdown-menu.show {
  opacity: 1;
  visibility: visible;
  transform: translateY(0) scale(1);
}

.custom-dropdown-menu::before {
  content: '';
  position: absolute;
  top: -6px;
  right: 20px;
  width: 12px;
  height: 12px;
  background: white;
  transform: rotate(45deg);
  border-left: 1px solid rgba(0, 0, 0, 0.1);
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  z-index: 1051;
}

/* Custom dropdown item styles */
.custom-dropdown-item {
  padding: 10px 16px !important;
  font-size: 14px !important;
  color: #555 !important;
  text-transform: none !important;
  letter-spacing: normal !important;
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
  transition: all 0.2s ease !important;
  text-decoration: none !important;
  margin: 0 !important;
  border-radius: 0 !important;
  position: relative !important;
  border: none !important;
  background: transparent !important;
  width: 100%;
  cursor: pointer;
}

.custom-dropdown-item:hover {
  background: #f8f9fa !important;
  color: #333 !important;
  text-decoration: none !important;
}

.dropdown-icon {
  width: 20px;
  height: 20px;
  color: #666;
  transition: color 0.2s ease;
  flex-shrink: 0;
}

.custom-dropdown-item:hover .dropdown-icon {
  color: #333;
}

.custom-dropdown-item span {
  font-weight: 400;
}

.custom-divider {
  height: 1px;
  margin: 4px 0;
  background: #e9ecef;
  border: 0;
}

.logout-item:hover {
  background: rgba(220, 53, 69, 0.1) !important;
  color: #dc3545 !important;
}

.logout-item:hover .dropdown-icon {
  color: #dc3545 !important;
}

/* Mobile responsiveness */
@media (max-width: 991px) {
  .custom-dropdown-menu {
    position: static !important;
    display: block !important;
    opacity: 1 !important;
    visibility: visible !important;
    transform: none !important;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
    background-color: white !important;
    margin-top: 10px !important;
    border-radius: 8px !important;
    min-width: auto !important;
  }

  .custom-dropdown-menu::before {
    display: none !important;
  }

  .user-info {
    display: block !important;
  }
}
</style>