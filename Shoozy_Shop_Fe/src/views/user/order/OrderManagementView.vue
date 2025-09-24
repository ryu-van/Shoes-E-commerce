<template>
  <div class="order-management">
    <!-- Loading State -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>Đang tải đơn hàng...</p>
    </div>

    <!-- Error State -->
    <div v-if="error" class="error">
      <p>{{ error }}</p>
      <button @click="fetchOrders" class="retry-btn">Thử lại</button>
    </div>

    <!-- Main Content -->
    <div v-if="!loading && !error" class="main-content">
      <!-- Tabs -->
      <div class="tabs">
        <button
            v-for="tab in tabs"
            :key="tab.id"
            :class="['tab', { active: activeTab === tab.id }]"
            @click="activeTab = tab.id"
        >
          {{ tab.name }}
          <span v-if="getOrderCount(tab.id)" class="count">
            ({{ getOrderCount(tab.id) }})
          </span>
        </button>

      </div>


      <!-- Search Bar -->
      <div class="search-bar">
        <input
            type="text"
            class="search-input"
            v-model="searchQuery"
            placeholder="Tìm kiếm theo mã đơn hàng, tên sản phẩm ..."
        >
      </div>

      <!-- Order List -->
      <!-- Order List -->
      <div class="content">
        <!-- Nếu là tab 'returns' thì hiển thị lịch sử trả hàng -->
        <ReturnHistory
   v-if="activeTab === 'returns'"
  :query="searchQuery"   
