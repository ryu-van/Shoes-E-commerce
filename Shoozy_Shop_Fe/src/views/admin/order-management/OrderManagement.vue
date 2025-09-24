<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import {getAllOrders, deleteOrderById, sendOrderCancelledByShopEmail} from "@/service/OrderApi.js";
import ShowToastComponent from "@/components/ShowToastComponent.vue";
import emitter from '@/service/EvenBus.js';
import {
  connectWebSocket,
  addMessageListener,
  removeMessageListener,
} from '@/service/Websocket'

const searchQuery = ref('')
const fromDate = ref('')
const toDate = ref('')
const selectedPaymentMethod = ref('all')
const selectedStatus = ref('all')
const currentPage = ref(1)
const itemsPerPage = ref(5)
const orders = ref([])
const loading = ref(false)
const error = ref(null)
const dateError = ref('')

const toastRef = ref(null);

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type);
};

// Cancel order modal data
const showCancelConfirmModal = ref(false);
const cancelOrderId = ref(null);
const cancelOrderCode = ref('');
const cancelOrderCustomer = ref('');
const cancelDescription = ref('');
const isCancellingOrder = ref(false);

// Page size options
const pageSizeOptions = [5, 10, 15, 20, 25];

// Format helpers
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount).replace('₫', 'đ')
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleString('vi-VN')
}

// Computed để validate ngày
const isDateRangeValid = computed(() => {
  if (!fromDate.value || !toDate.value) return true
  return new Date(fromDate.value) <= new Date(toDate.value)
})

// Helper function to compare dates
const isDateInRange = (orderDate, fromDate, toDate) => {
  // Kiểm tra validation trước
  if (fromDate && toDate && new Date(fromDate) > new Date(toDate)) {
    return false; // Không filter gì cả nếu ngày không hợp lệ
  }

  const orderDateObj = new Date(orderDate);
  const fromDateObj = fromDate ? new Date(fromDate) : null;
  const toDateObj = toDate ? new Date(toDate) : new Date(); // Nếu không có toDate, dùng ngày hôm nay

  // Set time to start of day for fromDate
  if (fromDateObj) {
    fromDateObj.setHours(0, 0, 0, 0);
  }

  // Set time to end of day for toDate
  toDateObj.setHours(23, 59, 59, 999);

  // Check if order date is in range
  if (fromDateObj && toDateObj) {
    return orderDateObj >= fromDateObj && orderDateObj <= toDateObj;
  } else if (fromDateObj) {
    return orderDateObj >= fromDateObj;
  } else if (toDate) {
    return orderDateObj <= toDateObj;
  }

  return true;
}

// Fetch orders from API
const fetchOrders = async () => {
  loading.value = true
  error.value = null
  try {
    const response = await getAllOrders()
    orders.value = response.data.data.map(order => ({
      id: order.id,
      code: order.orderCode,
      totalAmount: formatCurrency(order.totalMoney),
      customerName: order.fullname || '',
      createdAt: formatDate(order.createdAt),
      rawCreatedAt: order.updatedAt, // Giữ nguyên ngày gốc để filter
      paymentMethod: order.paymentMethod?.name || 'Chưa xác định',
      paymentType: order.paymentMethod?.type,
      status: order.status || '',
      active: order.active,
      phoneNumber: order.phoneNumber || '',
      address: order.address,
      note: order.note,
      coupon: order.coupon,
      shippingMethod: order.shippingMethod,
      shippingDate: order.shippingDate
    }))
        // Sắp xếp theo thời gian tạo mới nhất lên đầu
        .sort((a, b) => new Date(b.rawCreatedAt) - new Date(a.rawCreatedAt))
  } catch (err) {
    error.value = 'Không thể tải dữ liệu đơn hàng'
    console.error('Error fetching orders:', err)
  } finally {
    loading.value = false
  }
}

// Event handler cho WebSocket messages
const handleOrderUpdate = (data) => {
  console.log('Received order update via WebSocket:', data);
  // Refresh danh sách đơn hàng khi có cập nhật
  fetchOrders();
}


