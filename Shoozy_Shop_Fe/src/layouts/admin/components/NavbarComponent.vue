<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import emitter from '@/service/EvenBus.js'
import {useAuthStore} from "@/stores/Auth.js";
import router from "@/router/index.js";

// State cho notifications
const notifications = ref([])
const unreadCount = ref(0)
const isLoading = ref(false)
const user = useAuthStore().getUser
const authStore = useAuthStore();
const userProfile = ref(null);

// Thêm notification mới
const addNotification = (data) => {
  const { payload: order, action } = data
  console.log('Data:', data)
  console.log('Order notification received:', order, action)
  // Bỏ qua update action
  if (action === 'ORDER_UPDATED_STATUS') return
  let message = 'Thông báo đơn hàng'
  switch (action) {
    case 'ORDER_CREATED':
      message = `Đã tạo đơn hàng mới với mã: ${order.orderCode || order.id}`
      break
    case 'ORDER_CANCELLED':
      message = `Đã hủy đơn hàng ${order.orderCode || order.id} `
      break
    case 'ORDER_COMPLETED':
      message = `Đã xác nhận hoàn tất đơn hàng ${order.orderCode || order.id}`
      break
    default:
      message = ''
  }

  const notification = {
    id: `${order.id}_${Date.now()}_${Math.random()}`,
    orderId: order.id,
    user: order.fullname || 'Khách hàng',
    time: new Date().toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }),
    avatar: order.user?.avatar || null,
    phone: order.phoneNumber || '',
    orderValue: order.totalMoney,
    orderCode: order.orderCode || '',
    address: order.address || '',
    status: order.status || '',
    paymentMethod: order.paymentMethod?.name || order.paymentType || 'Chưa xác định',
    createdAt: order.createdAt || new Date().toISOString(),
    items: order.items || order.orderDetails || [],
    itemCount: (order.items || order.orderDetails || []).length,
    isNew: true,
    message: message,
    orderData: order
  }

  // Kiểm tra trùng lặp
  const existingIndex = notifications.value.findIndex(n => n.orderId === order.id)
  if (existingIndex > -1) {
    notifications.value[existingIndex] = notification
  } else {
    notifications.value.unshift(notification)
  }

  // Giới hạn số lượng notifications
  if (notifications.value.length > 50) {
    notifications.value = notifications.value.slice(0, 50)
  }

  unreadCount.value = notifications.value.filter(n => n.isNew).length

  // Hiển thị toast notification
  showToast(notification)

  // Phát âm thanh thông báo
  playNotificationSound()

  // Hiển thị desktop notification
  showDesktopNotification(notification)

  // Lưu vào localStorage
  saveNotificationsToStorage()
}

// Format tiền tệ
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount || 0)
}

// Audio notification - giống phần đầu
const playNotificationSound = () => {
  try {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()

    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)

    oscillator.frequency.value = 800
    oscillator.type = 'sine'
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5)

    oscillator.start(audioContext.currentTime)
    oscillator.stop(audioContext.currentTime + 0.5)
  } catch (error) {
    console.log('Cannot play notification sound:', error)
  }
}

// Desktop notification
const showDesktopNotification = (notification) => {
  if ('Notification' in window && Notification.permission === 'granted') {
    const desktopNotification = new Notification('Đơn hàng mới!', {
      body: `${notification.user} vừa đặt đơn hàng ${notification.orderCode}`,
      icon: notification.avatar,
      tag: notification.orderCode,
      requireInteraction: true
    })

    desktopNotification.onclick = () => {
      window.focus()
      handleNotificationClick(notification)
      desktopNotification.close()
    }

    setTimeout(() => {
      desktopNotification.close()
    }, 5000)
  }
}


// Updated logout function using auth store
const handleLogout = async () => {
  try {
    await authStore.logout();
    userProfile.value = null;
    // Redirect to login page after successful logout
    router.push('/login');
  } catch (error) {
    console.error('Đăng xuất thất bại:', error);
    // Even if logout API fails, clear auth and redirect
    authStore.clearAuth();
    userProfile.value = null;
    router.push('/login');
  }
}