/>

        <!-- Nếu không phải tab 'returns' thì hiển thị danh sách đơn hàng như cũ -->
        <template v-else>
          <div v-if="filteredOrders.length === 0" class="empty-state">
            <h3>Không có đơn hàng nào</h3>
            <p>{{ activeTab === 'all' ? 'Bạn chưa có đơn hàng nào' : 'Không có đơn hàng nào trong mục này' }}</p>
          </div>

          <div v-for="order in filteredOrders" :key="order.id" class="order-item" :class="{ 'updating': updatingOrders.includes(order.id) }">
            <!-- Loading overlay -->
            <div v-if="updatingOrders.includes(order.id)" class="order-loading-overlay">
              <div class="mini-spinner"></div>
            </div>

            <div class="order-header">
              <div class="order-id-container" @click="goToOrderDetail(order.id)">
                <span class="order-id">Đơn hàng {{ order.orderCode }}</span>
                <div class="click-hint">
                  <svg class="hint-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 18l6-6-6-6"/>
                  </svg>
                  <span class="hint-text">Xem chi tiết</span>
                </div>
              </div>
              <div class="order-header-right">
                <span :class="['order-status', `status-${order.status.toLowerCase()}`]">
                  {{ getStatusText(order.status) }}
                </span>
                <!-- Hiển thị thời gian tạo đơn hàng -->
                <div class="order-time">
                  <small class="text-muted">
                    <i class="fas fa-clock me-1"></i>
                    {{ formatOrderTime(order.createdAt) }}
                  </small>
                  <!-- Hiển thị thời gian còn lại để xác nhận đơn hàng -->
                  <small v-if="order.status === 'DELIVERED'" class="text-warning d-block">
                    <i class="fas fa-hourglass-half me-1"></i>
                    Tự động hoàn thành sau: {{ getConfirmTimeLeft(order.updatedAt) }}
                  </small>
                </div>
              </div>
            </div>

            <!-- Products -->
            <div v-for="detail in order.orderDetails" :key="detail.productVariantId" class="product-row">
              <img :src="detail.thumbnail" :alt="detail.productName" class="product-image">
              <div class="product-info">
                <div class="product-name">{{ detail.productName }}</div>
                <div class="product-variant">{{ detail.color }} - Size {{ detail.size }}</div>
                <div class="product-quantity">Số lượng: {{ detail.quantity }}</div>
              </div>
              <div class="product-price">
                <span class="price-current">₫{{ formatPrice(detail.price) }}</span>
                <div class="price-total">Tổng: ₫{{ formatPrice(detail.totalMoney) }}</div>
              </div>
              <!-- Nút đánh giá sản phẩm hoặc trạng thái đã đánh giá -->
              <button
                  v-if="order.status === 'COMPLETED' && !Number(detail.reviewId) && canReview(order.updatedAt)"
                  class="btn btn-primary"
                  style="margin-left: 16px"
                  @click="openReviewForm(order, detail)"
              >
                Đánh giá sản phẩm
              </button>
              <button
                  v-else-if="order.status === 'COMPLETED' && Number(detail.reviewId)"
                  class="btn btn-outline"
                  style="margin-left: 16px ; background-color:rgb(34, 134, 57) ; color: white"
                  @click="openShowReview(detail)"
              >
                Xem đánh giá
              </button>
            </div>

            <div class="order-footer">
              <div class="order-total">
                <span class="total-label">Tổng tiền:</span>
                <span class="total-amount">₫{{ formatPrice(order.finalPrice) }}</span>
              </div>
              <div class="action-buttons">
                 <button
                    v-if="order.status === 'COMPLETED' && canReturn(order.updatedAt) && returnableByOrder[order.id] === true"
                    class="btn btn-warning"
                    @click="openReturnModal(order)"
                  >
                    Trả hàng
                  </button>

                <!-- Nút thanh toán lại cho đơn hàng online -->
                <button
                    v-if="canShowRepayButton(order)"
                    class="btn btn-success"
                    @click="handleRepay(order)"
                    :disabled="updatingOrders.includes(order.id) || (order.status === 'PENDING' && !canRepay(order.createdAt))"
                >
                  <i class="fas fa-credit-card me-1"></i>
                  {{ order.status === 'PENDING' && !canRepay(order.createdAt) ? 'Đã hết hạn thanh toán' : 'Thanh toán lại với VNPay' }}
                  <small v-if="order.status === 'PENDING' && canRepay(order.createdAt)" class="d-block">{{ getRepayTimeLeft(order.createdAt) }}</small>
                </button>

                <button
                    v-for="action in getOrderActions(order)"
                    :key="action.text"
                    :class="['btn', action.type === 'primary' ? 'btn-primary' : 'btn-outline']"
                    :disabled="updatingOrders.includes(order.id)"
                    @click="handleAction(action.action, order)"
                >
                  {{ action.text }}
                </button>


              </div>
            </div>
          </div>
        </template>
      </div>

    </div>

    <!-- Confirmation Modal -->
    <div v-if="showConfirmModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ modalTitle }}</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <p>{{ modalMessage }}</p>
          <p v-if="isCancel" class="warning-text">Hành động này không thể hoàn tác.</p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeModal">Hủy bỏ</button>
          <button class="btn btn-primary" @click="confirmAction" :disabled="isProcessing">
            {{ isProcessing ? 'Đang xử lý...' : confirmButtonText }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal đánh giá sản phẩm -->
    <div v-if="showReviewForm" class="modal-overlay" @click="showReviewForm = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Đánh giá sản phẩm</h3>
          <button class="close-btn" @click="showReviewForm = false">×</button>
        </div>
        <div class="modal-body">
          <ReviewForm
              v-if="productToReview"
              :productId="productToReview.productId"
              :orderDetailId="productToReview.orderDetailId"
              @submitReview="handleAfterReview"
          />
        </div>
      </div>
    </div>

    <!-- Modal xem đánh giá sản phẩm -->
    <div v-if="showShowReview" class="modal-overlay" @click="showShowReview = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Xem đánh giá</h3>
          <button class="close-btn" @click="showShowReview = false">×</button>
        </div>
        <div class="modal-body">
          <ShowReview
              v-if="reviewDetail"
              :review="reviewDetail"
              @updated="handleReviewUpdated"
          />
        </div>
      </div>
    </div>

    <ShowToastComponent ref="toastRef"/>
    <ReturnModal
        v-if="showReturnModal"
        :order-id="selectedOrder.id"
        :user-id="selectedOrder.user.id"
        :visible="showReturnModal"
        @close="showReturnModal = false"
        @proceed="openCreateReturnRequestModal"
    />


   
<CreateReturnRequestModal
  v-if="showReturnRequestModal"
  :order="selectedOrder"
  :items="selectedItems"
  @close="closeReturnRequestModal"
  @submitted="handleReturnSubmitted"   
/>

  </div>
</template>

<script setup>
import {ref, computed, onMounted, onUnmounted} from 'vue'
import { useRouter } from 'vue-router'
import { deleteOrderById, getAllOrdersByUserId, updateOrderStatus, getReturnableItems, sendOrderCancelledByUserEmail, sendOrderCompletedEmail ,getTransactionByOrderCode} from '@/service/OrderApi.js'
import ShowToastComponent from "@/components/ShowToastComponent.vue"
import cartApi from "@/service/CartApi.js";
import emitter from "@/service/EvenBus.js";
import ReviewForm from '@/views/user/review/ReviewForm.vue'
import ShowReview from '@/views/user/review/ShowReview.vue'
import ReturnHistory from '@/views/user/return-rq/ReturnHistory.vue'
import ReturnModal from "@/views/user/return-rq/ReturnModal.vue";
import CreateReturnRequestModal from '@/views/user/return-rq/CreateReturnRequestModal.vue'
import { retryVnPay } from '@/service/PaymentService.js'
const returnableByOrder = ref({}) // { [orderId]: true/false }

const showReturnRequestModal = ref(false)
const selectedItems = ref([])

const showReturnModal = ref(false);
const selectedOrder = ref(null);

const router = useRouter()

const orders = ref([])
const loading = ref(false)
const error = ref('')
const activeTab = ref('all')
const searchQuery = ref('')

// Modal state - Updated to handle multiple modal types
const showConfirmModal = ref(false)
const orderToProcess = ref(null)
const currentAction = ref('')
const isProcessing = ref(false)

// Track updating orders
const updatingOrders = ref([])

const toastRef = ref(null)

// Ref để trigger reactivity cho timer
const currentTime = ref(Date.now())

// Set để theo dõi các đơn hàng đã được hủy tự động
const autoCancelledOrders = ref(new Set())

// Set để theo dõi các đơn hàng đã thanh toán thành công
const paidOrders = ref(new Set())

// Computed để theo dõi các đơn hàng cần cập nhật thời gian
const ordersNeedingUpdate = computed(() => {
  const now = new Date(currentTime.value);
  
  return orders.value.filter(order => {
    // Đơn hàng PENDING cần đếm ngược thanh toán (15 phút)
    // Nhưng không theo dõi nếu đã thanh toán thành công
    if (order.status === 'PENDING' && 
        order.paymentMethod?.type === 'ONLINE_PAYMENT' &&
        order.createdAt &&
        !autoCancelledOrders.value.has(order.id) &&
        !paidOrders.value.has(order.id)) { // Không theo dõi đơn hàng đã thanh toán
      const created = new Date(order.createdAt);
      const expire = new Date(created.getTime() + 15 * 60 * 1000); // 15 phút
      return now <= expire; // Chỉ theo dõi nếu chưa hết 15 phút
    }
    
    // Đơn hàng DELIVERED cần kiểm tra 7 ngày
    if (order.status === 'DELIVERED' && 
        order.updatedAt &&
        !autoCancelledOrders.value.has(order.id)) {
      const deliveredDate = new Date(order.updatedAt);
      const expire = new Date(deliveredDate.getTime() + 7 * 24 * 60 * 60 * 1000);
      return now <= expire; // Chỉ theo dõi nếu chưa hết 7 ngày
    }
    
    return false;
  });
});
// script setup
const handleReturnSubmitted = async ({ orderId, remainingCount } = {}) => {
  // đóng modal
  showReturnRequestModal.value = false

  // cập nhật trạng thái có còn trả được không
  if (selectedOrder.value) {
    if (typeof remainingCount === 'number') {
      // cập nhật lạc quan nếu có dữ liệu
      returnableByOrder.value = {
        ...returnableByOrder.value,
        [selectedOrder.value.id]: remainingCount > 0
      }
    } else {
      // không có count → hỏi BE để chắc chắn
      await refreshReturnable(selectedOrder.value)
      // ép reactivity cho object
      returnableByOrder.value = { ...returnableByOrder.value }
    }
  }

  showToast('Gửi yêu cầu trả hàng thành công.', 'success')
}

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type)
}

