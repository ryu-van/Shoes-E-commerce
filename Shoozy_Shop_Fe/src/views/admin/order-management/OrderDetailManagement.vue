<script setup>
import {ref, computed, onMounted, onBeforeUnmount} from 'vue'
import {getOrderById, updateOrderStatus as updateOrderStatusAPI, getOrderTimelinesByOrderId, updateCustomerInfo, deleteOrderById} from "@/service/OrderApi.js";
import ShowToastComponent from "@/components/ShowToastComponent.vue";
import ListAddressModal from "@/components/ListAddressModal.vue";
import {useAuthStore} from "@/stores/Auth.js";
import apiClient from "@/auth/api.js";
import {
  connectWebSocket,
  addCouponListener,
  removeCouponListener
} from '@/service/Websocket'

const props = defineProps({
  id: {
    type: [Number, String],
    required: true
  }
})

const toastRef = ref(null);

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type);
};

const isEditingUserInformation = ref(false)
const orderData = ref(null)
const currentStatus = ref('PENDING')
const showStatusModal = ref(false)
const showConfirmModal = ref(false)
const showCancelConfirmModal = ref(false)
const isUpdatingStatus = ref(false)
const nextStatusToUpdate = ref('')
const statusDescription = ref('')

// Address modal related refs
const showAddressModal = ref(false)
const selectedAddress = ref(null)

const showHistoryModal = ref(false)
const orderTimelines = ref([])
const isLoadingTimelines = ref(false)

// Status steps mapping for Vietnamese
const statusSteps = ref([
  {key: 'PENDING', label: 'Chờ xác nhận', icon: 'fas fa-clock', date: null},
  {key: 'CONFIRMED', label: 'Đã xác nhận', icon: 'fas fa-check-circle', date: null},
  {key: 'PROCESSING', label: 'Đang đóng gói', icon: 'fas fa-cog', date: null},
  {key: 'SHIPPING', label: 'Đang vận chuyển', icon: 'fas fa-truck', date: null},
  {key: 'DELIVERED', label: 'Đã giao', icon: 'fas fa-check-circle', date: null},
  {key: 'COMPLETED', label: 'Đã hoàn thành', icon: 'fas fa-star', date: null},
  {key: 'CANCELLED', label: 'Đã hủy', icon: 'fas fa-times-circle', date: null}
])

const getOrderTypeText = (type) => {
  switch (type) {
    case 'ORDER_CREATED':
      return 'Tạo đơn hàng'
    case 'ORDER_UPDATE':
      return 'Cập nhật đơn hàng'
    case 'ORDER_CANCELLED':
      return 'Hủy đơn hàng'
    case 'ORDER_RETURNED':
      return 'Trả hàng'
    default:
      return 'Không xác định'
  }
}

const currentStatusIndex = computed(() => {
  return statusSteps.value.findIndex(step => step.key === currentStatus.value)
})

// Check if order can be cancelled
const canCancelOrder = computed(() => {
  return ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(currentStatus.value)
})

// Check if customer info can be edited
const canEditCustomerInfo = computed(() => {
  return ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(currentStatus.value)
})

// Check if continue button should be shown
const shouldShowContinueButton = computed(() => {
  return currentStatus.value !== 'COMPLETED' && currentStatus.value !== 'CANCELLED'
})

// Get button text based on current status
const getButtonText = () => {
  if (currentStatus.value === 'PENDING') {
    return 'XÁC NHẬN'
  }
  return 'TIẾP TỤC'
}

// Get default description based on status transition
const getDefaultDescription = (fromStatus, toStatus) => {
  const fromLabel = statusSteps.value.find(step => step.key === fromStatus)?.label || fromStatus
  const toLabel = statusSteps.value.find(step => step.key === toStatus)?.label || toStatus
  return `Chuyển trạng thái từ ${fromLabel} sang ${toLabel}`
}

// Handle button click - show confirmation modal for all statuses
const handleStatusButtonClick = () => {
  const nextStatus = getNextStatus()
  if (!nextStatus) return

  nextStatusToUpdate.value = nextStatus
  statusDescription.value = getDefaultDescription(currentStatus.value, nextStatus)
  showConfirmModal.value = true
}

// Handle cancel button click
const handleCancelButtonClick = () => {
  nextStatusToUpdate.value = 'CANCELLED'
  statusDescription.value = 'Hủy đơn hàng theo yêu cầu'
  showCancelConfirmModal.value = true
}

// Confirm status update
const confirmStatusUpdate = () => {
  const description = statusDescription.value.trim() || getDefaultDescription(currentStatus.value, nextStatusToUpdate.value)
  updateOrderStatus(nextStatusToUpdate.value, description)
  showConfirmModal.value = false
  statusDescription.value = ''
}