// Request notification permission
const requestNotificationPermission = () => {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission().then(permission => {
      console.log('Notification permission:', permission)
    })
  }
}

// Toast notification - cải thiện

const showToast = (notification) => {
  const toast = document.createElement('div')
  toast.className = 'toast-notification'
  toast.innerHTML = `
    <div class="toast-content">
      <div class="d-flex align-items-center">
        <img src="${notification.avatar}"
             alt="Avatar"
             class="toast-avatar me-3"">
        <div>
          <strong class="d-block">${notification.user}</strong>
          <small class="text-muted">${notification.message}</small>
          <div class="mt-1">
            <small class="text-success fw-bold">${formatCurrency(notification.orderValue)}</small>
          </div>
        </div>
      </div>
    </div>
  `

  toast.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    background: white;
    color: #333;
    padding: 15px;
    border-radius: 12px;
    border-left: 4px solid #28a745;
    box-shadow: 0 10px 25px rgba(0,0,0,0.15);
    z-index: 9999;
    min-width: 350px;
    max-width: 400px;
    transform: translateX(450px);
    transition: transform 0.3s ease;
  `

  // Add toast avatar styles
  const style = document.createElement('style')
  style.textContent = `
    .toast-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      object-fit: cover;
      border: 2px solid white;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
  `
  document.head.appendChild(style)

  document.body.appendChild(toast)

  setTimeout(() => {
    toast.style.transform = 'translateX(0)'
  }, 100)

  setTimeout(() => {
    toast.style.transform = 'translateX(450px)'
    setTimeout(() => {
      if (document.body.contains(toast)) {
        document.body.removeChild(toast)
      }
      if (document.head.contains(style)) {
        document.head.removeChild(style)
      }
    }, 300)
  }, 4000)
}
// Storage functions
const saveNotificationsToStorage = () => {
  try {
    const toSave = notifications.value.slice(0, 20).map(n => ({
      ...n,
      isNew: false
    }))
    localStorage.setItem('admin_notifications', JSON.stringify(toSave))
  } catch (error) {
    console.warn('Could not save notifications to localStorage:', error)
  }
}

const loadNotificationsFromStorage = () => {
  try {
    const stored = localStorage.getItem('admin_notifications')
    if (stored) {
      const parsed = JSON.parse(stored)
      const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000)
      const recentNotifications = parsed.filter(n =>
          new Date(n.createdAt) > oneDayAgo
      )
      notifications.value = recentNotifications
      unreadCount.value = recentNotifications.filter(n => n.isNew).length
    }
  } catch (error) {
    console.warn('Could not load notifications from localStorage:', error)
  }
}

// Utility functions
const getStatusText = (status) => {
  const statusMap = {
    'PENDING': 'Chờ xử lý',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao',
    'DELIVERED': 'Đã giao',
    'CANCELLED': 'Đã hủy',
    'COMPLETED': 'Đã hoàn thành',
    'RETURNED': 'Đã trả'
  }
  return statusMap[status] || 'Không xác định'
}

const getStatusColor = (status) => {
  const colorMap = {
    'PENDING': 'warning',
    'CONFIRMED': 'info',
    'PROCESSING': 'primary',
    'SHIPPING': 'success',
    'DELIVERED': 'success',
    'CANCELLED': 'danger',
    'RETURNED': 'secondary'
  }
  return colorMap[status] || 'secondary'
}

const formatDateTime = (dateString) => {
  if (!dateString) return 'Chưa có'
  const date = new Date(dateString)
  const day = date.getDate().toString().padStart(2, '0')
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const year = date.getFullYear()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')

  return `${hours}:${minutes} ${day}/${month}/${year}`
}

// Đánh dấu đã đọc
const markAsRead = () => {
  notifications.value.forEach(n => n.isNew = false)
  unreadCount.value = 0
  saveNotificationsToStorage()
}

// Xử lý click notification
const handleNotificationClick = (notification) => {
  if (notification.orderId) {
    const url = `/admin/orders/${notification.orderId}`
    window.open(url, '_blank')
  } else if (notification.reviewId) {
    const url = `/admin/reviews`
    window.open(url, '_blank')
  }
  notification.isNew = false
  unreadCount.value = notifications.value.filter(n => n.isNew).length
  saveNotificationsToStorage()
}

// Clear all notifications
const clearAllNotifications = () => {
  notifications.value = []
  unreadCount.value = 0
  localStorage.removeItem('admin_notifications')
}

// Refresh notifications
const refreshNotifications = () => {
  isLoading.value = true
  // Simulate refresh
  setTimeout(() => {
    isLoading.value = false
  }, 1000)
}

// Lắng nghe sự kiện từ WebSocket

// Lắng nghe sự kiện từ WebSocket
onMounted(() => {
  loadNotificationsFromStorage()
  requestNotificationPermission()
  // Lắng nghe sự kiện 'order' từ EventBus
  emitter.on('order', (data) => {
    addNotification(data)
  })
  // Lắng nghe sự kiện 'review' từ EventBus
  emitter.on('review', (data) => {
    addReviewNotification(data)
  })
})

onBeforeUnmount(() => {
  emitter.off('order')
  saveNotificationsToStorage()
})

const addReviewNotification = (data) => {
  const { payload: review, action } = data
  if (action !== 'REVIEW_CREATED') return

  // Ưu tiên lấy đúng trường tên sản phẩm
  const productName =
    review.productName ||
    review.product?.name ||
    review.product?.productName ||
    review.productId ||
    ''
  let message = `Đánh giá mới cho sản phẩm: ${productName}`
  if (review.content) {
    message += `: "${review.content}"`
  }

  const notification = {
    id: `review_${review.id}_${Date.now()}_${Math.random()}`,
    reviewId: review.id,
    user: review.user?.fullname || review.userName || 'Khách hàng',
    time: new Date().toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }),
    avatar: review.user?.avatar || null,
    productId: review.product?.id || review.productId || '',
    productName: productName,
    isNew: true,
    message: message,
    reviewData: review
  }

  notifications.value.unshift(notification)
  if (notifications.value.length > 50) {
    notifications.value = notifications.value.slice(0, 50)
  }
  unreadCount.value = notifications.value.filter(n => n.isNew).length
  showToast(notification)
  playNotificationSound()
  showDesktopNotification(notification)
  saveNotificationsToStorage()
}
</script>

<template>
  <nav class="navbar navbar-top navbar-expand navbar-dashboard navbar-dark ps-0 pe-2 pb-0">
    <div class="container-fluid px-0">
      <div class="d-flex justify-content-end w-100" id="navbarSupportedContent">
        <ul class="navbar-nav align-items-center">
          <li class="nav-item dropdown">
            <a class="nav-link text-dark notification-bell dropdown-toggle position-relative"
               :class="{ 'unread': unreadCount > 0 }"
               href="#" role="button" data-bs-toggle="dropdown"
               data-bs-display="static" aria-expanded="false">
              <svg class="icon icon-sm text-gray-900" fill="currentColor" viewBox="0 0 20 20">
                <path
                    d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6zM10 18a3 3 0 01-3-3h6a3 3 0 01-3 3z"></path>
              </svg>
              <span v-if="unreadCount > 0"
                    class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger notification-badge">
                {{ unreadCount > 99 ? '99+' : unreadCount }}
              </span>
            </a>

            <div class="dropdown-menu dropdown-menu-lg dropdown-menu-end mt-2 py-0 notification-dropdown">
              <div
                  class="dropdown-header d-flex justify-content-between align-items-center px-3 py-3 border-bottom bg-light">
                <h6 class="mb-0 fw-bold">Thông báo đơn hàng</h6>
                <div class="d-flex align-items-center gap-2">
                  <span v-if="unreadCount > 0" class="badge bg-primary rounded-pill px-2 py-2">
                    {{ unreadCount }}
                  </span>
                  <button v-if="notifications.length > 0" class="btn btn-sm btn-outline-secondary"
                          @click="clearAllNotifications" title="Xoá tất cả">
                    <i class="fas fa-trash-alt"></i>
                  </button>
                  <button class="btn btn-sm btn-outline-primary" @click="refreshNotifications" :disabled="isLoading"
                          title="Tải lại">
                    <i class="fas fa-refresh" :class="{ 'fa-spin': isLoading }"></i>
                  </button>
                </div>
              </div>

              <div class="notification-list">
                <div v-if="isLoading" class="text-center py-4">
                  <div class="spinner-border spinner-border-sm text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                  </div>
                  <p class="mb-0 mt-2 text-muted">Đang tải đơn hàng...</p>
                </div>

                <div v-else-if="notifications.length === 0" class="text-center py-5 text-muted">
                  <div class="mb-3">
                    <svg class="icon icon-xl text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                      <path
                          d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6z"></path>
                    </svg>
                  </div>
                  <p class="mb-2 fw-medium">Chưa có thông báo mới</p>
                  <small class="text-muted">Thông báo đơn hàng sẽ xuất hiện tại đây</small>
                </div>

                <div v-for="(noti, index) in notifications" :key="noti.id"
                     class="notification-item position-relative"
                     @click="handleNotificationClick(noti)">

                  <div v-if="noti.isNew" class="new-indicator position-absolute top-0 start-0"></div>

                  <div class="notification-content p-3">
                    <div class="d-flex">
                      <div class="flex-shrink-0 me-3">
                        <div class="avatar-container position-relative">
                          <img v-if="noti.avatar"
                               :src="noti.avatar"
                               :alt="noti.user"
                               class="avatar-md rounded-circle"
                               @error="$event.target.style.display='none';"/>
                          <div v-else class="avatar-placeholder d-flex align-items-center justify-content-center">
                            <i class="fas fa-user text-muted"></i>
                          </div>
                        </div>
                      </div>

                      <div class="flex-grow-1 min-w-0">
                        <div class="d-flex justify-content-between align-items-start mb-1">
                          <div class="d-flex align-items-center flex-wrap">
                            <h6 class="mb-0 text-dark fw-bold me-2">{{ noti.user }}</h6>
                            <span class="badge rounded-pill px-2 py-1"
                                  :class="`bg-${getStatusColor(noti.status)}-subtle text-${getStatusColor(noti.status)}`">
                              {{ getStatusText(noti.status) }}
                            </span>
                          </div>
                          <small class="text-muted text-nowrap ms-2">{{ formatDateTime(noti.createdAt) }}</small>
                        </div>

                        <p class="mb-2 text-muted notification-message">{{ noti.message }}</p>

                        <div class="notification-details mb-2">
                          <div class="row g-2">
                            <div class="col-6" v-if="noti.phone">
                              <small class="text-muted d-flex align-items-center">
                                <i class="fas fa-phone me-1"></i>
                                {{ noti.phone }}
                              </small>
                            </div>
                            <div class="col-6" v-if="noti.paymentMethod">
                              <small class="text-info d-flex align-items-center">
                                <i class="fas fa-credit-card me-1"></i>
                                {{ noti.paymentMethod }}
                              </small>
                            </div>
                            <div class="col-6" v-if="noti.itemCount > 0">
                              <small class="text-muted d-flex align-items-center">
                                <i class="fas fa-shopping-cart me-1"></i>
                                {{ noti.itemCount }} sản phẩm
                              </small>
                            </div>
                            <div class="col-6" v-if="noti.orderValue > 0">
                              <small class="text-success fw-bold">
                                {{ formatCurrency(noti.orderValue) }}
                              </small>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="notifications.length > 5" class="dropdown-footer text-center py-2 border-top bg-light">
                <small class="text-muted">
                  Hiển thị {{ Math.min(notifications.length, 50) }} thông báo gần nhất
                </small>
              </div>
            </div>
          </li>

          <!-- User dropdown -->
          <li class="nav-item dropdown ms-lg-3">
            <a class="nav-link dropdown-toggle pt-1 px-0" href="#" role="button"
               data-bs-toggle="dropdown" aria-expanded="false">
              <div class="media d-flex align-items-center">
                <img class="avatar rounded-circle" alt=""
                     :src="user.avatar"/>
                <div class="media-body ms-2 text-dark align-items-center d-none d-lg-block">
                  <span class="mb-0 font-small fw-bold text-gray-900"> {{user.fullname}} </span>
                </div>
              </div>
            </a>
            <div class="dropdown-menu dashboard-dropdown dropdown-menu-end mt-2 py-1">
              <router-link to="/admin/profile" class="dropdown-item d-flex align-items-center">
                <svg class="dropdown-icon text-gray-400 me-2" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd"
                        d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-6-3a2 2 0 11-4 0 2 2 0 014 0zm-2 4a5 5 0 00-4.546 2.916A5.986 5.986 0 0010 16a5.986 5.986 0 004.546-2.084A5 5 0 0010 11z"
                        clip-rule="evenodd"></path>
                </svg>
                My Profile
              </router-link>
              <div role="separator" class="dropdown-divider my-1"></div>
              <a class="dropdown-item d-flex align-items-center" type="button" @click="handleLogout">
                <svg class="dropdown-icon text-danger me-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3v1"></path>
                </svg>
                Đăng xuất
              </a>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </nav>
</template>

<style scoped>
/* Fixed dropdown positioning and width */
.notification-dropdown {
  width: 450px !important;
  max-width: 95vw;
  right: 0 !important;
  left: auto !important;
  transform: none !important;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  border: none;
  border-radius: 12px;
}

.notification-list {
  max-height: 500px;
  overflow-y: auto;
}

.notification-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
  border-bottom: 1px solid #f1f3f4;
}

.notification-item:hover {
  background-color: #f8f9fa;
  border-left-color: #007bff;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-content {
  position: relative;
}

.new-indicator {
  width: 4px;
  height: 100%;
  background: linear-gradient(45deg, #007bff, #28a745);
  animation: newNotificationGlow 2s ease-in-out infinite alternate;
}

@keyframes newNotificationGlow {
  0% {
    opacity: 0.6;
  }
  100% {
    opacity: 1;
  }
}

.avatar-container {
  width: 60px;
  height: 45px;
}

.avatar-md {
  width: 45px;
  height: 45px;
  object-fit: cover;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-placeholder {
  width: 60px;
  height: 45px;
  background-color: #e9ecef;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar {
  width: 37px;
  height: 35px;
}

.notification-message {
  font-size: 13px;
  line-height: 1.4;
}

.notification-details small {
  font-size: 11px;
  line-height: 1.3;
}

.notification-bell {
  position: relative;
  transition: all 0.3s ease;
  margin-right: -10px;
}

.notification-badge {
  animation: bounceIn 0.5s ease-out;
  box-shadow: 0 2px 4px rgba(220, 53, 69, 0.3);
  font-size: 10px;
  min-width: 18px;
  height: 18px;
}

/* Scrollbar styling */
.notification-list::-webkit-scrollbar {
  width: 6px;
}

.notification-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.notification-list::-webkit-scrollbar-thumb {
  background: linear-gradient(45deg, #007bff, #6c757d);
  border-radius: 3px;
}

.notification-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(45deg, #0056b3, #5a6268);
}

/* Animation cho notification mới */
.notification-item:first-child {
  animation: slideInFromTop 0.5s ease-out;
}

@keyframes slideInFromTop {
  0% {
    opacity: 0;
    transform: translateY(-20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Bootstrap utility classes */
.bg-primary-subtle {
  background-color: rgba(13, 110, 253, 0.1) !important;
}

.bg-success-subtle {
  background-color: rgba(25, 135, 84, 0.1) !important;
}

.bg-warning-subtle {
  background-color: rgba(255, 193, 7, 0.1) !important;
}

.bg-danger-subtle {
  background-color: rgba(220, 53, 69, 0.1) !important;
}

.bg-info-subtle {
  background-color: rgba(13, 202, 240, 0.1) !important;
}

.bg-secondary-subtle {
  background-color: rgba(108, 117, 125, 0.1) !important;
}

/* Responsive */
@media (max-width: 768px) {
  .dropdown-menu-lg {
    min-width: 320px;
    max-width: 90vw;
  }

  .notification-details {
    font-size: 10px;
  }
}
</style>