// Hàm để lấy trạng thái giao dịch từ hệ thống thanh toán
const getTransactionStatus = async (orderCode) => {
  try {
    const response = await getTransactionByOrderCode(orderCode);
    return response.data.data; // Trả về thông tin giao dịch
  } catch (error) {
    console.error('Lỗi khi lấy trạng thái giao dịch:', error);
    return null;
  }
}

// Hàm để kiểm tra trạng thái thanh toán thực tế
const checkPaymentStatus = async (order) => {
  if (!order.orderCode) return false;
  
  const transaction = await getTransactionStatus(order.orderCode);
  if (!transaction) return false;
  
  // Kiểm tra trạng thái giao dịch từ hệ thống thanh toán
  return transaction.status === 'SUCCESS' || transaction.paymentStatus === 'SUCCESS';
}

// Hàm để kiểm tra và cập nhật trạng thái thanh toán cho tất cả đơn hàng
const checkAllPaymentStatuses = async () => {
  const pendingOrders = orders.value.filter(order => 
    order.status === 'PENDING' && 
    order.paymentMethod?.type === 'ONLINE_PAYMENT' &&
    order.orderCode
  );
  
  for (const order of pendingOrders) {
    try {
      const isPaid = await checkPaymentStatus(order);
      if (isPaid) {
        paidOrders.value.add(order.id);
        // Cập nhật trạng thái đơn hàng trong danh sách local
        const orderIndex = orders.value.findIndex(o => o.id === order.id);
        if (orderIndex !== -1) {
          orders.value[orderIndex].paymentStatus = 'SUCCESS';
          orders.value[orderIndex].status = 'CONFIRMED'; // Hoặc trạng thái phù hợp
        }
      }
    } catch (error) {
      console.error(`Lỗi khi kiểm tra trạng thái thanh toán cho đơn hàng ${order.orderCode}:`, error);
    }
  }
}

const tabs = ref([
  { id: 'all', name: 'Tất cả' },
  { id: 'PENDING', name: 'Chờ xác nhận' },
  { id: 'CONFIRMED', name: 'Đã xác nhận' },
  { id: 'PROCESSING', name: 'Đang xử lý' },
  { id: 'SHIPPING', name: 'Đang giao hàng' },
  { id: 'DELIVERED', name: 'Đã giao hàng' },
  { id: 'COMPLETED', name: 'Hoàn thành' },
  { id: 'CANCELLED', name: 'Đã hủy' },
  { id: 'FAIL', name: 'Thanh toán thất bại' },
  { id: 'returns', name: 'Lịch sử trả hàng' },
])

// Computed properties for modal content
const modalTitle = computed(() => {
  switch (currentAction.value) {
    case 'cancel':
      return 'Xác nhận hủy đơn hàng'
    case 'confirm_received':
      return 'Xác nhận đã nhận hàng'
    default:
      return 'Xác nhận'
  }
})

const modalMessage = computed(() => {
  switch (currentAction.value) {
    case 'cancel':
      return `Bạn có chắc chắn muốn hủy đơn hàng #${orderToProcess.value?.id} không?`
    case 'confirm_received':
      return `Bạn có chắc chắn đã nhận được đơn hàng #${orderToProcess.value?.id} không?`
    default:
      return 'Bạn có chắc chắn muốn thực hiện hành động này không?'
  }
})

const confirmButtonText = computed(() => {
  switch (currentAction.value) {
    case 'cancel':
      return 'Xác nhận hủy'
    case 'confirm_received':
      return 'Xác nhận đã nhận'
    default:
      return 'Xác nhận'
  }
})

const isCancel = computed(() => currentAction.value === 'cancel')

function canReturn(updatedAt) {
  const completedDate = new Date(updatedAt);
  const now = new Date();
  const diffDays = Math.abs(now - completedDate) / (1000 * 60 * 60 * 24);
  return diffDays <= 7;
}
async function refreshReturnable(order) {
  if (!order?.id || !order?.user?.id) return
  try {
    const res = await getReturnableItems(order.id, order.user.id)
    returnableByOrder.value[order.id] = (res?.data?.data?.length ?? 0) > 0
  } catch (e) {
    // lỗi thì cứ coi như không còn để tránh hiển thị sai
    returnableByOrder.value[order.id] = false
  }
}