// Computed properties
const filteredOrders = computed(() => {
  let filtered = orders.value

  // Filter by search query
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase().trim()
    filtered = filtered.filter(order =>
        order.code.toLowerCase().trim().includes(query) ||
        order.customerName.toLowerCase().trim().includes(query) ||
        order.phoneNumber?.toLowerCase().trim().includes(query)
    )
  }

  // Filter by date range
  if (fromDate.value || toDate.value) {
    filtered = filtered.filter(order => {
      return isDateInRange(order.rawCreatedAt, fromDate.value, toDate.value);
    });
  }
  // Filter by payment method
  if (selectedPaymentMethod.value !== 'all') {
    filtered = filtered.filter(order =>
        order.paymentType == selectedPaymentMethod.value
    )
  }
  // Filter by status
  if (selectedStatus.value !== 'all') {
    filtered = filtered.filter(order =>
        order.status === selectedStatus.value
    )
  }

  return filtered
})

const allStatusCounts = computed(() => {
  // Filter orders bởi tất cả điều kiện NGOẠI TRỪ status
  let filtered = orders.value

  // Filter by search query
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase().trim()
    filtered = filtered.filter(order =>
        order.code.toLowerCase().trim().includes(query) ||
        order.customerName.toLowerCase().trim().includes(query) ||
        order.phoneNumber?.toLowerCase().trim().includes(query)
    )
  }

  // Filter by date range
  if (fromDate.value || toDate.value) {
    filtered = filtered.filter(order => {
      return isDateInRange(order.rawCreatedAt, fromDate.value, toDate.value);
    });
  }

  // Filter by payment method
  if (selectedPaymentMethod.value !== 'all') {
    filtered = filtered.filter(order =>
        order.paymentType == selectedPaymentMethod.value
    )
  }

  // Tính count cho từng status từ orders đã filter
  const counts = {
    all: filtered.length,
    cancelled: 0,
    pending: 0,
    processing: 0,
    confirmed: 0,
    shipping: 0,
    delivered: 0,
    completed: 0
  }

  filtered.forEach(order => {
    if (counts.hasOwnProperty(order.status.toLowerCase())) {
      counts[order.status.toLowerCase()]++
    }
  })

  return counts
})

const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value
  const end = start + itemsPerPage.value
  return filteredOrders.value.slice(start, end)
})

const totalPages = computed(() => {
  return Math.ceil(filteredOrders.value.length / itemsPerPage.value)
})

// Computed pagination info
const paginationInfo = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value + 1;
  const end = Math.min(currentPage.value * itemsPerPage.value, filteredOrders.value.length);
  return {
    start,
    end,
    total: filteredOrders.value.length
  };
});

// Computed visible pages for pagination
const visiblePages = computed(() => {
  const current = currentPage.value;
  const total = totalPages.value;
  const pages = [];

  // Luôn hiển thị trang đầu
  if (current > 3) {
    pages.push(1);
    if (current > 4) {
      pages.push('...');
    }
  }

  // Hiển thị các trang xung quanh trang hiện tại
  const start = Math.max(1, current - 2);
  const end = Math.min(total, current + 2);

  for (let i = start; i <= end; i++) {
    if (!pages.includes(i)) {
      pages.push(i);
    }
  }

  // Luôn hiển thị trang cuối
  if (current < total - 2) {
    if (current < total - 3) {
      pages.push('...');
    }
    if (!pages.includes(total)) {
      pages.push(total);
    }
  }

  return pages;
});

// Method để validate và show error
const validateDateRange = () => {
  const today = new Date()
  today.setHours(23, 59, 59, 999) // Set to end of today
  const currentYear = new Date().getFullYear()

  // Kiểm tra fromDate
  if (fromDate.value) {
    const fromDateObj = new Date(fromDate.value)
    if (fromDateObj > today) {
      dateError.value = 'Ngày bắt đầu không thể lớn hơn ngày hiện tại'
      showToast('Ngày bắt đầu không thể lớn hơn ngày hiện tại!', 'error')
      return false
    }
  }

  // Kiểm tra toDate
  if (toDate.value) {
    const toDateObj = new Date(toDate.value)
    if (toDateObj > today) {
      dateError.value = 'Ngày kết thúc không thể lớn hơn ngày hiện tại'
      return false
    }

    // Kiểm tra năm kết thúc
    if (toDateObj.getFullYear() > currentYear) {
      dateError.value = 'Năm kết thúc không thể lớn hơn năm hiện tại'
      showToast('Năm kết thúc không thể lớn hơn năm hiện tại!', 'error')
      return false
    }
  }

  // Kiểm tra range
  if (fromDate.value && toDate.value) {
    if (new Date(fromDate.value) > new Date(toDate.value)) {
      dateError.value = 'Ngày bắt đầu không thể lớn hơn ngày kết thúc'
      showToast('Ngày bắt đầu không thể lớn hơn ngày kết thúc!', 'error')
      return false
    }
  }

  dateError.value = ''
  return true
}

