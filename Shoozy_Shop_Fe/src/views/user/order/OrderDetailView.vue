<template>
  <div class="order-detail">
    <!-- Loading State -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>Đang tải chi tiết đơn hàng...</p>
    </div>

    <!-- Error State -->
    <div v-if="error" class="error">
      <p>{{ error }}</p>
      <button @click="fetchOrderDetail" class="retry-btn">Thử lại</button>
    </div>

    <!-- Main Content -->
    <div v-if="!loading && !error && order" class="main-content">
      <!-- Header -->
      <div class="header">
        <div class="header-left">
          <button @click="goBack" class="back-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                    stroke-linejoin="round" />
            </svg>
            TRỞ LẠI
          </button>
        </div>
        <div class="header-center">
          <span class="order-id">MÃ ĐƠN HÀNG: {{ order.orderCode }}</span>
        </div>
        <div class="header-right">
          <span class="order-type">{{ order.type == true ? 'ONLINE' : 'OFFLINE' }}</span>
        </div>
      </div>

      <!-- Order Status Timeline -->
      <div class="info-card">
        <div class="card-header">
          <i class="fas fa-route"></i>
          <span class="card-title">Trạng thái đơn hàng</span>
        </div>

        <div class="status-timeline">
          <div v-for="(step, index) in statusSteps" :key="step.key"
               :class="['status-step', getStatusClass(index)]">
            <div class="status-icon">
              <i :class="step.icon"></i>
            </div>
            <div class="status-content">
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
      </div>

      <!-- Payment History -->
      <div class="info-card">
        <div class="card-header">
          <i class="fas fa-credit-card"></i>
          <span class="card-title">Lịch sử thanh toán</span>
        </div>

        <div class="payment-table">
          <div class="table-header">
            <div class="col-stt">STT</div>
            <div class="col-method">HÌNH THỨC GIAO DỊCH</div>
            <div class="col-payment">PHƯƠNG THỨC THANH TOÁN</div>
            <div class="col-status">TRẠNG THÁI THANH TOÁN</div>
            <div class="col-time">THỜI GIAN</div>
            <div class="col-amount">TỔNG TIỀN</div>
            <div class="col-note">GHI CHÚ</div>
          </div>

          <div v-if="order.transactions && order.transactions.length > 0" class="table-body">
            <div v-for="(transaction, index) in order.transactions" :key="transaction.id" class="table-row">
              <div class="col-stt">{{ index + 1 }}</div>
              <div class="col-method">
                <span :class="['method-badge', getTransactionTypeClass(order.paymentMethod.type)]">
                  {{ getTransactionTypeText(order.paymentMethod.type) }}
                </span>
              </div>
              <div class="col-payment">{{ getPaymentMethodText(order.paymentMethod.type) }}</div>
              <div class="col-status">
                <span :class="['status-badge', transaction.status?.toLowerCase()]">
                  {{ getTransactionStatusText(transaction.status) }}
                </span>
              </div>
              <div class="col-time">{{ formatDateTime(transaction.completedDate) }}</div>
              <div class="col-amount">{{ formatCurrency(transaction.amount) }}</div>
              <div class="col-note">{{ transaction.note || 'Không có ghi chú' }}</div>
            </div>
          </div>

          <div v-else class="table-body">
            <div class="table-row empty">
              <div class="col-stt">1</div>
              <div class="col-method">
                <span class="method-badge offline">OFFLINE</span>
              </div>
              <div class="col-payment">Thanh toán khi nhận hàng</div>
              <div class="col-status">
                <span class="status-badge pending">Chờ thanh toán</span>
              </div>
              <div class="col-time">Chưa có</div>
              <div class="col-amount">{{ formatCurrency(finalTotal) }}</div>
              <div class="col-note">Không có ghi chú</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Products Card -->
      <div class="info-card">
        <div class="card-header">
          <i class="fas fa-shopping-bag"></i>
          <span class="card-title">Thông tin sản phẩm đã mua</span>
        </div>

        <div class="product-table">
          <div class="table-header">
            <div class="col-stt">STT</div>
            <div class="col-image">ẢNH SẢN PHẨM</div>
            <div class="col-product">SẢN PHẨM</div>
            <div class="col-quantity">SỐ LƯỢNG</div>
            <div class="col-time">THỜI GIAN</div>
            <div class="col-price">ĐƠN GIÁ</div>
            <div class="col-total">TỔNG TIỀN</div>
          </div>

          <div class="table-body">
            <div v-for="(detail, index) in order.orderDetails" :key="detail.orderDetailId" class="table-row">
              <div class="col-stt">{{ index + 1 }}</div>
              <div class="col-image">
                <img :src="detail.thumbnail" :alt="detail.productName" class="product-image">
              </div>
              <div class="col-product">
                <div class="product-info">
                  <h4 class="product-name">
                    {{ detail.productName }}
                    <span v-if="detail.refundedQuantity === detail.quantity" class="badge bg-success ms-2">
                        Đã hoàn tiền
                      </span>
                    <span v-else-if="detail.refundedQuantity > 0" class="badge bg-info ms-2">
                        Hoàn tiền {{ detail.refundedQuantity }} / {{ detail.quantity }}
                      </span>
                  </h4>
                  <p class="product-variant">Size: {{ detail.size }} | Màu: {{ detail.color }}</p>
                </div>
              </div>
              <div class="col-quantity">{{ detail.quantity }}</div>
              <div class="col-time">{{ formatDateTime(order.createdAt) }}</div>
              <div class="col-price">{{ formatCurrency(detail.price) }}</div>
              <div class="col-total">{{ formatCurrency(detail.totalMoney) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Customer Information and Invoice Summary -->
      <div class="bottom-section">
        <!-- Customer Information -->
        <div class="info-card customer-info">
          <div class="card-header2">
            <span class="card-title">Thông tin khách hàng</span>
            <button
                v-if="canEditCustomerInfo"
                class="btn btn-edit"
                @click="toggleEditMode"
            >
              {{ isEditingUserInformation ? 'HỦY' : 'CHỈNH SỬA' }}
            </button>
          </div>

          <div class="customer-details">
            <!-- Họ tên -->
            <div class="detail-group">
              <label>Họ tên <span class="required">*</span></label>
              <input
                  type="text"
                  class="detail-input"
                  v-model="order.fullName"
                  :readonly="!isEditingUserInformation"
                  :class="{
                    'editable': isEditingUserInformation,
                    'error': validationErrors.fullName
                  }"
                  @input="handleFullNameInput"
                  @blur="validateFullName"
                  placeholder="Nhập họ và tên"
                  maxlength="100"
                  autocomplete="name"
              >
              <div v-if="validationErrors.fullName" class="error-message">
                {{ validationErrors.fullName }}
              </div>
            </div>

            <!-- Số điện thoại -->
            <div class="detail-group">
              <label>Số điện thoại <span class="required">*</span></label>
              <input
                  type="tel"
                  class="detail-input"
                  v-model="order.phoneNumber"
                  :readonly="!isEditingUserInformation"
                  :class="{
                    'editable': isEditingUserInformation,
                    'error': validationErrors.phoneNumber
                  }"
                  @input="formatPhoneNumber"
                  @blur="validatePhoneNumber"
                  placeholder="Nhập số điện thoại (VD: 0901234567)"
                  maxlength="15"
                  autocomplete="tel"
              >
              <div v-if="validationErrors.phoneNumber" class="error-message">
                {{ validationErrors.phoneNumber }}
              </div>
            </div>

            <!-- Địa chỉ giao hàng -->
            <div class="detail-group">
              <label>Địa chỉ giao hàng <span class="required">*</span></label>
              <div class="address-section">
                <!-- Display mode -->
                <div v-if="!isEditingUserInformation" class="address-display">
                  <div class="address-text">{{ order.address || 'Chưa có địa chỉ' }}</div>
                </div>

                <!-- Edit mode -->
                <div v-else class="address-edit">
                  <div class="current-address" :class="{ 'error': validationErrors.address }">
                    <div class="current-address-label">Địa chỉ hiện tại:</div>
                    <div class="current-address-text">{{ order.address || 'Chưa có địa chỉ' }}</div>
                  </div>
                  <button
                      class="btn btn-outline-secondary address-change-btn"
                      @click="showAddressModal = true"
                      type="button"
                  >
                    <i class="fas fa-map-marker-alt"></i>
                    Chọn địa chỉ từ danh sách
                  </button>
                  <div v-if="validationErrors.address" class="error-message">
                    {{ validationErrors.address }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Phương thức thanh toán -->
            <div class="detail-group">
              <label>Phương thức thanh toán</label>
              <div class="detail-value">{{ getMainPaymentMethod() }}</div>
            </div>

            <!-- Ghi chú -->
            <div class="detail-group">
              <label>Ghi chú</label>
              <textarea
                  class="detail-input detail-textarea"
                  rows="3"
                  v-model="order.note"
                  :readonly="!isEditingUserInformation"
                  :class="{ 'editable': isEditingUserInformation }"
                  placeholder="Nhập ghi chú (tùy chọn)"
                  maxlength="500"
                  @input="handleNoteInput"
              ></textarea>
              <div v-if="isEditingUserInformation" class="char-count">
                {{ (order.note || '').length }}/500 ký tự
              </div>
            </div>

            <!-- Action buttons -->
            <div v-if="isEditingUserInformation" class="edit-actions">
              <button
                  class="btn btn-save"
                  @click="handleUpdate"
                  :disabled="!isFormValid || isUpdating"
              >
                <span v-if="isUpdating" class="loading-spinner"></span>
                {{ isUpdating ? 'ĐANG CẬP NHẬT...' : 'CẬP NHẬT' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Invoice Summary -->
        <div class="info-card invoice-summary">
          <div class="card-header">
            <span class="card-title">Hóa đơn</span>
          </div>

          <div class="invoice-details">
            <div class="invoice-row">
              <span class="invoice-label">Tổng tiền sản phẩm:</span>
              <span class="invoice-value">{{ formatCurrency(subtotal) }}</span>
            </div>

            <div class="invoice-row">
              <span class="invoice-label">Phí vận chuyển:</span>
              <span class="invoice-value shipping-fee">+ {{ formatCurrency(shippingFee) }}</span>
            </div>

            <div v-if="couponDiscount > 0" class="invoice-row">
              <span class="invoice-label">Giảm giá ({{ order.coupon.name }})  :</span>
              <span class="invoice-value discount">- {{ formatCurrency(couponDiscount) }}</span>
            </div>

            <div class="invoice-row">
              <span class="invoice-label fw-bold">Tổng cần thanh toán:</span>
              <span class="invoice-value total-amount">{{ formatCurrency(finalTotal) }}</span>
            </div>

            <div class="invoice-row">
              <span class="invoice-label">Số tiền đã thanh toán:</span>
              <span class="invoice-value paid-amount">{{ formatCurrency(amountPaid) }}</span>
            </div>

            <div class="invoice-row">
              <span class="invoice-label">Cần trả thêm:</span>
              <span class="invoice-value remaining-amount">{{ formatCurrency(remainingAmount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Address Modal -->
    <ListAddressModal
        v-if="showAddressModal"
        :userId="order?.userId || getCurrentUserId()"
        @save="handleAddressSave"
        @close="showAddressModal = false"
    />
  </div>

  <ShowToastComponent ref="toastRef"/>
</template>

<script setup>
import {ref, computed, onMounted, onUnmounted} from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {getOrderById, updateCustomerInfo} from "@/service/OrderApi.js"
import ShowToastComponent from "@/components/ShowToastComponent.vue";
import ListAddressModal from "@/components/ListAddressModal.vue";
import emitter from "@/service/EvenBus.js";

const router = useRouter()
const route = useRoute()

const order = ref(null)
const loading = ref(false)
const error = ref('')
const isEditingUserInformation = ref(false);
const originalData = ref(null);
const showAddressModal = ref(false); // State để hiển thị modal địa chỉ
const isUpdating = ref(false); // State cho loading button

// Validation states
const validationErrors = ref({
  fullName: '',
  phoneNumber: '',
  address: ''
});

const toastRef = ref(null);

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type);
};

// Enhanced validation functions
const validateFullName = () => {
  const name = order.value?.fullName?.trim() || '';
  validationErrors.value.fullName = '';

  if (!name) {
    validationErrors.value.fullName = 'Họ tên không được để trống';
    return false;
  }

  if (name.length < 2) {
    validationErrors.value.fullName = 'Họ tên phải có ít nhất 2 ký tự';
    return false;
  }

  if (name.length > 100) {
    validationErrors.value.fullName = 'Họ tên không được vượt quá 100 ký tự';
    return false;
  }

  // Kiểm tra ký tự đặc biệt (chỉ cho phép chữ cái, khoảng trắng và dấu tiếng Việt)
  const nameRegex = /^[a-zA-ZÀ-ỹ\u00C0-\u024F\u1E00-\u1EFF\s]+$/;
  if (!nameRegex.test(name)) {
    validationErrors.value.fullName = 'Họ tên chỉ được chứa chữ cái và khoảng trắng';
    return false;
  }

  // Kiểm tra không có nhiều khoảng trắng liên tiếp
  if (/\s{2,}/.test(name)) {
    validationErrors.value.fullName = 'Họ tên không được chứa nhiều khoảng trắng liên tiếp';
    return false;
  }

  return true;
};

const validatePhoneNumber = () => {
  const phone = order.value?.phoneNumber?.trim() || '';
  validationErrors.value.phoneNumber = '';

  if (!phone) {
    validationErrors.value.phoneNumber = 'Số điện thoại không được để trống';
    return false;
  }

  // Remove all non-digit characters for validation
  const cleanPhone = phone.replace(/\D/g, '');

  if (cleanPhone.length < 10) {
    validationErrors.value.phoneNumber = 'Số điện thoại phải có ít nhất 10 chữ số';
    return false;
  }

  if (cleanPhone.length > 11) {
    validationErrors.value.phoneNumber = 'Số điện thoại không được vượt quá 11 chữ số';
    return false;
  }

  // Kiểm tra định dạng số điện thoại Việt Nam cải thiện
  const phoneRegex = /^(0|84)[3-9]\d{8}$|^(0|84)[1][2-9]\d{8}$/;
  if (!phoneRegex.test(cleanPhone)) {
    validationErrors.value.phoneNumber = 'Số điện thoại không đúng định dạng Việt Nam';
    return false;
  }

  // Kiểm tra số điện thoại không được toàn số giống nhau
  if (/^(\d)\1{9,10}$/.test(cleanPhone)) {
    validationErrors.value.phoneNumber = 'Số điện thoại không được toàn số giống nhau';
    return false;
  }

  return true;
};

const validateAddress = () => {
  const address = order.value?.address?.trim() || '';
  validationErrors.value.address = '';

  if (!address) {
    validationErrors.value.address = 'Địa chỉ giao hàng không được để trống';
    return false;
  }

  if (address.length < 10) {
    validationErrors.value.address = 'Địa chỉ phải có ít nhất 10 ký tự';
    return false;
  }

  if (address.length > 500) {
    validationErrors.value.address = 'Địa chỉ không được vượt quá 500 ký tự';
    return false;
  }

  // Kiểm tra địa chỉ có ít nhất một chữ cái
  if (!/[a-zA-ZÀ-ỹ\u00C0-\u024F\u1E00-\u1EFF]/.test(address)) {
    validationErrors.value.address = 'Địa chỉ phải chứa ít nhất một chữ cái';
    return false;
  }

  return true;
};

// Enhanced format phone number function
const formatPhoneNumber = (event) => {
  let phone = event.target.value.replace(/\D/g, '') || '';

  // Limit to 11 digits
  if (phone.length > 11) {
    phone = phone.substring(0, 11);
  }

  // Format based on length
  if (phone.length >= 10) {
    if (phone.length === 10) {
      // Format: 0xxx xxx xxx
      phone = phone.replace(/(\d{4})(\d{3})(\d{3})/, '$1 $2 $3');
    } else {
      // Format: 0xxx xxxx xxx
      phone = phone.replace(/(\d{4})(\d{4})(\d{3})/, '$1 $2 $3');
    }
  } else if (phone.length >= 7) {
    phone = phone.replace(/(\d{4})(\d{3})/, '$1 $2');
  } else if (phone.length >= 4) {
    phone = phone.replace(/(\d{4})/, '$1');
  }

  order.value.phoneNumber = phone;

  // Clear validation error when typing
  if (validationErrors.value.phoneNumber && phone.length > 0) {
    validationErrors.value.phoneNumber = '';
  }
};

// Computed property to check if form is valid
const isFormValid = computed(() => {
  if (!isEditingUserInformation.value) return true;

  // Only validate if currently editing
  const fullNameValid = validateFullName();
  const phoneValid = validatePhoneNumber();
  const addressValid = validateAddress();

  return fullNameValid && phoneValid && addressValid;
});

// Real-time validation handlers
const handleFullNameInput = () => {
  // Clear error when user starts typing
  if (validationErrors.value.fullName) {
    validationErrors.value.fullName = '';
  }

  // Clean up multiple spaces while typing
  if (order.value.fullName) {
    order.value.fullName = order.value.fullName.replace(/\s{2,}/g, ' ');
  }
};

const handleNoteInput = () => {
  // Just clear any potential errors (note doesn't have validation but we keep consistency)
};

// Clear all validation errors
const clearValidationErrors = () => {
  validationErrors.value = {
    fullName: '',
    phoneNumber: '',
    address: ''
  };
};

// Hàm lấy user ID hiện tại
const getCurrentUserId = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  return user.id || null;
};

// Xử lý khi chọn địa chỉ từ modal
const handleAddressSave = (addressData) => {
  if (addressData && addressData.data) {
    const address = addressData.data;
    // Cập nhật địa chỉ trong order
    if (address.line) {
      order.value.address = address.line;
    } else {
      // Tạo địa chỉ đầy đủ từ các thành phần
      const fullAddress = [
        address.addressDetail,
        address.wardName,
        address.districtName,
        address.provinceName
      ].filter(Boolean).join(', ');
      order.value.address = fullAddress;
    }

    // Validate địa chỉ mới
    validateAddress();
  }
  showAddressModal.value = false;
  showToast('Đã chọn địa chỉ thành công', 'success');
};

const statusSteps = ref([
  {key: 'PENDING', label: 'Chờ xác nhận', icon: 'fas fa-clock', date: null},
  {key: 'CONFIRMED', label: 'Đã xác nhận', icon: 'fas fa-check-circle', date: null},
  {key: 'PROCESSING', label: 'Đang đóng gói', icon: 'fas fa-cog', date: null},
  {key: 'SHIPPING', label: 'Đang vận chuyển', icon: 'fas fa-truck', date: null},
  {key: 'DELIVERED', label: 'Đã giao', icon: 'fas fa-check-circle', date: null},
  {key: 'COMPLETED', label: 'Đã hoàn thành', icon: 'fas fa-star', date: null},
  {key: 'CANCELLED', label: 'Đã hủy', icon: 'fas fa-times-circle', date: null}
])

const currentStatusIndex = computed(() => {
  if (!order.value) return -1
  return statusSteps.value.findIndex(step => step.key === order.value.status)
})

const subtotal = computed(() => {
  if (!order.value || !order.value.orderDetails) return 0
  return order.value.orderDetails.reduce((sum, item) => sum + item.totalMoney, 0)
})

const couponDiscount = computed(() => {
  return order.value?.couponDiscountAmount || 0
})

const shippingFee = computed(() => {
  return order.value?.shippingFee || 0
})

const finalTotal = computed(() => {
  return order.value?.finalPrice || 0
})

const amountPaid = computed(() => {
  if (!order.value || !order.value.transactions) return 0
  return order.value.transactions
      .filter(transaction => transaction.status === 'SUCCESS')
      .reduce((sum, transaction) => sum + transaction.amount, 0)
})

const remainingAmount = computed(() => {
  return Math.max(0, finalTotal.value - amountPaid.value)
})

const fetchOrderDetail = async () => {
  loading.value = true
  error.value = ''
  try {
    const orderId = route.params.id
    const res = await getOrderById(orderId)
    console.log('Order detail fetched:', res.data.data)
    order.value = res.data.data
    initializeStatusDates()
  } catch (err) {
    console.error('Error fetching order detail:', err)
    error.value = 'Không thể tải chi tiết đơn hàng. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

const initializeStatusDates = () => {
  if (!order.value) return
  const currentIndex = currentStatusIndex.value
  if (currentIndex >= 0) {
    statusSteps.value[0].date = formatDateTime(order.value.createdAt)
  }
  if (currentIndex > 0) {
    statusSteps.value[currentIndex].date = formatDateTime(order.value.updatedAt)
  }
  if (order.value.shippingDate && currentIndex >= 3) {
    statusSteps.value[3].date = formatDateTime(order.value.shippingDate)
  }
}

const getStatusClass = (index) => {
  const currentIndex = currentStatusIndex.value
  if (order.value?.status === 'CANCELLED') {
    return statusSteps.value[index].key === 'CANCELLED' ? 'completed cancelled' : 'pending'
  }
  return index <= currentIndex ? 'completed' : 'pending'
}

const goBack = () => {
  router.go(-1)
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

const getTransactionTypeText = (type) => {
  switch (type) {
    case 'ONLINE_PAYMENT':
    case 'VNPAY':
    case 'MOMO':
    case 'CREDIT_CARD': return 'ONLINE'
    case 'CASH': return 'OFFLINE'
    default: return 'OFFLINE'
  }
}

const getTransactionTypeClass = (type) => {
  switch (type) {
    case 'ONLINE_PAYMENT':
    case 'VNPAY':
    case 'MOMO':
    case 'CREDIT_CARD': return 'online'
    case 'CASH': return 'offline'
    default: return 'offline'
  }
}

const getPaymentMethodText = (method) => {
  switch (method) {
    case 'CASH': return 'Thanh toán khi nhận hàng'
    case 'ONLINE_PAYMENT': return 'Chuyển khoản ngân hàng'
    case 'CREDIT_CARD': return 'Thẻ tín dụng'
    case 'MOMO': return 'Ví MoMo'
    case 'VNPAY': return 'VNPay'
    default: return 'Thanh toán khi nhận hàng'
  }
}

const getTransactionStatusText = (status) => {
  switch (status) {
    case 'SUCCESS': return 'Đã thanh toán'
    case 'PENDING': return 'Chờ thanh toán'
    case 'FAILED': return 'Thất bại'
    case 'CANCELLED': return 'Đã hủy'
    default: return 'Chờ thanh toán'
  }
}

const getMainPaymentMethod = () => {
  if (!order.value || !order.value.paymentMethod) {
    return 'Thanh toán khi nhận hàng'
  }
  return order.value.paymentMethod.name || getPaymentMethodText(order.value.paymentMethod.type)
}

const getLineClass = (index) => {
  const currentIndex = currentStatusIndex.value;
  if (order.value?.status === 'CANCELLED') {
    return 'line-cancelled';
  }
  if (index < currentIndex) {
    return 'line-completed';
  }
  return '';
}

// Enhanced toggle edit mode
const toggleEditMode = () => {
  if (!isEditingUserInformation.value) {
    // Save original data before editing
    originalData.value = {
      fullName: order.value.fullName,
      phoneNumber: order.value.phoneNumber,
      address: order.value.address,
      note: order.value.note
    };
    isEditingUserInformation.value = true;
    clearValidationErrors();
  } else {
    // Cancel editing - restore original data
    if (originalData.value) {
      order.value.fullName = originalData.value.fullName;
      order.value.phoneNumber = originalData.value.phoneNumber;
      order.value.address = originalData.value.address;
      order.value.note = originalData.value.note;
    }
    isEditingUserInformation.value = false;
    originalData.value = null;
    showAddressModal.value = false;
    clearValidationErrors();
  }
};

// Enhanced handle update
const handleUpdate = async () => {
  // Validate all fields before submitting
  const isFullNameValid = validateFullName();
  const isPhoneValid = validatePhoneNumber();
  const isAddressValid = validateAddress();

  if (!isFullNameValid || !isPhoneValid || !isAddressValid) {
    showToast('Vui lòng kiểm tra lại thông tin đã nhập', 'error');

    // Focus vào field đầu tiên có lỗi
    if (!isFullNameValid) {
      const nameInput = document.querySelector('input[v-model="order.fullName"]');
      nameInput?.focus();
    } else if (!isPhoneValid) {
      const phoneInput = document.querySelector('input[v-model="order.phoneNumber"]');
      phoneInput?.focus();
    } else if (!isAddressValid) {
      const addressBtn = document.querySelector('.address-change-btn');
      addressBtn?.focus();
    }
    return;
  }

  isUpdating.value = true;
  try {
    // Clean và validate data trước khi gửi
    const cleanPhone = order.value.phoneNumber.replace(/\D/g, '');
    const cleanFullName = order.value.fullName.trim().replace(/\s+/g, ' ');
    const cleanAddress = order.value.address.trim();
    const cleanNote = order.value.note?.trim() || '';

    const updateData = {
      fullName: cleanFullName,
      phoneNumber: cleanPhone,
      address: cleanAddress,
      note: cleanNote
    };

    console.log('Updating customer info:', updateData);

    await updateCustomerInfo(order.value.id, updateData);

    showToast('Cập nhật thông tin thành công', 'success');

    // Update local data with cleaned values
    order.value.fullName = cleanFullName;
    order.value.phoneNumber = cleanPhone;
    order.value.address = cleanAddress;
    order.value.note = cleanNote;

    isEditingUserInformation.value = false;
    originalData.value = null;
    clearValidationErrors();

    // Refresh order data from server
    fetchOrderDetail();
  } catch (error) {
    showToast('Cập nhật thất bại. Vui lòng thử lại', 'error');
    console.error('Update error:', error);

    // Hiển thị lỗi cụ thể nếu có từ server
    if (error.response?.data?.message) {
      showToast(error.response.data.message, 'error');
    }
  } finally {
    isUpdating.value = false;
  }
};

// Computed property for checking if customer info can be edited
const canEditCustomerInfo = computed(() => {
  if (!order.value) return false;
  return ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(order.value.status);
});

// Event handler cho WebSocket messages
const handleOrderUpdate = (data) => {
  console.log('Received order update via WebSocket:', data);
  // Refresh đơn hàng khi có cập nhật
  fetchOrderDetail()
}

onMounted(() => {
  fetchOrderDetail()
  emitter.on('order', handleOrderUpdate);
})

onUnmounted(() => {
  // Gỡ bỏ listener khi component bị unmount để tránh memory leak
  emitter.off('order', handleOrderUpdate);
});
</script>

<style scoped>
.order-detail {
  max-width: 1330px;
  margin: 0 auto;
  padding: 0px 20px 60px 20px;
}

.loading {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #000;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading p {
  color: #666;
  font-size: 16px;
  margin: 0;
}

.error {
  text-align: center;
  padding: 60px 20px;
  background: #fff5f5;
  color: #e53e3e;
  border-radius: 20px;
  margin-bottom: 20px;
  border: 1px solid #fed7d7;
}

.retry-btn {
  background: #000;
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 25px;
  cursor: pointer;
  margin-top: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.retry-btn:hover {
  background: #333;
  transform: translateY(-2px);
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  padding: 20px 0px;
  border-radius: 0;
  box-shadow: none;
  border-bottom: 1px solid #e5e7eb;
}

.header-left {
  flex: 1;
  display: flex;
  justify-content: flex-start;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 8px 0;
  transition: color 0.2s ease;
}

.back-btn:hover {
  color: #000;
}

.back-btn svg {
  width: 16px;
  height: 16px;
}

.order-id {
  font-size: 16px;
  font-weight: 600;
  color: #000;
}

.order-type {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  padding: 6px 12px;
  background: #f3f4f6;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
}

.info-card {
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.card-header i {
  color: #666;
  font-size: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #000;
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

.status-step.completed .status-icon {
  background: #000;
  color: white;
}

.status-step.cancelled .status-icon {
  background: #dc3545;
  color: white;
}

.status-content {
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

.status-line.line-completed {
  background: #000;
}

.status-line.line-cancelled {
  background: #dc3545;
}

.status-step:last-child .status-line {
  display: none;
}

/* Tables */
.payment-table, .product-table {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

/* Payment table columns */
.col-stt { width: 8%; text-align: center; }
.col-method { width: 15%; text-align: center; }
.col-payment { width: 20%; text-align: center;}
.col-status { width: 15%; text-align: center; }
.col-time { width: 15%; font-size: 12px; text-align: center; }
.col-amount { width: 15%; text-align: center; font-weight: 600; }
.col-note { width: 12%; font-size: 12px; text-align: center;}

/* Product table columns */
.col-image { width: 10%; text-align: center; }
.col-product { width: 25%; text-align: center; }
.col-quantity { width: 12%; text-align: center; }
.col-price { width: 15%; text-align: center; font-weight: 600; }
.col-total { width: 15%; text-align: center; font-weight: 600; }

.table-header {
  color: black;
  display: flex;
  font-weight: 600;
  font-size: 12px;
  text-align: center;
  background: #efefef;
}

.table-header > div {
  padding: 15px 10px;
  border-right: 1px solid #e5e7eb;
}

.table-header > div:last-child {
  border-right: none;
}

.table-body {
  background: white;
}

.table-row {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
  align-items: center;
}

.table-row:last-child {
  border-bottom: none;
}

.table-row > div {
  padding: 15px 10px;
  border-right: 1px solid #e5e7eb;
  font-size: 14px;
}

.table-row > div:last-child {
  border-right: none;
}

.method-badge, .status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.method-badge.online {
  background: #e3f2fd;
  color: #1976d2;
}

.method-badge.offline {
  background: #ffebee;
  color: #d32f2f;
}

.status-badge.success {
  background: #e8f5e8;
  color: #2e7d32;
}

.status-badge.pending {
  background: #fff8e1;
  color: #f57c00;
}

.status-badge.failed {
  background: #ffebee;
  color: #d32f2f;
}

.product-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
}

.product-name {
  font-weight: 600;
  color: #000;
  margin: 0 0 5px 0;
  font-size: 14px;
}

.product-variant {
  color: #666;
  font-size: 12px;
  margin: 0;
}

.badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
}

.bg-success {
  background-color: #28a745;
  color: white;
}

.bg-info {
  background-color: #17a2b8;
  color: white;
}

.ms-2 {
  margin-left: 8px;
}

/* Bottom Section */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 25px;
}

.customer-details {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-group label {
  font-weight: 600;
  color: #000;
  font-size: 14px;
  margin-bottom: 5px;
}

.detail-input {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  background-color: #f9fafb;
  transition: all 0.3s ease;
  resize: vertical;
}

.detail-input:readonly {
  background-color: #f9fafb;
  cursor: default;
}

.detail-input.editable {
  background-color: white;
  border-color: #000;
  color: #000;
  cursor: text;
}

.detail-input.editable:focus {
  outline: none;
  border-color: #000;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.1);
}

.detail-textarea {
  min-height: 80px;
  font-family: inherit;
}

.detail-value {
  color: #666;
  font-size: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

/* Address Section */
.address-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.address-display {
  padding: 12px 15px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background-color: #f9fafb;
  min-height: 80px;
  display: flex;
  align-items: flex-start;
}

.address-text {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.address-edit {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.current-address {
  padding: 12px 15px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background-color: #f8f9fa;
  min-height: 80px;
}

.current-address-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  margin-bottom: 8px;
}

.current-address-text {
  color: #333;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.address-change-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: white;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  align-self: flex-start;
}

.address-change-btn:hover {
  border-color: #000;
  color: #000;
  background-color: #f8f9fa;
}

.address-change-btn:focus {
  outline: none;
  border-color: #000;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
}

.address-change-btn i {
  font-size: 14px;
}

/* Edit Actions */
.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.btn-save {
  background: #000;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-save:hover {
  background: #333;
  color: white;
}

.btn-save:disabled {
  background: #6b7280 !important;
  cursor: not-allowed !important;
  opacity: 0.6;
}

.btn-save:disabled:hover {
  background: #6b7280 !important;
  transform: none !important;
}

.invoice-details {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.invoice-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.invoice-row:last-child {
  border-bottom: none;
}

.invoice-label {
  font-weight: 500;
  color: #666;
}

.invoice-value {
  font-weight: 600;
  color: #000;
}

.shipping-fee {
  color: #dc3545;
}

.discount {
  color: #28a745;
}

.total-amount {
  font-size: 18px;
  font-weight: 700;
  color: #000;
}

.paid-amount {
  color: #28a745;
}

.remaining-amount {
  color: #dc3545;
}

.card-header2 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.btn-edit {
  background: #000;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.btn-edit:hover {
  background: #333;
  color: white;
}

.fw-bold {
  font-weight: 600;
}

/* Simplified Error Validation Styles */

.current-address.error {
  border-bottom: 2px solid #dc3545 !important;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  background-color: #fff8f8;
}

.error-message {
  color: #dc3545;
  font-size: 13px;
  margin-top: 5px;
  padding-left: 2px;
  font-weight: 500;
}

.required {
  color: #dc3545;
  font-weight: bold;
}

/* Loading spinner for button */
.loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  border-top-color: transparent;
  animation: spin 1s ease-in-out infinite;
  margin-right: 8px;
}

/* Character count styling */
.char-count {
  font-size: 11px;
  color: #6b7280;
  text-align: right;
  margin-top: 4px;
}

</style>