// 1) Một dòng còn trả được không?
function isDetailReturnable(detail) {
  if (!detail) return false;

  const qty = Number(detail.quantity ?? 0);
  if (qty <= 0) return false;

  // Nếu BE trả về số lượng còn có thể trả → ưu tiên dùng
  if (detail.returnableQuantity != null) {
    return Number(detail.returnableQuantity) > 0;
  }

  // Nếu có trạng thái trả hàng và nó đã kết thúc hoàn toàn → không cho trả
  if (detail.returnStatus) {
    const st = String(detail.returnStatus).toUpperCase();
    if (['RETURNED', 'REFUNDED', 'COMPLETED', 'CANCELLED'].includes(st)) return false;
  }

  // Nếu có số lượng đã/đang trả → còn dư thì được trả tiếp
  if (detail.returnedQuantity != null || detail.returnQuantity != null) {
    const returned = Number(detail.returnedQuantity ?? detail.returnQuantity ?? 0);
    return qty - returned > 0;
  }

  // Nếu có cờ boolean → chưa trả thì được trả
  if (detail.isReturned != null) return !detail.isReturned;
  if (detail.returned != null)   return !detail.returned;

  // ⬅️ MẶC ĐỊNH: không có thông tin gì cản trở ⇒ cho trả
  return true;
}

// 2) Lọc các dòng còn trả được
function extractReturnableItems(order) {
  if (!order?.orderDetails?.length) return [];
  return order.orderDetails.filter(isDetailReturnable);
}

// 3) Dùng để ẩn/hiện nút
function hasReturnableItems(order) {
  return extractReturnableItems(order).length > 0;
}



const prefilteredItems = ref([]);

async function openReturnModal(order) {
  if (!(order?.status === 'COMPLETED' && canReturn(order.updatedAt))) return

  // Hỏi BE xem còn item không
  const res = await getReturnableItems(order.id, order.user.id)
  const items = res?.data?.data ?? []

  // Cập nhật cache để nút ẩn/hiện đúng ở lần render sau
  returnableByOrder.value[order.id] = items.length > 0

  if (!items.length) {
    showToast('Đơn hàng này không còn sản phẩm nào để trả.', 'info')
    return
  }

  selectedOrder.value = order
  showReturnModal.value = true
}





const openCreateReturnRequestModal = (items) => {
  selectedItems.value = items;
  showReturnModal.value = false;
  showReturnRequestModal.value = true;
};

const closeReturnRequestModal = async () => {
  showReturnRequestModal.value = false
  if (selectedOrder.value) {
    await refreshReturnable(selectedOrder.value)
    returnableByOrder.value = { ...returnableByOrder.value }
  }
}


const filteredOrders = computed(() => {
  let filtered = orders.value

  if (activeTab.value !== 'all') {
    filtered = filtered.filter(order => order.status === activeTab.value)
  }

  if (searchQuery.value.trim()) {
    const query = searchQuery.value.trim().toLowerCase()
    filtered = filtered.filter(order => {
      // QUAN TRỌNG: Phải chuyển orderCode thành chữ thường để so sánh
      const orderCodeMatch = order.orderCode.toLowerCase().includes(query)
      console.log('Order Code:', order.orderCode, 'Lowercase:', order.orderCode.toLowerCase(), 'Query:', query, 'Match:', orderCodeMatch);

      const productNameMatch = order.orderDetails?.some(detail =>
          detail.productName.toLowerCase().includes(query)
      )
      return orderCodeMatch || productNameMatch
    })
  }

  return filtered
})