// Methods
const handleSearch = () => {
  if (!validateDateRange()) {
    return
  }
  currentPage.value = 1
}

// Event handlers cho date inputs
const handleFromDateChange = () => {
  validateDateRange()
  if (isDateRangeValid.value) {
    handleSearch()
  }
}

const handleToDateChange = () => {
  validateDateRange()
  if (isDateRangeValid.value) {
    handleSearch()
  }
}

const clearFilter = () => {
  searchQuery.value = ''
  fromDate.value = ''
  toDate.value = ''
  selectedPaymentMethod.value = 'all'
  selectedStatus.value = 'all'
  currentPage.value = 1
  dateError.value = '' // Clear error
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value && page !== '...') {
    currentPage.value = page;
  }
};

const deleteOrder = async (id) => {
  // Tìm thông tin đơn hàng để hiển thị trong modal
  const order = orders.value.find(o => o.id === id);
  if (!order) {
    showToast("Không tìm thấy thông tin đơn hàng!", "error");
    return;
  }
  
  // Hiển thị modal xác nhận hủy đơn hàng
  showCancelConfirmModal.value = true;
  cancelOrderId.value = id;
  cancelOrderCode.value = order.code;
  cancelOrderCustomer.value = order.customerName;
  cancelDescription.value = '';
}

// Check if order can be cancelled
const canCancelOrder = (status) => {
  return status === 'PENDING' || status === 'CONFIRMED';
}

const getStatusBadgeClass = (status) => {
  switch (status.toLowerCase()) {
    case 'completed':
      return 'badge status-completed'
    case 'delivered':
      return 'badge status-delivered'
    case 'shipping':
      return 'badge status-shipping'
    case 'confirmed':
      return 'badge status-confirmed'
    case 'processing':
      return 'badge status-processing'
    case 'pending':
      return 'badge status-pending'
    case 'cancelled':
      return 'badge status-cancelled'
    default:
      return 'badge status-default'
  }
}

const getStatusText = (status) => {
  switch (status.toUpperCase()) {
    case 'COMPLETED':
      return 'Hoàn thành'
    case 'DELIVERED':
      return 'Đã giao hàng'
    case 'SHIPPING':
      return 'Đang vận chuyển'
    case 'CONFIRMED':
      return 'Đã xác nhận'
    case 'PROCESSING':
      return 'Đang xử lý'
    case 'PENDING':
      return 'Chờ xác nhận'
    case 'CANCELLED':
      return 'Đã hủy'
    default:
      return 'Chưa xác định'
  }
}

const getPaymentMethodText = (paymentType) => {
  switch (paymentType) {
    case 'CASH':
      return 'Thanh toán khi nhận hàng'
    case 'ONLINE_PAYMENT':
      return 'Chuyển khoản ngân hàng'
    default:
      return 'Chưa xác định'
  }
}

const getPaymentMethodBadgeClass = (paymentType) => {
  switch (paymentType) {
    case 'COD':
      return 'badge payment-cod'
    case 'ONLINE_PAYMENT':
      return 'badge payment-online'
    default:
      return 'badge payment-default'
  }
}

// Watch for itemsPerPage changes
watch(itemsPerPage, () => {
  currentPage.value = 1;
});

// Watch for filters
watch([searchQuery, selectedPaymentMethod, selectedStatus, fromDate, toDate], () => {
  currentPage.value = 1;
});