// Confirm cancel order
const confirmCancelOrder = async () => {
  try {
    isUpdatingStatus.value = true
    // Gọi API hủy đơn hàng
    const response = await deleteOrderById(props.id)

    if (response.success || response.status === 200) {
      currentStatus.value = 'CANCELLED'
      if (orderData.value) {
        orderData.value.status = 'CANCELLED'
      }
      // Tạo timeline cho việc hủy đơn hàng
      await createOrderTimeline('CANCELLED', statusDescription.value || 'Hủy đơn hàng theo yêu cầu')
      // Refresh dữ liệu từ server
      await fetchOrderById(props.id)
      showToast('Hủy đơn hàng thành công', 'success')
    } else {
      throw new Error(response.message || 'Hủy đơn hàng thất bại')
    }
  } catch (error) {
    console.error('Error cancelling order:', error)
  } finally {
    isUpdatingStatus.value = false
    showCancelConfirmModal.value = false
    statusDescription.value = ''
  }
}

// Cancel confirmation
const cancelConfirmation = () => {
  showConfirmModal.value = false
  showCancelConfirmModal.value = false
  statusDescription.value = ''
  nextStatusToUpdate.value = ''
}

// Updated function to call real API
const updateOrderStatus = async (newStatus, description = '') => {
  if (isUpdatingStatus.value) return
  try {
    isUpdatingStatus.value = true
    const response = await updateOrderStatusAPI(props.id, newStatus, description)
    if (response.success || response.status === 200) {
      currentStatus.value = newStatus
      if (orderData.value) {
        orderData.value.status = newStatus
      }

      // Update status step date
      const statusIndex = statusSteps.value.findIndex(step => step.key === newStatus)
      if (statusIndex >= 0) {
        const now = new Date()
        const formattedDate = formatDateTime(now.toISOString())
        statusSteps.value[statusIndex].date = formattedDate
      }

      await createOrderTimeline(newStatus, description)
      fetchOrderById(props.id); // Refresh order data to reflect changes
      showStatusModal.value = false

      const action = newStatus === 'CANCELLED' ? 'hủy' : 'cập nhật trạng thái'
      showToast(`${action} đơn hàng thành công`, 'success')
    } else {
      throw new Error(response.message || 'Cập nhật trạng thái thất bại')
    }
  } catch (error) {
    console.error('Error updating order status:', error)
    const action = nextStatusToUpdate.value === 'CANCELLED' ? 'hủy' : 'cập nhật trạng thái'
    showToast(`${action} đơn hàng thất bại`, 'error')
  } finally {
    isUpdatingStatus.value = false
  }
}

const createOrderTimeline = async (statusKey, description = '') => {
  const authStore = useAuthStore()
  try {
    await apiClient.post('/order-timeline', {
      orderId: props.id,
      userId: authStore.user.id,
      type: statusKey,
      description
    })
    console.log('Tạo order timeline thành công')
  } catch (e) {
    console.error('Tạo order timeline thất bại:', e)
  }
}

const subtotal = computed(() => {
  if (!orderData.value || !orderData.value.orderDetails) return 0
  return orderData.value.orderDetails.reduce((sum, item) => sum + item.totalMoney, 0)
})

const shippingFee = computed(() => {
  return orderData.value?.shippingFee || 0
})

const couponDiscount = computed(() => {
  return orderData.value?.couponDiscountAmount || 0
})

const finalTotal = computed(() => {
  return orderData.value?.finalPrice || 0
})

const amountPaid = computed(() => {
  if (!orderData.value || !orderData.value.transactions) return 0
  return orderData.value.transactions
      .filter(transaction => transaction.status === 'SUCCESS')
      .reduce((sum, transaction) => sum + transaction.amount, 0)
})

const remainingAmount = computed(() => {
  return Math.max(0, finalTotal.value - amountPaid.value)
})

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

const formatCurrency = (amount) => {
  if (!amount) return '0 ₫'
  return new Intl.NumberFormat('vi-VN').format(amount) + ' ₫'
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('vi-VN')
}

const getStatusClass = (index) => {
  const currentIndex = currentStatusIndex.value

  // Special handling for CANCELLED status
  if (currentStatus.value === 'CANCELLED') {
    return statusSteps.value[index].key === 'CANCELLED' ? 'completed cancelled' : 'pending'
  }

  return index <= currentIndex ? 'completed' : 'pending'
}

const getLineClass = (index) => {
  const currentIndex = currentStatusIndex.value;
  // Nếu đơn hàng bị hủy
  if (orderData.value?.status === 'CANCELLED') {
    return 'line-cancelled';
  }
  // Chỉ bôi đen đường line khi bước hiện tại đã hoàn thành
  // và bước tiếp theo cũng đã hoàn thành
  if (index < currentIndex) {
    return 'line-completed';
  }
  return ''; // Giữ màu xám mặc định
}