const fetchOrders = async () => {
  debugger
  loading.value = true
  error.value = ''

  try {
    const user = JSON.parse(localStorage.getItem('user'))
    const userId = user?.id
    if (!userId) throw new Error('Không tìm thấy userId trong localStorage')

    const res = await getAllOrdersByUserId(userId)
    console.log('API response:', res)
    orders.value = res.data.data || []
    for (const o of orders.value) {
    if (o.status === 'COMPLETED' && canReturn(o.updatedAt)) {
      refreshReturnable(o)
    }
  }
    console.log('Fetched orders:', res.data.data)
    
    // Kiểm tra trạng thái thanh toán cho tất cả đơn hàng PENDING
    await checkAllPaymentStatuses();
    
    // Kiểm tra và hủy đơn hàng hết hạn ngay lập tức
    checkAndAutoCancelExpiredOrders();
    
    // Kiểm tra và hoàn thành đơn hàng sau 7 ngày ngay lập tức
    checkAndAutoCompleteDeliveredOrders();
    
    // Khởi động lại timer nếu có đơn hàng cần cập nhật
    startRepayTimerIfNeeded();
  } catch (err) {
    console.error('Error fetching orders:', err)
    error.value = 'Không thể tải đơn hàng. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

const goToOrderDetail = (orderId) => {
  router.push(`/order-detail/${orderId}`)
}

const getOrderCount = (tabId) => {
  return tabId === 'all'
      ? orders.value.length
      : orders.value.filter(order => order.status === tabId).length
}

const formatPrice = (price) => {
  return new Intl.NumberFormat('vi-VN').format(price)
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': 'Chờ xác nhận',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao hàng',
    'DELIVERED': 'Đã giao hàng',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy',
    'FAIL': 'Thanh toán thất bại'
  }
  return statusMap[status] || status
}

const getOrderActions = (order) => {
  const actions = []
  const user = JSON.parse(localStorage.getItem('user'))
  const userRole = user?.role?.name || 'USER'

  // USER - chỉ làm được khi PENDING
  if (userRole === 'USER') {
    if (order.status === 'PENDING') {
      actions.push(
          { text: 'Hủy đơn hàng', action: 'cancel', type: 'outline' },
      )
    }
    if (order.status === 'DELIVERED') {
      actions.push(
          { text: 'Xác nhận đã nhận', action: 'confirm_received', type: 'primary' }
      )
    }
    if (order.status === 'COMPLETED' || order.status === 'CANCELLED') {
      actions.push(
          { text: 'Mua lại', action: 'reorder', type: 'outline' }
      )
    }
  }

  // ADMIN/STAFF - làm được nhiều hơn
  if (userRole === 'ADMIN' || userRole === 'STAFF') {
    // Hủy được khi PENDING, CONFIRMED, PROCESSING
    if (['PENDING', 'CONFIRMED', 'PROCESSING'].includes(order.status)) {
      actions.push({ text: 'Hủy đơn hàng', action: 'cancel', type: 'outline' })
    }

    // Các action chuyển trạng thái
    if (order.status === 'PENDING') {
      actions.push({ text: 'Xác nhận', action: 'confirm', type: 'primary' })
    }
    if (order.status === 'CONFIRMED') {
      actions.push({ text: 'Xử lý', action: 'process', type: 'primary' })
    }
    if (order.status === 'PROCESSING') {
      actions.push({ text: 'Giao hàng', action: 'ship', type: 'primary' })
    }
    if (order.status === 'SHIPPING') {
      actions.push({ text: 'Đã giao', action: 'deliver', type: 'primary' })
    }
    if (order.status === 'DELIVERED') {
      actions.push({ text: 'Hoàn thành', action: 'complete', type: 'primary' })
    }

    if (order.status === 'COMPLETED' || order.status === 'CANCELLED') {
      actions.push({ text: 'Mua lại', action: 'reorder', type: 'outline' })
    }
  }

  return actions
}

// Hàm xử lý mua lại
const handleReorder = async (order) => {
  const orderId = order.id
  updatingOrders.value.push(orderId)

  try {
    const user = JSON.parse(localStorage.getItem('user'))
    const userId = user?.id
    // Thêm từng sản phẩm vào giỏ hàng
    const promises = order.orderDetails.map(detail =>
        cartApi.addToCart({
          userId,
          productVariantId: detail.productVariantId,
          quantity: detail.quantity
        })
    )
    await Promise.all(promises)
    setTimeout(() => {
      router.push('/carts')
    }, 1)

  } catch (error) {
    console.error('Error reordering:', error)
    showToast('Có lỗi xảy ra khi thêm vào giỏ hàng', 'error')
  } finally {
    updatingOrders.value = updatingOrders.value.filter(id => id !== orderId)
  }
}

// Updated handleAction to handle confirm_received
const handleAction = async (action, order) => {
  switch (action) {
    case 'cancel':
      orderToProcess.value = order
      currentAction.value = 'cancel'
      showConfirmModal.value = true
      break
    case 'confirm_received':
      orderToProcess.value = order
      currentAction.value = 'confirm_received'
      showConfirmModal.value = true
      break
    case 'reorder':
      await handleReorder(order)
      break
    default:
      console.log('Unknown action:', action)
  }
}

const closeModal = () => {
  showConfirmModal.value = false
  orderToProcess.value = null
  currentAction.value = ''
  isProcessing.value = false
}

// Updated confirmAction to handle both cancel and confirm_received
const confirmAction = async () => {
  if (!orderToProcess.value) return

  isProcessing.value = true
  const orderId = orderToProcess.value.id

  updatingOrders.value.push(orderId)

  try {
    if (currentAction.value === 'cancel') {
      await deleteOrderById(orderId)

      // Gửi email thông báo hủy đơn hàng bởi người dùng
      try {
        await sendOrderCancelledByUserEmail(orderId);
      } catch (emailError) {
        console.warn("Không thể gửi email thông báo hủy đơn hàng:", emailError);
        // Không hiển thị lỗi cho user vì đơn hàng đã được hủy thành công
      }

      const orderIndex = orders.value.findIndex(order => order.id === orderId)
      if (orderIndex !== -1) {
        orders.value[orderIndex].status = 'CANCELLED'
      }

      showToast('Đơn hàng đã được hủy thành công', 'success')
    } else if (currentAction.value === 'confirm_received') {
      await updateOrderStatus(orderId, 'COMPLETED')

      // Gửi email thông báo hoàn thành đơn hàng
      try {
        await sendOrderCompletedEmail(orderId);
      } catch (emailError) {
        console.warn("Không thể gửi email thông báo hoàn thành đơn hàng:", emailError);
        // Không hiển thị lỗi cho user vì đơn hàng đã được hoàn thành thành công
      }

      const orderIndex = orders.value.findIndex(order => order.id === orderId)
      if (orderIndex !== -1) {
        orders.value[orderIndex].status = 'COMPLETED'
        // Update the updatedAt timestamp to current time for return calculation
        orders.value[orderIndex].updatedAt = new Date().toISOString()
      }

      showToast('Đã xác nhận nhận hàng thành công', 'success')
    }

    closeModal()
  } catch (error) {
    console.error(`Error ${currentAction.value === 'cancel' ? 'canceling' : 'confirming'} order:`, error)
    showToast(`Có lỗi xảy ra khi ${currentAction.value === 'cancel' ? 'hủy' : 'xác nhận'} đơn hàng`, 'error')
  } finally {
    updatingOrders.value = updatingOrders.value.filter(id => id !== orderId)
    isProcessing.value = false
  }
}

// Event handler cho WebSocket messages
const handleOrderUpdate = (data) => {
  console.log('Received order update via WebSocket:', data);
  // Refresh danh sách đơn hàng khi có cập nhật
  fetchOrders();
}

const showReviewForm = ref(false)
const productToReview = ref(null)
const reviewingOrderId = ref(null)

const openReviewForm = (order, detail) => {
  showReviewForm.value = true
  productToReview.value = detail
  reviewingOrderId.value = order.id
}

// Thêm hàm xử lý sau khi đánh giá xong
const handleAfterReview = ({ orderDetailId, reviewId, content, rating }) => {
  showReviewForm.value = false;
  fetchOrders(); // Gọi lại API lấy đơn hàng để đảm bảo đồng bộ dữ liệu
};

const showShowReview = ref(false)
const reviewDetail = ref(null)

const openShowReview = (detail) => {
  // Truyền thêm thông tin về thời gian đơn hàng hoàn thành
  const order = orders.value.find(order => 
    order.orderDetails.some(od => od.reviewId === detail.reviewId)
  );
  
  reviewDetail.value = {
    ...detail,
    orderUpdatedAt: order?.updatedAt // Thời gian đơn hàng hoàn thành
  }
  showShowReview.value = true
}

const handleReviewUpdated = ({ reviewId, content, rating }) => {
  showShowReview.value = false;
  // Tìm order chứa review này
  for (const order of orders.value) {
    const detail = order.orderDetails.find(d => d.reviewId === reviewId);
    if (detail) {
      detail.reviewContent = content;
      detail.reviewRating = rating;
      break;
    }
  }
};

const canReview = (updatedAt) => {
  if (!updatedAt) return false;
  const updated = new Date(updatedAt);
  const now = new Date();
  const expire = new Date(updated.getTime() + 7 * 24 * 60 * 60 * 1000);
  return now <= expire;
};

// Kiểm tra có thể hiển thị nút thanh toán lại không
const canShowRepayButton = (order) => {
  // Chỉ hiển thị cho đơn hàng online (không phải COD)
  if (order.paymentMethod?.type !== 'ONLINE_PAYMENT') {
    return false;
  }
  
  // Nếu đã thanh toán thành công, không hiển thị nút
  if (paidOrders.value.has(order.id)) {
    return false;
  }
  
  // Hiển thị cho đơn hàng PENDING (chưa thanh toán)
  if (order.status === 'PENDING') {
    return true;
  }
  
  // Hiển thị cho đơn hàng FAIL (thanh toán thất bại) - không giới hạn thời gian
  if (order.status === 'FAIL' || order.paymentStatus === 'FAIL') {
    return true;
  }
  
  return false;
};

// Kiểm tra có thể thanh toán lại trong 15 phút không (chỉ cho PENDING)
const canRepay = (createdAt) => {
  if (!createdAt) return false;
  const created = new Date(createdAt);
  const now = new Date(currentTime.value); // Sử dụng currentTime để trigger reactivity
  const expire = new Date(created.getTime() + 15 * 60 * 1000); // 15 phút
  return now <= expire;
};

// Lấy thời gian còn lại để thanh toán lại
const getRepayTimeLeft = (createdAt) => {
  if (!createdAt) return '';
  const created = new Date(createdAt);
  const now = new Date(currentTime.value); // Sử dụng currentTime để trigger reactivity
  const expire = new Date(created.getTime() + 15 * 60 * 1000); // 15 phút
  const diff = expire - now;
  
  if (diff <= 0) return 'Hết hạn';
  
  const minutes = Math.floor(diff / (1000 * 60));
  const seconds = Math.floor((diff % (1000 * 60)) / 1000);
  
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
};

// Lấy thời gian còn lại để xác nhận đơn hàng (7 ngày)
const getConfirmTimeLeft = (updatedAt) => {
  if (!updatedAt) return '';
  const delivered = new Date(updatedAt);
  const now = new Date(currentTime.value);
  const expire = new Date(delivered.getTime() + 60 * 1000);
  const diff = expire - now;
  
  if (diff <= 0) return 'Tự động hoàn thành';
  
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  
  if (days > 0) {
    return `${days} ngày ${hours}h`;
  } else {
    return `${hours}h`;
  }
};

// Format thời gian tạo đơn hàng
const formatOrderTime = (createdAt) => {
  if (!createdAt) return '';
  const date = new Date(createdAt);
  return date.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// Xử lý thanh toán lại
const handleRepay = async (order) => {
  // Kiểm tra điều kiện thanh toán lại
  if (!canShowRepayButton(order)) {
    if (order.status === 'PENDING' && !canRepay(order.createdAt)) {
      showToast('Đã hết thời gian thanh toán lại!', 'error');
    } else {
      showToast('Không thể thanh toán lại đơn hàng này!', 'error');
    }
    return;
  }
  
  // Thêm order vào danh sách đang xử lý
  updatingOrders.value.push(order.id);
  
  try {
    // Sử dụng hàm retryVnPay từ PaymentService
    const paymentUrl = await retryVnPay(order.orderCode);
    
    if (typeof paymentUrl === 'string' && paymentUrl.startsWith('http')) {
      // Chuyển đến VNPay
      window.location.assign(paymentUrl);
    } else {
      showToast('Không thể tạo link thanh toán. Vui lòng thử lại!', 'error');
    }
  } catch (error) {
    console.error('Lỗi khi thanh toán lại:', error);
    showToast('Có lỗi xảy ra khi thanh toán lại. Vui lòng thử lại!', 'error');
  } finally {
    // Xóa order khỏi danh sách đang xử lý
    updatingOrders.value = updatingOrders.value.filter(id => id !== order.id);
  }
};

// Timer để cập nhật thời gian đếm ngược
let repayTimer = null;

// Hàm kiểm tra và tự động hủy đơn hàng hết hạn
const checkAndAutoCancelExpiredOrders = async () => {
  const now = new Date(currentTime.value);
  const expiredOrders = [];
  
  // Tìm các đơn hàng đã hết hạn
  orders.value.forEach(order => {
    if (order.status === 'PENDING' && 
        order.paymentMethod?.type === 'ONLINE_PAYMENT' &&
        order.createdAt &&
        !autoCancelledOrders.value.has(order.id) &&
        !paidOrders.value.has(order.id)) { // Chưa được hủy tự động và chưa thanh toán
      const created = new Date(order.createdAt);
      const expire = new Date(created.getTime() + 15 * 60 * 1000); // 15 phút
      
      if (now > expire) {
        expiredOrders.push(order);
      }
    }
  });
  
  // Tự động hủy các đơn hàng hết hạn
  for (const order of expiredOrders) {
    try {
      console.log(`Tự động hủy đơn hàng hết hạn: ${order.orderCode}`);
      
      // Đánh dấu đơn hàng đã được hủy tự động
      autoCancelledOrders.value.add(order.id);
      
      await deleteOrderById(order.id);
      
      // Cập nhật trạng thái trong danh sách local
      const orderIndex = orders.value.findIndex(o => o.id === order.id);
      if (orderIndex !== -1) {
        orders.value[orderIndex].status = 'CANCELLED';
        orders.value[orderIndex].updatedAt = new Date().toISOString();
      }
      
      // Hiển thị thông báo
      showToast(`Đơn hàng ${order.orderCode} đã được tự động hủy do hết thời gian thanh toán (15 phút)`, 'warning');
      
    } catch (error) {
      console.error(`Lỗi khi tự động hủy đơn hàng ${order.orderCode}:`, error);
      // Nếu lỗi, xóa khỏi danh sách đã hủy để có thể thử lại
      autoCancelledOrders.value.delete(order.id);
    }
  }
};

// Hàm kiểm tra và tự động hoàn thành đơn hàng sau 7 ngày
const checkAndAutoCompleteDeliveredOrders = async () => {
  const now = new Date(currentTime.value);
  const autoCompletedOrders = [];
  
  // Tìm các đơn hàng DELIVERED đã quá 7 ngày
  orders.value.forEach(order => {
    if (order.status === 'DELIVERED' && 
        order.updatedAt &&
        !autoCancelledOrders.value.has(order.id)) { // Chưa được xử lý tự động
      const deliveredDate = new Date(order.updatedAt);
      const expire = new Date(deliveredDate.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 ngày
      
      if (now > expire) {
        autoCompletedOrders.push(order);
      }
    }
  });
  
  // Tự động hoàn thành các đơn hàng
  for (const order of autoCompletedOrders) {
    try {
      console.log(`Tự động hoàn thành đơn hàng sau 7 ngày: ${order.orderCode}`);
      
      // Đánh dấu đơn hàng đã được xử lý tự động
      autoCancelledOrders.value.add(order.id);
      
      await updateOrderStatus(order.id, 'COMPLETED');
      
      // Cập nhật trạng thái trong danh sách local
      const orderIndex = orders.value.findIndex(o => o.id === order.id);
      if (orderIndex !== -1) {
        orders.value[orderIndex].status = 'COMPLETED';
        // Cập nhật updatedAt để tính toán thời gian trả hàng
        orders.value[orderIndex].updatedAt = new Date().toISOString();
         await refreshReturnable(orders.value[orderIndex])
  returnableByOrder.value = { ...returnableByOrder.value }
      }
      
      // Hiển thị thông báo
      showToast(`Đơn hàng ${order.orderCode} đã được tự động hoàn thành sau 7 ngày`, 'success');
      
    } catch (error) {
      console.error(`Lỗi khi tự động hoàn thành đơn hàng ${order.orderCode}:`, error);
      // Nếu lỗi, xóa khỏi danh sách đã xử lý để có thể thử lại
      autoCancelledOrders.value.delete(order.id);
    }
  }
};

// Hàm khởi động timer nếu cần thiết
const startRepayTimerIfNeeded = () => {
  // Nếu timer đã chạy, không cần khởi động lại
  if (repayTimer) return;
  
  // Nếu có đơn hàng cần cập nhật thời gian, khởi động timer
  if (ordersNeedingUpdate.value.length > 0) {
    repayTimer = setInterval(async () => {
      // Force re-render để cập nhật thời gian
      currentTime.value = Date.now();
      
      // Kiểm tra trạng thái thanh toán định kỳ (mỗi 30 giây)
      if (Math.floor(Date.now() / 1000) % 30 === 0) {
        await checkAllPaymentStatuses();
      }
      
      // Kiểm tra và tự động hủy đơn hàng hết hạn
      checkAndAutoCancelExpiredOrders();
      
      // Kiểm tra và tự động hoàn thành đơn hàng sau 7 ngày
      checkAndAutoCompleteDeliveredOrders();
      
      // Kiểm tra xem còn đơn hàng nào cần cập nhật không
      if (ordersNeedingUpdate.value.length === 0) {
        // Nếu không còn đơn hàng nào cần cập nhật, dừng timer
        if (repayTimer) {
          clearInterval(repayTimer);
          repayTimer = null;
        }
      }
    }, 1000);
  }
};

onMounted(() => {
  fetchOrders()
  emitter.on('order', handleOrderUpdate);
})

onUnmounted(() => {
  // Gỡ bỏ listener khi component bị unmount để tránh memory leak
  emitter.off('order', handleOrderUpdate);
  
  // Clear timer
  if (repayTimer) {
    clearInterval(repayTimer);
  }
});
</script>

<style scoped>
.btn-warning {
  background-color: #ff9800;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 4px;
  font-weight: 500;
  cursor: pointer;
}
.btn-warning:hover {
  background-color: #f57c00;
}

.order-management {
  max-width: 1330px;
  margin: 0 auto;
  padding: 20px;
  margin-bottom: 50px;
}

.loading {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.spinner, .mini-spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #000;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

.spinner {
  width: 40px;
  height: 40px;
}

.mini-spinner {
  width: 24px;
  height: 24px;
  border-width: 3px;
  margin: 0;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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
}

.main-content {
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 5px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.tabs {
  display: flex;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  overflow-x: auto;
}

.tab {
  padding: 18px 24px;
  cursor: pointer;
  border: none;
  background: transparent;
  color: #666;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
  white-space: nowrap;
  position: relative;
}

.tab:hover {
  background: #e9ecef;
  color: #333;
}

.tab.active {
  color: #000;
  background: white;
  font-weight: 700;
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #000;
}

.search-bar {
  padding: 25px;
  border-bottom: 1px solid #f0f0f0;
}

.search-input {
  width: 100%;
  padding: 15px 20px;
  border: 2px solid #f0f0f0;
  border-radius: 25px;
  font-size: 14px;
  transition: all 0.3s ease;
  outline: none;
  background: #fafafa;
}

.search-input:focus {
  border-color: #000;
  background: white;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.1);
}

.content {
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #666;
}

.empty-state h3 {
  margin-bottom: 12px;
  color: #333;
  font-size: 24px;
  font-weight: 700;
}

.order-item {
  padding: 30px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  position: relative;
}

.order-item:hover {
  background: #fafafa;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item.updating {
  opacity: 0.7;
  pointer-events: none;
}

.order-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  border-radius: 12px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.order-header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.order-time {
  font-size: 12px;
  color: #666;
}

.order-id-container {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 8px 16px;
  border-radius: 12px;
}

.order-id-container:hover {
  background: #f5f5f5;
}

.order-id {
  font-weight: 700;
  color: #000;
  font-size: 18px;
  margin-right: 12px;
}

.click-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 13px;
  font-weight: 500;
}

.hint-icon {
  width: 16px;
  height: 16px;
}

.order-status {
  padding: 8px 16px;
  border-radius: 25px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-pending { background: #f5f5f5; color: #424242; }
.status-confirmed { background: #e3f2fd; color: #1a365d; }
.status-processing { background: #f3e5f5; color: #4a148c; }
.status-shipping { background: #fff8e1; color: #5a4a00; }
.status-delivered { background: #e8f4f8; color: #1e4a54; }
.status-completed { background: #f0f9f0; color: #2d5a2d; }
.status-cancelled { background: #ffebee; color: #c62828; }
.status-fail { background: #ffebee; color: #d32f2f; }

.product-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 12px;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid #e9ecef;
}

.product-info {
  flex: 1;
}

.product-name {
  font-weight: 600;
  margin-bottom: 8px;
  color: #000;
  font-size: 15px;
}

.product-variant {
  color: #666;
  font-size: 13px;
  margin-bottom: 6px;
}

.product-quantity {
  color: #666;
  font-size: 13px;
}

.product-price {
  text-align: right;
}

.price-current {
  font-weight: 600;
  color: #000;
  font-size: 16px;
  display: block;
  margin-bottom: 4px;
}

.price-total {
  font-size: 13px;
  color: #666;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.order-total {
  display: flex;
  align-items: center;
  gap: 15px;
}

.total-label {
  font-size: 16px;
  color: #666;
  font-weight: 500;
}

.total-amount {
  font-size: 20px;
  font-weight: 700;
  color: #000;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #000;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #333;
}

.btn-outline {
  border: 1px solid #ccc;
  background: white;
  color: #333;
}

.btn-outline:hover:not(:disabled) {
  border-color: #999;
}

.btn-success {
  background: #28a745;
  color: white;
  border: none;
}

.btn-success:hover:not(:disabled) {
  background: #218838;
}

.btn-success small {
  font-size: 10px;
  opacity: 0.9;
  margin-top: 2px;
}

.btn-success:disabled {
  background: #6c757d;
  color: white;
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-success:disabled:hover {
  background: #6c757d;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  font-size: 24px;
  color: #666;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #000;
}

.modal-body {
  padding: 20px 24px;
}

.modal-body p {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
}

.warning-text {
  color: #dc3545;
  font-size: 14px;
  font-style: italic;
}

.modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 16px 24px 24px;
}

@media (max-width: 768px) {
  .order-management {
    padding: 15px;
  }

  .order-item {
    padding: 20px;
  }

  .order-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }

  .order-footer {
    flex-direction: column;
    gap: 20px;
    align-items: stretch;
  }

  .action-buttons {
    justify-content: center;
  }

  .product-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-image {
    width: 100%;
    height: 200px;
  }

  .modal-content {
    margin: 20px;
    width: calc(100% - 40px);
  }
}

</style>