// Cancel order functions
const confirmCancelOrder = async () => {
  if (!cancelOrderId.value) {
    showToast("Không tìm thấy ID đơn hàng!", "error");
    return;
  }

  isCancellingOrder.value = true;
  try {
    await deleteOrderById(cancelOrderId.value);
    
    // Gửi email thông báo hủy đơn hàng bởi shop
    try {
      await sendOrderCancelledByShopEmail(cancelOrderId.value, cancelDescription.value || 'Hủy đơn hàng theo yêu cầu');
    } catch (emailError) {
      console.warn("Không thể gửi email thông báo hủy đơn hàng:", emailError);
      // Không hiển thị lỗi cho admin vì đơn hàng đã được hủy thành công
    }
    
    showToast("Hủy đơn hàng thành công!", "success");
    fetchOrders();
    hideCancelConfirmModal();
  } catch (error) {
    console.error("Lỗi khi hủy đơn hàng:", error);
    showToast("Hủy đơn hàng thất bại!", "error");
  } finally {
    isCancellingOrder.value = false;
  }
};

const hideCancelConfirmModal = () => {
  showCancelConfirmModal.value = false;
  cancelOrderId.value = null;
  cancelOrderCode.value = '';
  cancelOrderCustomer.value = '';
  cancelDescription.value = '';
  isCancellingOrder.value = false;
};

// Lifecycle
onMounted(() => {
  fetchOrders();
  // Kết nối WebSocket và đăng ký listeners
  connectWebSocket();
  addMessageListener(handleOrderUpdate);
  // Đăng ký lắng nghe sự kiện 'order' từ event bus
  emitter.on('order', handleOrderUpdate);
});

onUnmounted(() => {
  // Gỡ bỏ listeners khi component bị unmount để tránh memory leak
  removeMessageListener(handleOrderUpdate);
  emitter.off('order', handleOrderUpdate);
  // Không disconnect WebSocket vì có thể được sử dụng bởi các component khác
  // disconnectWebSocket();
});
</script>

