<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/Auth.js'

const authStore = useAuthStore()
const currentRole = computed(() => authStore.getUserRole)
const showProductMenu = ref(false)
const activeItem = ref('')
const showUserMenu = ref(false)


// Thêm các computed này vào
const normalizedRole = computed(() => (currentRole.value || '').trim().toLowerCase())
const isAdmin = computed(() => normalizedRole.value === 'admin')
const isStaff = computed(() => normalizedRole.value === 'staff')

console.log("Role from store (computed):", currentRole.value)


function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}
function toggleProductMenu() {
  showProductMenu.value = !showProductMenu.value
}
function setActiveItem(item) {
  activeItem.value = item
}
</script>


<template>
  <nav class="navbar navbar-dark navbar-theme-primary px-4 col-12 d-lg-none">
    <a class="navbar-brand me-lg-5" href="/admin">
      <img class="navbar-brand-dark" src="@/assets/img/z6629089719417_7f6fd84eae2c714569a48c95adecd6c0.jpg"
           alt="Shoozy Shop Logo"/>
    </a>
    <div class="d-flex align-items-center">
      <button class="navbar-toggler d-lg-none collapsed" type="button" data-bs-toggle="collapse"
              data-bs-target="#sidebarMenu" aria-controls="sidebarMenu" aria-expanded="false"
              aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
      </button>
    </div>
  </nav>

  <nav id="sidebarMenu" class="sidebar d-lg-block bg-gray-800 text-white collapse" data-simplebar>
    <div class="sidebar-inner px-4 pt-3">
      <ul class="nav flex-column pt-3 pt-md-0">
        <li style="margin-bottom: 10px" class="nav-item">
          <a href="/admin" class="nav-link d-flex align-items-center" @click="setActiveItem('shoozy')">
                        <span class="sidebar-icon-logo">
                            <img style="border-radius: 15px; margin-right: 5px; object-fit: contain"
                                 src="@/assets/img/logo.jpg" height="80"
                                 width="80" alt="Shoozy Logo"/>
                        </span>
            <span style="font-size: 15px" class="mt-1 ms-1 sidebar-text">Shoozy Shop</span>
          </a>
        </li>
        <li role="separator" class="dropdown-divider mb-3 border-gray-700"></li>

        <li class="nav-item">
          <router-link to="/admin" class="nav-link d-flex align-items-center"
                       :class="{ active: activeItem === 'trangchu' }" @click="setActiveItem('trangchu')">
                        <span class="sidebar-icon">
                            <svg class="icon icon-xs me-2" fill="currentColor" viewBox="0 0 20 20">
                                <path d="M2 10a8 8 0 018-8v8h8a8 8 0 11-16 0z"/>
                                <path d="M12 2.252A8.014 8.014 0 0117.748 8H12V2.252z"/>
                            </svg>
                        </span>
            <span class="sidebar-text">Trang chủ</span>
          </router-link>
        </li>
        <li class="nav-item">
          <a href="#" class="nav-link d-flex align-items-center" role="button"
             :aria-expanded="showProductMenu.toString()" @click.prevent="toggleProductMenu">
                        <span class="sidebar-icon">
                            <svg height="20px" width="20px" viewBox="0 0 512 512" fill="currentColor">
                <path class="st0"
                      d="M264.141,416.896L14.646,205.121L0,222.387l249.53,211.791l0.036,0.032 c28.359,23.865,64.362,37,101.444,37.008h159.915v-22.639H351.01C319.297,448.578,288.402,437.326,264.141,416.896z"/>
                <path class="st0" d="M351.01,425.948c0,0,63.721,0,120.309,0c56.587,0,48.101-56.591,11.318-65.074
                  c-24.818-5.728-56.682-18.895-56.682-18.895c-12.577-4.194-22.875-13.389-28.442-25.4c0,0-1.377-2.92-3.688-7.834l-46.194,
                  13.966c-5.048,1.528-10.381-1.322-11.908-6.369c-1.519-5.048,1.326-10.382,6.374-11.901l43.528-13.167
                  c-3.743-7.975-8.083-17.241-12.656-27.021l-43.171,13.064c-5.049,1.528-10.382-1.345-11.909-6.377
                  c-1.531-5.049,1.338-10.382,6.373-11.908l40.529-12.257c-4.312-9.258-8.601-18.476-12.574-27.046l-40.255,
                  12.185c-5.049,1.52-10.382-1.33-11.912-6.377c-1.515-5.049,1.329-10.382,6.378-11.901l37.708-11.41
                  c-5.405-11.766-9.326-20.486-10.37-23.192c-4.538-11.798-14.504-44.525-37.245-26.334
                  c-37.391,29.934-105.602,21.158-123.212-11.56c-13.202-24.522-5.654-50.926,0-71.674
                  c4.526-16.584-18.868-42.443-35.837-19.805C125.464,65.66,24.296,185.458,24.296,185.458
                  L278.72,399.575C298.961,416.611,324.559,425.948,351.01,425.948z"/>
              </svg>
                        </span>
            <span class="sidebar-text">Quản lý sản phẩm</span>
            <span class="ms-auto">
                            <svg class="icon chevron"
                                 :style="{ transform: showProductMenu ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }"
                                 xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M6 12l4-4-4-4v8z"/>
                            </svg>
                        </span>
          </a>
          <ul class="nav flex-column ms-3" v-show="showProductMenu"
              style="transition: max-height 0.3s ease; overflow: hidden;">
            <li class="nav-item">
              <router-link to="/admin/products" class="nav-link"
                           :class="{ active: activeItem === 'product' }" @click="setActiveItem('product')">
                <span class="sidebar-text">Sản phẩm</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/admin/brands" class="nav-link" :class="{ active: activeItem === 'brand' }"
                           @click="setActiveItem('brand')">
                <span class="sidebar-text">Thương hiệu</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/admin/categories" class="nav-link"
                           :class="{ active: activeItem === 'category' }" @click="setActiveItem('category')">
                <span class="sidebar-text">Danh mục</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/admin/materials" class="nav-link"
                           :class="{ active: activeItem === 'material' }" @click="setActiveItem('material')">
                <span class="sidebar-text">Chất liệu</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/admin/colors" class="nav-link" :class="{ active: activeItem === 'color' }"
                           @click="setActiveItem('color')">
                <span class="sidebar-text">Màu sắc</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/admin/sizes" class="nav-link" :class="{ active: activeItem === 'size' }"
                           @click="setActiveItem('size')">
                <span class="sidebar-text">Size</span>
              </router-link>
            </li>
          </ul>
        </li>
        <li class="nav-item">
          <router-link to="/admin/orders" class="nav-link d-flex align-items-center"
                       :class="{ active: activeItem === 'donhang' }" @click.native="setActiveItem('donhang')">
                        <span class="sidebar-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor"
                                 viewBox="0 0 24 24">
                                <path
                                    d="M5 3a2 2 0 00-2 2v15.586l2.293-2.293a1 1 0 011.414 0L9 20l2.293-2.293a1 1 0 011.414 0L15 20l2.293-2.293a1 1 0 011.414 0L21 20V5a2 2 0 00-2-2H5zm2 4h10a1 1 0 110 2H7a1 1 0 110-2zm0 4h10a1 1 0 110 2H7a1 1 0 110-2z"/>
                            </svg>
                        </span>
            <span class="sidebar-text">Quản lý đơn hàng</span>
          </router-link>
        </li>

        <li class="nav-item">
          <router-link to="/admin/promotions" class="nav-link d-flex align-items-center"
                       :class="{ active: activeItem === 'khuyenmai' }" @click="setActiveItem('khuyenmai')">
            <span class="sidebar-icon"><i class="fas fa-tags"></i></span>
            <span class="sidebar-text">Quản lý khuyến mãi</span>
          </router-link>
        </li>

        <li class="nav-item">
          <router-link to="/admin/coupons" class="nav-link d-flex align-items-center"
                       :class="{ active: activeItem === 'coupon' }" @click="setActiveItem('coupon')">
            <span class="sidebar-icon"><i class="fas fa-percentage"></i></span>
            <span class="sidebar-text">Quản lý mã giảm giá</span>
          </router-link>
        </li>

        <li class="nav-item">
          <router-link to="/admin/invoices" class="nav-link d-flex align-items-center"
                       :class="{ active: activeItem === 'hoadon' }" @click.native="setActiveItem('hoadon')">
                        <span class="sidebar-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor"
                                 viewBox="0 0 24 24">
                                <path
                                    d="M5 3a2 2 0 00-2 2v15.586l2.293-2.293a1 1 0 011.414 0L9 20l2.293-2.293a1 1 0 011.414 0L15 20l2.293-2.293a1 1 0 011.414 0L21 20V5a2 2 0 00-2-2H5zm2 4h10a1 1 0 110 2H7a1 1 0 110-2zm0 4h10a1 1 0 110 2H7a1 1 0 110-2z"/>
                            </svg>
                        </span>
            <span class="sidebar-text">Quản lý hóa đơn</span>
          </router-link>
        </li>

        <li class="nav-item">
          <router-link to="/admin/reviews" class="nav-link d-flex align-items-center"
                       :class="{ active: activeItem === 'binhluan' }" @click.native="setActiveItem('binhluan')">
                        <span class="sidebar-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none"
                                 stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round"
                                      d="M7 8h10M7 12h5m-5 8l-4-4H3a2 2 0 01-2-2V6a2 2 0 012-2h18a2 2 0 012 2v8a2 2 0 01-2 2h-5l-4 4z"/>
                            </svg>

                        </span>
            <span class="sidebar-text">Quản lý đánh giá</span>
          </router-link>
        </li>