const canUpdateStatus = (statusKey) => {
  if (statusKey === 'CANCELLED') {
    return currentStatus.value !== 'CANCELLED'
  }
  if (currentStatus.value === 'CANCELLED') {
    return false
  }
  const index = statusSteps.value.findIndex(step => step.key === statusKey)
  const currentIndex = currentStatusIndex.value
  return index >= currentIndex
}

const getNextStatus = () => {
  const currentIndex = currentStatusIndex.value
  if (currentIndex < statusSteps.value.length - 1) {
    return statusSteps.value[currentIndex + 1].key
  }
  return null
}

const initializeStatusDates = () => {
  if (!orderData.value) return
  const currentIndex = currentStatusIndex.value
  if (currentIndex >= 0) {
    statusSteps.value[0].date = formatDateTime(orderData.value.createdAt)
  }
  if (currentIndex > 0) {
    statusSteps.value[currentIndex].date = formatDateTime(orderData.value.updatedAt)
  }
  if (orderData.value.shippingDate && currentIndex >= 3) {
    statusSteps.value[3].date = formatDateTime(orderData.value.shippingDate)
  }
}

const fetchOrderById = async (id) => {
  try {
    const numericId = typeof id === 'string' ? parseInt(id, 10) : id
    const res = await getOrderById(numericId)
    if (res.data && res.data.status === 200) {
      console.log('Order data fetched successfully:', res.data.data)
      orderData.value = res.data.data
      currentStatus.value = orderData.value.status
      initializeStatusDates()
    } else {
      throw new Error(res.data?.message || 'Lỗi khi tải dữ liệu')
    }
  } catch (error) {
    console.error('Error when fetching order detail: ', error)
    showToast('Lỗi khi tải thông tin đơn hàng', 'error')
  }
}

const fetchOrderTimelines = async () => {
  try {
    isLoadingTimelines.value = true
    const data = await getOrderTimelinesByOrderId(props.id)
    orderTimelines.value = data
  } catch (error) {
    console.error('Error fetching order timelines:', error)
    showToast('Lỗi khi tải lịch sử hóa đơn', 'error')
  } finally {
    isLoadingTimelines.value = false
  }
}

const handleHistoryClick = async () => {
  showHistoryModal.value = true
  await fetchOrderTimelines()
}

const closeHistoryModal = () => {
  showHistoryModal.value = false
  orderTimelines.value = []
}

// Address modal handlers
const handleAddressEdit = () => {
  showAddressModal.value = true
}

const handleAddressModalClose = () => {
  showAddressModal.value = false
}

const handleAddressModalSave = (addressData) => {
  if (addressData && addressData.data) {
    selectedAddress.value = addressData.data
    // Update the order data with the selected address
    if (orderData.value) {
      orderData.value.address = addressData.data.line || addressData.data.addressDetail
    }
    showAddressModal.value = false
  }
}

// Event handler cho WebSocket coupon messages
const handleCouponUpdate = (data) => {
  console.log('Received coupon update via WebSocket in OrderDetail:', data);
  
  // Xử lý cập nhật coupon quantity khi có đơn hàng sử dụng coupon
  if (data?.type === 'coupon' && data?.action === 'DECREMENT') {
    const { couponId, quantity, status, code } = data.payload || {}
    
    // Cập nhật thông tin coupon trong orderData nếu đơn hàng này sử dụng coupon
    if (orderData.value?.coupon && 
        (orderData.value.coupon.id === couponId || orderData.value.coupon.code === code)) {
      if (typeof quantity !== 'undefined') orderData.value.coupon.quantity = quantity
      if (typeof status !== 'undefined') orderData.value.coupon.status = status
      
      // Hiển thị thông báo cập nhật
      if (code) {
        showToast(`Cập nhật mã giảm giá ${code}: còn ${quantity} lượt sử dụng`, 'success')
      }
    }
  }
}

onMounted(() => {
  fetchOrderById(props.id)
  
  // Kết nối WebSocket và đăng ký listener cho coupon updates
  connectWebSocket();
  addCouponListener(handleCouponUpdate);
})

onBeforeUnmount(() => {
  // Gỡ bỏ listener khi component bị unmount
  removeCouponListener(handleCouponUpdate);
})