<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">📋 Quản lý đơn hàng</h2>

    <!-- Filters Row -->
    <div class="row gy-2 gx-3 align-items-center mb-3 flex-wrap">
      <!-- Search -->
      <div class="col-md-3">
        <input
            type="text"
            class="form-control"
            placeholder="🔍 Tìm mã đơn hàng, tên khách hàng..."
            v-model="searchQuery"
            @input="handleSearch"
        >
      </div>

      <!-- Date Range -->
      <div class="col-md-2 position-relative">
        <input
            type="date"
            class="form-control"
            :class="{ 'is-invalid': dateError }"
            placeholder="Từ ngày"
            v-model="fromDate"
            @change="handleFromDateChange"
        >
      </div>
      <div class="col-md-2 position-relative">
        <input
            type="date"
            class="form-control"
            :class="{ 'is-invalid': dateError }"
            placeholder="Đến ngày"
            v-model="toDate"
            @change="handleToDateChange"
        >
        <!-- Error tooltip - positioned absolutely -->
        <div v-if="dateError" class="error-tooltip">
          <i class="fas fa-exclamation-triangle"></i>
          {{ dateError }}
        </div>
      </div>

      <!-- Payment Method Filter -->
      <div class="col-md-3">
        <select class="form-select" v-model="selectedPaymentMethod">
          <option value="all">Phương thức TT</option>
          <option value="CASH">Thanh toán khi nhận hàng</option>
          <option value="ONLINE_PAYMENT">Chuyển khoản</option>
        </select>
      </div>

      <!-- Clear Filter Button -->
      <div class="col-md-2">
        <button class="btn btn-secondary w-100" style="height: 38px" @click="clearFilter">
          Xóa bộ lọc
        </button>
      </div>
    </div>

    <!-- Tab Navigation -->
    <div class="tab-navigation mb-3">
      <ul class="nav nav-tabs custom-tabs">
        <li class="nav-item">
          <a class="nav-link"
             :class="{ active: selectedStatus === 'all' }"
             href="#"
             @click.prevent="selectedStatus = 'all'">
            TẤT CẢ ({{ allStatusCounts.all }})
          </a>
        </li>
        <li class="nav-item">
          <a class="nav-link"
             :class="{ active: selectedStatus === 'PENDING' }"
             href="#"
             @click.prevent="selectedStatus = 'PENDING'">
            CHỜ XÁC NHẬN ({{ allStatusCounts.pending }})
          </a>
        </li>
        <li class="nav-item">
          <a class="nav-link"
             :class="{ active: selectedStatus === 'CONFIRMED' }"
             href="#"
             @click.prevent="selectedStatus = 'CONFIRMED'">
            ĐÃ XÁC NHẬN ({{ allStatusCounts.confirmed }})
          </a>
        </li>
        <li class="nav-item">
          <a class="nav-link"
             :class="{ active: selectedStatus === 'PROCESSING' }"
             href="#"
             @click.prevent="selectedStatus = 'PROCESSING'">
            ĐANG XỬ LÝ ({{ allStatusCounts.processing }})
          </a>
        </li>
        <li class="nav-item">
          <a class="nav-link"
             :class="{ active: selectedStatus === 'SHIPPING' }"
             href="#"
             @click.prevent="selectedStatus = 'SHIPPING'">
            ĐANG VẬN CHUYỂN ({{ allStatusCounts.shipping }})
          </a>
        </li>
        <li class="nav-item">
          <a class="nav-link"
             :class="{ active: selectedStatus === 'DELIVERED' }"
             href="#"
             @click.prevent="selectedStatus = 'DELIVERED'">
            ĐÃ GIAO HÀNG ({{ allStatusCounts.delivered }})
          </a>
        </li>
        <li class="nav-item">
          <a class="nav-link"
             :class="{ active: selectedStatus === 'COMPLETED' }"
             href="#"
             @click.prevent="selectedStatus = 'COMPLETED'">
            HOÀN THÀNH ({{ allStatusCounts.completed }})
          </a>
        </li>
        <li class="nav-item">
          <a class="nav-link"
             :class="{ active: selectedStatus === 'CANCELLED' }"
             href="#"
             @click.prevent="selectedStatus = 'CANCELLED'">
            ĐÃ HỦY ({{ allStatusCounts.cancelled }})
          </a>
        </li>
      </ul>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Đang tải...</span>
      </div>
      <p class="mt-2 text-muted">Đang tải dữ liệu đơn hàng...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="alert alert-danger" role="alert">
      <i class="fas fa-exclamation-triangle me-2"></i>
      <i class="fas fa-exclamation-triangle me-2"></i>
      {{ error }}
      <button class="btn btn-sm btn-outline-danger ms-2" @click="fetchOrders">
        Thử lại
      </button>
    </div>

    <!-- Orders Table -->
    <div v-else style="margin-top: 32px" class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
        <tr>
          <th class="text-center" style="width: 5%">STT</th>
          <th class="text-center">Mã đơn hàng</th>
          <th class="text-center">Tên khách hàng</th>
          <th class="text-center">SĐT</th>
          <th class="text-center">Tổng tiền</th>
          <th class="text-center">Mã giảm giá</th>
          <th class="text-center">Ngày tạo</th>
          <th class="text-center">Phương thức TT</th>
          <th class="text-center">Trạng thái</th>
          <th class="text-center" style="width: 20%">Hành động</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="paginatedOrders.length === 0">
          <td colspan="11" class="text-center">Không có đơn hàng nào.</td>
        </tr>
        <tr v-for="(order, index) in paginatedOrders" :key="order.id">
          <td class="text-center">{{ (currentPage - 1) * itemsPerPage + index + 1 }}</td>
          <td class="text-center">
            <strong>{{ order.code }}</strong>
          </td>
          <td>
            <span v-if="order.customerName === 'Khách lẻ'" class="badge bg-light text-warning">
              {{ order.customerName }}
            </span>
            <span v-else>{{ order.customerName }}</span>
          </td>
          <td class="text-center">{{ order.phoneNumber || 'N/A' }}</td>
          <td class="fw-bold text-success">{{ order.totalAmount }}</td>
          <td class="text-center">
            <div v-if="order.coupon" class="coupon-info">
              <span class="badge bg-info text-white" style="font-size: 12px; padding: 4px 8px; border-radius: 12px">
                {{ order.coupon.code || order.coupon.name }}
              </span>
            </div>
            <span v-else class="text-muted small">Không có</span>
          </td>
          <td class="text-center small">{{ order.createdAt }}</td>
          <td class="text-center">
            <span :class="getPaymentMethodBadgeClass(order.paymentType)" style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
              {{ getPaymentMethodText(order.paymentType) }}
            </span>
          </td>
          <td class="text-center">
            <span :class="getStatusBadgeClass(order.status)" style="font-size: 13px; padding: 5px 10px; border-radius: 20px">
              {{ getStatusText(order.status) }}
            </span>
          </td>
          <td class="text-center vertical-mid">
            <router-link :to="`/admin/orders/${order.id}`" class="btn btn-sm btn-warning me-2">
              Chi tiết
            </router-link>
            <button
                v-if="canCancelOrder(order.status)"
                class="btn btn-sm btn-danger"
                @click="deleteOrder(order.id)"
            >
              Hủy đơn
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="paginatedOrders.length" class="mt-4">
      <div class="pagination-wrapper">
        <!-- Left side: Page size selector -->
        <div class="page-size-selector">
          <span class="me-2 text-muted">Hiển thị</span>
          <select class="form-select form-select-sm me-2 custom-select" v-model="itemsPerPage">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">
              {{ size }}
            </option>
          </select>
          <span class="text-muted">đơn hàng</span>
        </div>

        <!-- Center: Pagination controls -->
        <div class="pagination-controls">
          <nav aria-label="Page navigation">
            <ul class="pagination pagination-sm mb-0 custom-pagination">
              <!-- Previous button -->
              <li class="page-item" :class="{ disabled: currentPage === 1 }">
                <button class="page-link custom-page-link" @click="goToPage(currentPage - 1)" :disabled="currentPage === 1">
                  «
                </button>
              </li>

              <!-- Page numbers -->
              <li v-for="page in visiblePages" :key="page" class="page-item" :class="{active: page === currentPage, disabled: page === '...'}">
                <button
                    v-if="page !== '...'"
                    class="page-link custom-page-link"
                    :class="{ 'active-page': page === currentPage }"
                    @click="goToPage(page)"
                >
                  {{ page }}
                </button>
                <span v-else class="page-link custom-page-link disabled">...</span>
              </li>

              <!-- Next button -->
              <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                <button class="page-link custom-page-link" @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages">
                  »
                </button>
              </li>
            </ul>
          </nav>
        </div>

        <!-- Right side: Pagination info -->
        <div class="pagination-info">
          <span class="text-muted">
            Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong tổng số {{ paginationInfo.total }} đơn hàng
          </span>
        </div>
      </div>
    </div>
  </div>

  <ShowToastComponent ref="toastRef"/>

  <!-- Cancel Order Confirmation Modal -->
  <div v-if="showCancelConfirmModal" class="modal-overlay" @click="hideCancelConfirmModal">
    <div class="modal-dialog modal-lg" @click.stop>
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <i class="fas fa-exclamation-triangle text-warning me-2"></i>
            Xác nhận hủy đơn hàng
          </h5>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <div class="alert alert-warning">
              <i class="fas fa-exclamation-triangle me-2"></i>
              <strong>Cảnh báo:</strong> Bạn đang thực hiện hủy đơn hàng #{{ cancelOrderCode }}.
              Hành động này không thể hoàn tác!
            </div>

            

            <div class="form-group">
              <label for="cancelDescription" class="form-label fw-bold">Lý do hủy đơn hàng:</label>
              <textarea
                  id="cancelDescription"
                  class="form-control"
                  rows="3"
                  v-model="cancelDescription"
                  placeholder="Nhập lý do hủy đơn hàng..."
              ></textarea>
              <small class="form-text text-muted">
                <i class="fas fa-info-circle me-1"></i>
                Vui lòng nhập lý do hủy đơn hàng để ghi nhận.
              </small>
            </div>
          </div>
        </div>
                 <div class="modal-footer">
           <button type="button" class="btn btn-warning" @click="hideCancelConfirmModal" :disabled="isCancellingOrder">
             <i class="fas fa-times me-1"></i>Không hủy
           </button>
           <button type="button" class="btn btn-danger" @click="confirmCancelOrder" :disabled="isCancellingOrder">
             <i class="fas fa-ban me-1"></i>
             {{ isCancellingOrder ? 'Đang hủy...' : 'Xác nhận hủy' }}
           </button>
         </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Main container */