<li class="nav-item">
  <router-link to="/admin/return-requests" class="nav-link d-flex align-items-center"
               :class="{ active: activeItem === 'returnrequests' }"
               @click="setActiveItem('returnrequests')">
    <span class="sidebar-icon">
      <i class="fas fa-undo-alt"></i>
    </span>
    <span class="sidebar-text">Quản lý trả hàng</span>
  </router-link>
</li>

<!-- quản lý người dùng (admin) -->
<li class="nav-item" v-if="isAdmin || isStaff">
  <a href="#" class="nav-link d-flex align-items-center"
     :aria-expanded="showUserMenu.toString()" @click.prevent="toggleUserMenu">
    <span class="sidebar-icon"><i class="fas fa-users"></i></span>
    <span class="sidebar-text">Quản lý người dùng</span>
    <span class="ms-auto">
      <svg class="icon chevron"
           :style="{ transform: showUserMenu ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }"
           xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
           viewBox="0 0 16 16">
        <path fill-rule="evenodd" d="M6 12l4-4-4-4v8z"/>
      </svg>
    </span>
  </a>
  <ul class="nav flex-column ms-3" v-show="showUserMenu">
    <li class="nav-item" v-if="isAdmin">
      <router-link to="/admin/users/staff"
                   class="nav-link"
                   :class="{ active: activeItem === 'staff' }"
                   @click="setActiveItem('staff')">
        <span class="sidebar-text">Quản lý nhân viên</span>
      </router-link>
    </li>
    <li class="nav-item">
      <router-link to="/admin/users/customer"
                   class="nav-link"
                   :class="{ active: activeItem === 'customer' }"
                   @click="setActiveItem('customer')">
        <span class="sidebar-text">Quản lý khách hàng</span>
      </router-link>
    </li>
  </ul>