const handleUpdate = async () => {
  try {
    console.log('Updating customer info:', orderData.value)
    await updateCustomerInfo(orderData.value.id, { ...orderData.value });
    fetchOrderById(props.id)
    showToast('Cập nhật thông tin thành công', 'success')
    isEditingUserInformation.value = false
  } catch (error) {
    showToast('Cập nhật thất bại', 'error')
    console.error(error)
  }
}
</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <!-- Loading state -->
    <div v-if="!orderData" class="loading-container">
      <div class="loading-spinner"></div>
      <p>Đang tải thông tin đơn hàng...</p>
    </div>

    <!-- Main content - only show when data is loaded -->
    <template v-else>
      <!-- Header -->
      <div class="order-header mb-4">
        <div class="header-left">
          <h2 class="fw-extrabold mb-2">📋 Chi tiết đơn hàng {{ orderData.orderCode }}</h2>
          <p class="order-date text-muted">Ngày đặt: {{ formatDate(orderData.createdAt) }}</p>
          <p class="order-date text-muted">Loại: {{ orderData.type == 1 ? 'ONLINE' : "OFFLINE" }}</p>
        </div>
      </div>

      <!-- Order Status Timeline -->
      <div class="status-section mb-4">
        <h3 class="section-title">🔄 Trạng thái đơn hàng</h3>
        <div class="status-timeline">
          <div v-for="(step, index) in statusSteps" :key="step.key" class="status-step">
            <div class="status-icon" :class="getStatusClass(index)">
              <i :class="step.icon"></i>
            </div>
            <div class="status-info">
              <div class="status-label">{{ step.label }}</div>
              <div class="status-date">{{ step.date || 'Chưa có' }}</div>
            </div>
            <!-- Đường kẻ ngang giữa các bước -->
            <div v-if="index < statusSteps.length - 1 && step.key !== 'CANCELLED'"
                 class="status-line"
                 :class="getLineClass(index)">
            </div>
          </div>
        </div>

        <!-- Status Action Buttons -->
        <div class="status-actions d-flex justify-content-between">
          <div class="d-flex gap-3">
            <!-- Continue/Next Status Button -->
            <button
                v-if="shouldShowContinueButton && getNextStatus()"
                class="btn btn-primary"
                :disabled="isUpdatingStatus"
                @click="handleStatusButtonClick"
            >
              {{ isUpdatingStatus ? 'ĐANG XỬ LÝ...' : getButtonText() }}
            </button>

            <!-- Cancel Order Button -->
            <button
                v-if="canCancelOrder"
                class="btn btn-danger"
                :disabled="isUpdatingStatus"
                @click="handleCancelButtonClick"
            >
              {{ isUpdatingStatus ? 'ĐANG XỬ LÝ...' : 'HỦY ĐƠN HÀNG' }}
            </button>
          </div>

          <button class="btn btn-primary" @click="handleHistoryClick">
            LỊCH SỬ ĐƠN HÀNG
          </button>
        </div>
      </div>

      <!-- Payment History Section -->
      <div class="payment-section mb-4">
        <h3 class="section-title">💳 Lịch sử thanh toán</h3>
        <div class="table-responsive">
          <table class="table table-hover table-bordered align-middle">
            <thead class="table-dark">
            <tr>
              <th class="text-center" style="width: 5%">STT</th>
              <th class="text-center">Hình thức giao dịch</th>
              <th class="text-center">Phương thức thanh toán</th>
              <th class="text-center">Trạng thái thanh toán</th>
              <th class="text-center">Thời gian</th>
              <th class="text-center">Tổng tiền</th>
              <th class="text-center">Ghi chú</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(transaction, index) in orderData.transactions" :key="transaction.id">
              <td class="text-center">{{ index + 1 }}</td>
              <td class="text-center">
                <span class="badge" :class="orderData.paymentMethod.id === 2 ? 'bg-success' : 'bg-danger'">
                  {{ orderData.paymentMethod.id === 1 ? 'OFFLINE' : 'ONLINE' }}
                </span>
              </td>
              <td class="text-center">{{ orderData.paymentMethod.name }}</td>
              <td class="text-center">
                <span v-if="transaction.status === 'SUCCESS'" class="badge bg-success">
                  <i class="fas fa-check me-1"></i>Đã thanh toán
                </span>
                <span v-else-if="transaction.status === 'PENDING'" class="badge bg-warning">
                  <i class="fas fa-clock me-1"></i>Chờ thanh toán
                </span>
                <span v-else-if="transaction.status === 'FAILED'" class="badge bg-danger">
                  <i class="fas fa-times me-1"></i>Thất bại
                </span>
                <span v-else-if="transaction.status === 'CANCELLED'" class="badge bg-secondary">
                  <i class="fas fa-ban me-1"></i>Đã hủy
                </span>
                <span v-else class="badge bg-info">
                  {{ transaction.status }}
                </span>
              </td>
              <td class="text-center">{{ formatDateTime(transaction.completedDate) }}</td>
              <td class="text-center">{{ formatCurrency(transaction.amount) }}</td>
              <td class="text-center">{{ transaction.note || 'Không có ghi chú' }}</td>
            </tr>

            <!-- Show message if no transactions -->
            <tr v-if="!orderData.transactions || orderData.transactions.length === 0">
              <td colspan="7" class="text-center text-muted">
                Không có giao dịch nào
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Order Information Section -->
      <div class="order-info-section mb-4">
        <h3 class="section-title">📦 Thông tin sản phẩm đã mua</h3>

        <!-- Products Table -->
        <div class="table-responsive">
          <table class="table table-hover table-bordered align-middle">
            <thead class="table-dark">
            <tr>
              <th class="text-center" style="width: 5%">STT</th>
              <th class="text-center">Ảnh sản phẩm</th>
              <th class="text-center">Sản phẩm</th>
              <th class="text-center">Số lượng sản phẩm</th>
              <th class="text-center">Thời gian</th>
              <th class="text-center">Đơn giá</th>
              <th class="text-center">Tổng tiền</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(item, index) in orderData.orderDetails || []" :key="item.id">
              <td class="text-center">{{ index + 1 }}</td>
              <td class="text-center">
                <img :src="item.thumbnail || '/404.jpg'" :alt="'Product ' + (index + 1)" class="product-image"
                     width="80" height="80">
              </td>
              <td>
                <div class="product-info">
                  <div class="product-name fw-bold">{{ item.productName }}</div>
                  <div class="product-details text-muted small">
                    Size: {{ item.size || 'N/A' }} |
                    Màu: {{ item.color || 'N/A' }}
                  </div>
                </div>
              </td>
              <td class="text-center">
                {{ item.quantity }}
              </td>
              <td class="text-center">{{ formatDateTime(orderData.createdAt) }}</td>
              <td class="text-center">{{ formatCurrency(item.price) }}</td>
              <td class="text-center fw-bold">{{ formatCurrency(item.totalMoney) }}</td>
            </tr>
            <tr v-if="!orderData.orderDetails || orderData.orderDetails.length === 0">
              <td colspan="7" class="text-center">Không có sản phẩm nào.</td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- Customer Information and Invoice Summary -->
        <div class="row mt-4">
          <div class="col-md-6">
            <div class="customer-info">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h4 class="section-title mb-0">Thông tin khách hàng</h4>
                <!-- Only show edit button if customer info can be edited -->
                <button
                    v-if="canEditCustomerInfo"
                    class="btn btn-primary"
                    @click="isEditingUserInformation = !isEditingUserInformation"
                >
                  {{ isEditingUserInformation ? 'HỦY' : 'CHỈNH SỬA' }}
                </button>
              </div>

              <div class="info-group">
                <label>Họ tên</label>
                <input type="text" class="form-control" v-model="orderData.fullName"
                       :readonly="!isEditingUserInformation">
              </div>

              <div class="info-group">
                <label>Số điện thoại</label>
                <input type="text" class="form-control" v-model="orderData.phoneNumber"
                       :readonly="!isEditingUserInformation">
              </div>

              <div class="info-group">
                <label>Địa chỉ giao hàng</label>
                <div class="address-input-group">
                  <div class="form-control address-display" :class="{ 'editable': isEditingUserInformation }">
                    {{ orderData.address || 'Chưa có địa chỉ' }}
                  </div>
                  <button
                      v-if="isEditingUserInformation"
                      type="button"
                      class="btn btn-outline-primary address-select-btn"
                      @click="handleAddressEdit"
                  >
                    <i class="fas fa-map-marker-alt me-1"></i>
                    Chọn địa chỉ
                  </button>
                </div>
              </div>

              <div class="info-group">
                <label>Phương thức thanh toán</label>
                <input type="text" class="form-control" v-model="orderData.paymentMethod.name" readonly>
              </div>

              <div class="info-group">
                <label>Ghi chú</label>
                <textarea class="form-control" rows="3" v-model="orderData.note"
                          :readonly="!isEditingUserInformation"></textarea>
              </div>

              <div v-if="isEditingUserInformation" class="text-end mt-3">
                <button class="btn btn-primary" @click="handleUpdate">CẬP NHẬT</button>
              </div>
            </div>

          </div>

          <div class="col-md-6">
            <div class="invoice-summary">
              <h4 class="section-title">Hóa đơn</h4>

              <div class="summary-row">
                <span>Tổng tiền sản phẩm:</span>
                <span>{{ formatCurrency(subtotal) }}</span>
              </div>

              <div class="summary-row" v-if="couponDiscount > 0">
                <span>Giảm giá ({{ orderData.coupon?.name }}):</span>
                <span class="text-success">- {{ formatCurrency(couponDiscount) }}</span>
              </div>

              <div class="summary-row" v-if="shippingFee > 0">
                <span>Phí vận chuyển:</span>
                <span class="text-danger">+ {{ formatCurrency(shippingFee) }}</span>
              </div>

              <div class="summary-row total">
                <span>Tổng cần thanh toán:</span>
                <span>{{ formatCurrency(finalTotal) }}</span>
              </div>

              <div class="summary-row">
                <span>Số tiền đã thanh toán:</span>
                <span class="text-success">{{ formatCurrency(amountPaid) }}</span>
              </div>

              <div class="summary-row" v-if="remainingAmount > 0">
                <span>Cần trả thêm:</span>
                <span class="text-danger">{{ formatCurrency(remainingAmount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Status Confirmation Modal -->
    <div v-if="showConfirmModal" class="modal-overlay" @click="cancelConfirmation">
      <div class="modal-dialog modal-lg" @click.stop>
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Xác nhận cập nhật trạng thái</h5>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <p class="mb-3">Bạn có chắc chắn muốn cập nhật trạng thái đơn hàng từ
                <strong>{{ statusSteps.find(s => s.key === currentStatus)?.label }}</strong>
                sang <strong>{{ statusSteps.find(s => s.key === nextStatusToUpdate)?.label }}</strong>?
              </p>

              <div class="form-group">
                <label for="statusDescription" class="form-label">Mô tả (tùy chọn):</label>
                <textarea
                    id="statusDescription"
                    class="form-control"
                    rows="3"
                    v-model="statusDescription"
                    placeholder="Nhập mô tả cho việc cập nhật trạng thái..."
                ></textarea>
                <small class="form-text text-muted">
                  Nếu để trống, hệ thống sẽ tự động tạo mô tả mặc định.
                </small>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="cancelConfirmation">Hủy</button>
            <button type="button" class="btn btn-primary" @click="confirmStatusUpdate" :disabled="isUpdatingStatus">
              {{ isUpdatingStatus ? 'Đang xử lý...' : 'Xác nhận' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Cancel Order Confirmation Modal -->
    <div v-if="showCancelConfirmModal" class="modal-overlay" @click="cancelConfirmation">
      <div class="modal-dialog modal-lg" @click.stop>
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title"> Xác nhận hủy đơn hàng</h5>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <div class="alert alert-warning">
                <i class="fas fa-exclamation-triangle me-2"></i>
                <strong>Cảnh báo:</strong> Bạn đang thực hiện hủy đơn hàng #{{ orderData.id }}.
                Hành động này không thể hoàn tác!
              </div>

              <div class="form-group">
                <label for="cancelDescription" class="form-label">Lý do hủy đơn hàng:</label>
                <textarea
                    id="cancelDescription"
                    class="form-control"
                    rows="3"
                    v-model="statusDescription"
                    placeholder="Nhập lý do hủy đơn hàng..."
                ></textarea>
                <small class="form-text text-muted">
                  Vui lòng nhập lý do hủy đơn hàng để ghi nhận.
                </small>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="cancelConfirmation">
              <i class="fas fa-times me-1"></i>Không hủy
            </button>
            <button type="button" class="btn btn-danger" @click="confirmCancelOrder" :disabled="isUpdatingStatus">
              <i class="fas fa-ban me-1"></i>
              {{ isUpdatingStatus ? 'Đang hủy...' : 'Xác nhận hủy' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Status Update Modal -->
    <div v-if="showStatusModal" class="modal-overlay" @click="!isUpdatingStatus && (showStatusModal = false)">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Cập nhật trạng thái đơn hàng</h5>
            <button type="button" class="btn-primary" @click="showStatusModal = false"
                    :disabled="isUpdatingStatus"></button>
          </div>
          <div class="modal-body">
            <div class="status-options">
              <div v-for="step in statusSteps" :key="step.key" class="status-option" :class="{
                    'active': step.key === currentStatus,
                    'disabled': !canUpdateStatus(step.key) || isUpdatingStatus,
                    'updating': isUpdatingStatus && step.key === currentStatus
                  }" @click="canUpdateStatus(step.key) && !isUpdatingStatus && updateOrderStatus(step.key)">
                <i :class="step.icon" class="me-3"></i>
                <span>{{ step.label }}</span>
                <i v-if="isUpdatingStatus && step.key === currentStatus" class="fas fa-spinner fa-spin ms-auto"></i>
              </div>
            </div>
            <div v-if="isUpdatingStatus" class="text-center mt-3">
              <small class="text-muted">Đang cập nhật trạng thái đơn hàng...</small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- History Modal -->
    <div v-if="showHistoryModal" class="modal-overlay" @click="closeHistoryModal">
      <div class="modal-dialog" style="max-width: 90vw; width: 60%" @click.stop>
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Lịch sử đơn hàng</h5>
            <button type="button" class="btn-close" @click="closeHistoryModal">&times;</button>
          </div>
          <div class="modal-body">
            <!-- Loading state -->
            <div v-if="isLoadingTimelines" class="text-center">
              <div class="loading-spinner mb-3"></div>
              <p>Đang tải lịch sử...</p>
            </div>

            <!-- History table -->
            <div v-else class="table-responsive">
              <table class="table table-hover table-bordered align-middle">
                <thead class="table-dark">
                <tr>
                  <th class="text-center" style="width: 5%">STT</th>
                  <th class="text-center">Hành động</th>
                  <th class="text-center">Ghi chú</th>
                  <th class="text-center">Ngày</th>
                  <th class="text-center">Người xác nhận</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(timeline, index) in orderTimelines" :key="timeline.id">
                  <td class="text-center">{{ index + 1 }}</td>
                  <td class="text-center">{{ getOrderTypeText(timeline.type) }}</td>
                  <td class="text-center">{{ timeline.description || 'Không có ghi chú' }}</td>
                  <td class="text-center">{{ formatDateTime(timeline.createDate) }}</td>
                  <td class="text-center">{{ timeline.userFullName || 'Hệ thống' }}</td>
                </tr>

                <!-- Empty state -->
                <tr v-if="orderTimelines.length === 0">
                  <td colspan="5" class="text-center text-muted">
                    Không có lịch sử nào
                  </td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeHistoryModal">Đóng</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Address Selection Modal -->
    <ListAddressModal
        v-if="showAddressModal"
        :userId="orderData?.userId || orderData?.customerId"
        @save="handleAddressModalSave"
        @close="handleAddressModalClose"
    />

    <ShowToastComponent ref="toastRef"/>
  </div>
</template>

<style scoped>
/* Loading styles */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e2e8f0;
  border-top: 4px solid #0d6efd;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-container p {
  color: #6c757d;
  font-size: 16px;
}

/* Header */
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left h2 {
  color: #212529;
  margin-bottom: 8px;
}

.order-date {
  font-size: 14px;
  margin-bottom: 0;
  color: #666;
}

.header-actions .btn {
  height: 38px;
}

/* Section Styles */
.status-section,
.payment-section,
.order-info-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

/* Status Timeline */
.status-timeline {
  display: flex;
  justify-content: space-between;
  position: relative;
  margin: 30px 0;
  align-items: flex-start;
}

.status-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  z-index: 2;
  flex: 1;
}

.status-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  margin-bottom: 10px;
  font-size: 16px;
  position: relative;
  z-index: 3;
}

.status-step .status-icon.completed {
  background: #000;
  color: white;
}

.status-step .status-icon.cancelled {
  background: #dc3545;
  color: white;
}

.status-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.status-label {
  font-size: 12px;
  font-weight: 600;
  color: #000;
  margin-bottom: 5px;
  white-space: nowrap;
}

.status-date {
  font-size: 11px;
  color: #666;
  white-space: nowrap;
}

/* Đường kẻ ngang giữa các status */
.status-line {
  position: absolute;
  top: 20px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: #e5e7eb;
  z-index: 1;
  transform: translateY(-50%);
}

/* Chỉ bôi đen đường line khi bước hiện tại đã hoàn thành */
.status-line.line-completed {
  background: #000;
}

.status-line.line-cancelled {
  background: #dc3545;
}

/* Đảm bảo đường line không hiển thị ở step cuối cùng */
.status-step:last-child .status-line {
  display: none;
}

/* Status Action Buttons */
.status-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
}

/* Tables */
.table-responsive {
  overflow-x: auto;
}

.table {
  border-collapse: collapse;
  margin-bottom: 0;
}

.table th,
.table td {
  padding: 12px;
  vertical-align: middle;
  border: 1px solid #dee2e6;
}

.table-dark th {
  background-color: #212529;
  color: white;
  font-weight: 600;
}

.table-hover tbody tr:hover {
  background-color: #f8f9fa;
}

/* Badges */
.badge {
  display: inline-block;
  padding: 6px 12px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: #fff;
  text-align: center;
  white-space: nowrap;
  vertical-align: baseline;
  border-radius: 4px;
}

.bg-success {
  background-color: #198754;
}

.bg-warning {
  background-color: #ffc107;
  color: #000;
}

.bg-danger {
  background-color: #dc3545;
}

.bg-secondary {
  background-color: #6c757d;
}

.bg-info {
  background-color: #0dcaf0;
  color: #000;
}

/* Product Image */
.product-image {
  border-radius: 4px;
  object-fit: cover;
  border: 1px solid #dee2e6;
}

/* Product Info */
.product-info {
  text-align: left;
}

.product-name {
  font-size: 14px;
  margin-bottom: 4px;
}

.product-details {
  font-size: 12px;
}

/* Customer Info */
.customer-info {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.info-group {
  margin-bottom: 15px;
}

.info-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 5px;
  color: #333;
  font-size: 14px;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}

.form-control:focus {
  outline: none;
  border-color: #1F2937;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-control:read-only {
  background-color: #e9ecef;
  opacity: 1;
}

/* Address Input Group Styles */
.address-input-group {
  display: flex;
  gap: 8px;
  align-items: stretch;
}

.address-display {
  flex: 1;
  min-height: 38px;
  display: flex;
  align-items: center;
  background-color: #f8f9fa;
  border: 1px solid #ced4da;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 14px;
  color: #495057;
}

.address-display.editable {
  background-color: #fff;
  border-color: #0d6efd;
}

.address-select-btn {
  background-color: #333 !important;
  flex-shrink: 0;
  padding: 8px 16px;
  font-size: 14px;
  white-space: nowrap;
  color: white;
}

.btn-primary:active {
  background-color: #3e3e3e !important;
  border: 0px !important;
}

/* Invoice Summary */
.invoice-summary {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #dee2e6;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-row.total {
  font-weight: 600;
  font-size: 16px;
  background: #e9ecef;
  margin: 10px -20px;
  padding: 12px 20px;
  border-bottom: none;
}

.text-success {
  color: #198754;
}

.text-danger {
  color: #dc3545;
}

.text-muted {
  color: #6c757d;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1050;
}

.modal-dialog {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-dialog.modal-lg {
  max-width: 600px;
}

.modal-content {
  border-radius: 8px;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  color: #6c757d;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  color: #000;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  padding: 20px;
  border-top: 1px solid #dee2e6;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Form Elements in Modal */
.form-group {
  margin-bottom: 15px;
}

.form-label {
  display: block;
  font-weight: 500;
  margin-bottom: 5px;
  color: #333;
  font-size: 14px;
}

.form-text {
  font-size: 12px;
  margin-top: 5px;
}

/* Alert Styles */
.alert {
  padding: 12px 16px;
  margin-bottom: 16px;
  border: 1px solid transparent;
  border-radius: 4px;
}

.alert-warning {
  color: #856404;
  background-color: #fff3cd;
  border-color: #ffeaa7;
}

/* Status Options */
.status-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-option {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.status-option:hover {
  background-color: #f8f9fa;
}

.status-option.active {
  background-color: #0d6efd;
  color: white;
  border-color: #0d6efd;
}

.status-option.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status-option.updating {
  background-color: #ffc107;
  color: #000;
}

/* Utility Classes */
.fw-bold {
  font-weight: 600;
}

.fw-extrabold {
  font-weight: 800;
}

.text-center {
  text-align: center;
}

.text-end {
  text-align: right;
}

.mb-0 {
  margin-bottom: 0;
}

.mb-2 {
  margin-bottom: 8px;
}

.mb-3 {
  margin-bottom: 16px;
}

.mb-4 {
  margin-bottom: 24px;
}

.mt-3 {
  margin-top: 16px;
}

.mt-4 {
  margin-top: 24px;
}

.me-1 {
  margin-right: 4px;
}

.me-3 {
  margin-right: 12px;
}

.ms-auto {
  margin-left: auto;
}

.d-flex {
  display: flex;
}

.justify-content-between {
  justify-content: space-between;
}

.justify-content-center {
  justify-content: center;
}

.align-items-center {
  align-items: center;
}

.align-middle {
  vertical-align: middle;
}

/* Responsive Design */
@media (max-width: 768px) {
  .status-timeline {
    flex-direction: column;
    gap: 20px;
  }

  .status-step {
    flex-direction: row;
    justify-content: flex-start;
    text-align: left;
  }

  .status-info {
    margin-left: 15px;
    text-align: left;
  }

  .status-line {
    display: none;
  }

  .row {
    flex-direction: column;
  }

  .col-md-6 {
    width: 100%;
    margin-bottom: 20px;
  }

  .modal-dialog {
    width: 95%;
    margin: 10px;
  }

  .table-responsive {
    font-size: 12px;
  }

  .product-image {
    width: 60px;
    height: 60px;
  }

  .address-input-group {
    flex-direction: column;
    gap: 10px;
  }

  .address-select-btn {
    width: 100%;
  }
}

@media (max-width: 576px) {
  .status-actions {
    flex-direction: column;
    gap: 10px;
  }

  .btn {
    width: 100%;
  }

  .order-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }

  .header-actions {
    width: 100%;
  }
}
</style>