.py-4 {
  min-height: 100vh;
}

/* Custom Tabs */
.custom-tabs {
  border-bottom: 2px solid #e9ecef;
  overflow-x: auto;
  flex-wrap: nowrap;
}

.custom-tabs .nav-item {
  white-space: nowrap;
}

.custom-tabs .nav-link {
  color: #1F2937;
  font-weight: 500;
  font-size: 0.875rem;
  padding: 0.75rem 1rem;
  border: none;
  border-bottom: 3px solid transparent;
  background: none;
  transition: all 0.3s ease;
}

.custom-tabs .nav-link:hover {
  border-bottom-color: #1F2937;
  color: #495057;
  background-color: rgba(13, 110, 253, 0.05);
}

.custom-tabs .nav-link.active {
  color: #1F2937;
  border-bottom-color: #1F2937;
  background: none;
  font-weight: 600;
}

/* Table styles to match product management */
.table-dark {
  background-color: #212529 !important;
  color: #fff;
}

.table-dark th {
  border-color: #32383e;
  font-weight: 600;
  font-size: 0.875rem;
}

.table-hover tbody tr:hover {
  background-color: rgba(0, 0, 0, 0.075);
}

.table-bordered {
  border: 1px solid #dee2e6;
}

.table-bordered th,
.table-bordered td {
  border: 1px solid #dee2e6;
}