</li>

        <!-- bán hàng tại quầy -->
        <li class="nav-item">
          <router-link to="/admin/selloff" class="nav-link d-flex align-items-center"
                       :class="{ active: activeItem === 'taiquay' }" @click="setActiveItem('taiquay')">
                        <span class="sidebar-icon">
                              <svg xmlns="http://www.w3.org/2000/svg" 
           width="20" height="20" fill="none" 
           stroke="currentColor" stroke-width="2" 
           viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round"
          d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13l-1.35 2.7a1 1 0 00.9 1.5h12.9m-13.45-4.2L5.4 5M16 17a2 2 0 11.001 3.999A2 2 0 0116 17zm-8 0a2 2 0 11.001 3.999A2 2 0 018 17z"/>
      </svg>

                        </span>
            <span class="sidebar-text">Bán Hàng Tại Quầy</span>
          </router-link>
        </li>
        
        <!-- Divider before Back to Client button -->
        <li role="separator" class="dropdown-divider my-3 border-gray-700"></li>

        <!-- Back to Client View Button -->
        <li class="nav-item">
          <a href="/" class="nav-link d-flex align-items-center back-to-client"
             @click="setActiveItem('client')">
                        <span class="sidebar-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 24 24">
                                <path d="M9.5 3A6.5 6.5 0 0 1 16 9.5c0 1.61-.59 3.09-1.56 4.23l.27.27h.79l5 5-1.5 1.5-5-5v-.79l-.27-.27A6.516 6.516 0 0 1 9.5 16 6.5 6.5 0 0 1 3 9.5 6.5 6.5 0 0 1 9.5 3m0 2C7 5 5 7 5 9.5S7 14 9.5 14 14 12 14 9.5 12 5 9.5 5z"/>
                                <path d="M12 10.5v3l4-4-4-4v3c-3.31 0-6 2.69-6 6 0 1.01.25 1.97.7 2.8l1.46-1.46A3.74 3.74 0 0 1 8 12c0-2.21 1.79-4 4-4z"/>
                            </svg>
                        </span>
            <span class="sidebar-text">Về trang khách hàng</span>
          </a>
        </li>
      </ul>
    </div>
  </nav>