/* Date validation styles */
.is-invalid {
  border-color: #dc3545 !important;
  box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25) !important;
}

.invalid-feedback {
  color: #dc3545;
  font-size: 0.875rem;
  margin-top: 4px;
  font-weight: 500;
}

/* Minimalist Badge Styles - Subtle and Professional */
.badge {
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.35rem 0.7rem;
  border-radius: 6px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  border: 1px solid;
  transition: all 0.2s ease;
}

/* Status Badge Colors - Toned Down */
.status-completed {
  background-color: #f0f9f0;
  color: #2d5a2d;
  border-color: #c8e6c9;
}

.status-delivered {
  background-color: #e8f4f8;
  color: #1e4a54;
  border-color: #b3d9e6;
}

.status-shipping {
  background-color: #fff8e1;
  color: #5a4a00;
  border-color: #ffe082;
}

.status-confirmed {
  background-color: #e3f2fd;
  color: #1a365d;
  border-color: #90caf9;
}

.status-processing {
  background-color: #f3e5f5;
  color: #4a148c;
  border-color: #ce93d8;
}

.status-pending {
  background-color: #f5f5f5;
  color: #424242;
  border-color: #bdbdbd;
}

.status-cancelled {
  background-color: #ffebee;
  color: #c62828;
  border-color: #ef9a9a;
}

.status-default {
  background-color: #f8f9fa;
  color: #6c757d;
  border-color: #dee2e6;
}

/* Payment Method Badge Colors - Toned Down */
.payment-cod {
  background-color: #fff3e0;
  color: #e65100;
  border-color: #ffcc02;
}

.payment-online {
  background-color: #e0f2f1;
  color: #00695c;
  border-color: #80cbc4;
}

.payment-default {
  background-color: #f8f9fa;
  color: #6c757d;
  border-color: #dee2e6;
}

/* Subtle hover effects */
.badge:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* Guest customer badge - Subtle styling */
.badge.bg-light {
  background-color: #fff8e1 !important;
  color: #e65100 !important;
  border: 1px solid #ffcc02 !important;
  font-weight: 500;
}

/* Date helper text styling */
.text-muted {
  font-size: 0.75rem;
  margin-top: 2px;
  display: block;
}

/* Pagination wrapper styles - matching product management */
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-top: 1px solid #e9ecef;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 12px;
  margin-top: 20px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* Page size selector styles */
.page-size-selector {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #6c757d;
}