</template>

<style scoped>
.nav-item .nav-link {
  padding: 0.5rem 1rem;
  border-radius: 0.25rem;
  transition: all 0.2s ease;
  position: relative;
}

.nav-item .nav-link:hover {
  background-color: #3b4252;
}

.nav-item .nav-link.active {
  background-color: #3b4252;
}

.nav-item .nav-link .sidebar-text {
  font-size: 14px;
}

.nav-item .nav-link .sidebar-icon {
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-right: 8px;
  width: 24px;
  min-width: 24px;
}

.nav-item .nav-link .sidebar-icon svg,
.nav-item .nav-link .sidebar-icon i {
  transition: transform 0.2s;
  width: 20px;
  height: 20px;
}

/* Logo styles - không bị ảnh hưởng bởi hover/active */
.nav-item:first-child .nav-link {
  transform: none !important;
  background-color: transparent !important;
  border-left: none !important;
}

.nav-item:first-child .nav-link:hover {
  transform: none !important;
  background-color: transparent !important;
}

.nav-item:first-child .nav-link.active {
  transform: none !important;
  background-color: transparent !important;
  border-left: none !important;
}

.nav-item:first-child .nav-link .sidebar-icon {
  background-color: transparent !important;
  border-radius: 0 !important;
  padding: 0 !important;
}

/* Submenu styles */
.submenu {
  transition: all 0.3s ease;
  padding-left: 1.5rem;
}

.submenu .nav-item .nav-link {
  padding: 0.5rem 1rem;
  font-size: 13px;
  margin-left: 0;
}

.submenu .nav-item .nav-link:hover {
  background-color: #3b4252;
}

.submenu .nav-item .nav-link.active {
  background-color: #3b4252;
}

.icon.chevron {
  transition: transform 0.2s;
}

.navbar-toggler {
  border: 0 !important;
  outline: none !important;
  box-shadow: none !important;
}

/* Back to Client button special styling */
.back-to-client {
  background-color: #2d3748 !important;
  border: 1px solid #4a5568;
  color: #e2e8f0 !important;
  margin-top: 18px;
}

.back-to-client:hover {
  background-color: #4a5568 !important;
  color: #ffffff !important;
  transform: translateX(2px);
}

.back-to-client .sidebar-icon svg {
  color: #81c784;
}
</style>