.custom-select {
  width: 80px !important;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.custom-select:focus {
  border-color: #212529;
  box-shadow: 0 0 0 3px rgba(33, 37, 41, 0.1);
}

/* Pagination controls - center */
.pagination-controls {
  flex: 1;
  display: flex;
  justify-content: center;
  max-width: 600px;
}

.custom-pagination {
  gap: 4px;
}

.custom-page-link {
  border: 2px solid #e9ecef;
  color: #495057;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-weight: 500;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  min-width: 40px;
  text-align: center;
}

.custom-page-link:hover:not(:disabled):not(.disabled) {
  background: #212529;
  border-color: #212529;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(33, 37, 41, 0.3);
}

.custom-page-link:disabled {
  background: #f8f9fa;
  border-color: #e9ecef;
  color: #adb5bd;
  cursor: not-allowed;
}

.active-page {
  background: linear-gradient(135deg, #212529 0%, #000000 100%) !important;
  border-color: #212529 !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(33, 37, 41, 0.4) !important;
  transform: translateY(-1px);
}

.page-item.active .custom-page-link {
  background: linear-gradient(135deg, #212529 0%, #000000 100%) !important;
  border-color: #212529 !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(33, 37, 41, 0.4) !important;
}

/* Pagination info styles */
.pagination-info {
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}

/* Loading & Error States */
.spinner-border {
  width: 3rem;
  height: 3rem;
}

.alert {
  border: none;
  border-radius: 0.5rem;
  font-weight: 500;
}

.alert-danger {
  background-color: #f8d7da;
  color: #721c24;
}

/* Form controls */
.form-control, .form-select {
  border: 1px solid #ced4da;
  border-radius: 0.375rem;
}

.form-control:focus, .form-select:focus {
  border-color: #161D27;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

/* Responsive design */
@media (max-width: 768px) {
  .pagination-wrapper {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .pagination-controls {
    order: 1;
  }

  .page-size-selector {
    order: 2;
    justify-content: center;
  }

  .pagination-info {
    order: 3;
    text-align: center;
  }

  .custom-tabs {
    overflow-x: auto;
    white-space: nowrap;
  }

  .custom-tabs .nav-link {
    padding: 0.5rem 1rem;
    font-size: 0.8rem;
  }

  .table-responsive {
    border: none;
  }

  .table thead th,
  .table tbody td {
    padding: 0.5rem;
    font-size: 0.8rem;
  }
}

@media (max-width: 576px) {
  .payment-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .form-check-inline {
    width: 100%;
    justify-content: flex-start;
  }

  .pagination-sm {
    font-size: 0.8rem;
  }

  .pagination-sm .page-link {
    padding: 0.25rem 0.5rem;
  }
}

/* Error tooltip styles */
.error-tooltip {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
  z-index: 1000;
  margin-top: 4px;
  box-shadow: 0 4px 12px rgba(220, 53, 69, 0.3);
  animation: slideDown 0.3s ease;
}

.error-tooltip i {
  margin-right: 6px;
  font-size: 0.7rem;
}

.error-tooltip::before {
  content: '';
  position: absolute;
  top: -4px;
  left: 16px;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-bottom: 4px solid #dc3545;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  animation: fadeIn 0.3s ease;
}

.modal-dialog {
  max-width: 600px;
  width: 90%;
  margin: 0 auto;
}

.modal-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  animation: slideInUp 0.3s ease;
}

.modal-header {
  border-bottom: 1px solid #dee2e6;
  padding: 1rem 1.5rem;
  border-radius: 12px 12px 0 0;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  border-top: 1px solid #dee2e6;
  padding: 1rem 1.5rem;
  border-radius: 0 0 12px 12px;
  background: #f8f9fa;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Form group styles */
.form-group {
  margin-bottom: 1rem;
}

.form-label {
  font-weight: 600;
  color: #495057;
  margin-bottom: 0.5rem;
}

.form-control {
  border: 2px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.form-control:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

/* Alert styles */
.alert-warning {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
  border: 1px solid #ffeaa7;
  color: #856404;
  border-radius: 8px;
}

/* Coupon info styles */
.coupon-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.coupon-info .badge {
  font-weight: 600;
  letter-spacing: 0.5px;
}

.coupon-info .small {
  font-size: 0.7rem;
  font-weight: 500;
}

/* Button styles */
.btn {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.btn-danger {
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
  border: none;
}

.btn-danger:hover:not(:disabled) {
  background: linear-gradient(135deg, #c82333 0%, #bd2130 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(220, 53, 69, 0.3);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}
</style>