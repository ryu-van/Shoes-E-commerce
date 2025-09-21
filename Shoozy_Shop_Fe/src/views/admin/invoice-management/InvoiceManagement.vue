<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">🧾 Quản lý hóa đơn</h2>

    <!-- Bộ lọc -->
    <div class="row gy-2 gx-3 align-items-center mb-3 flex-wrap">
      <div class="col-md-3">
        <input 
          type="text" 
          class="form-control" 
          placeholder="🔍 Tìm theo mã hóa đơn, email..." 
          v-model="search"
        >
      </div>
      <div class="col-md-2">
        <input 
          type="date" 
          class="form-control" 
          v-model="filterDateFrom"
          title="Từ ngày"
        >
      </div>
      <div class="col-md-2">
        <input 
          type="date" 
          class="form-control" 
          v-model="filterDateTo"
          title="Đến ngày"
        >
      </div>
      <div class="col-md-2">
        <select class="form-select" v-model="filterStatus">
          <option value="">Tất cả trạng thái</option>
          <option value="COMPLETED">Hoàn thành</option>
          <option value="CANCELLED">Đã hủy</option>
        </select>
      </div>
      <div class="col-md-3 col-12 d-flex gap-3 flex-wrap">
        <button class="btn btn-secondary flex-fill" style="height: 38px" @click="resetFilter">
          Clear
        </button>
        <div class="dropdown position-relative flex-fill">
          <button 
            class="btn btn-primary w-100 d-flex align-items-center justify-content-center" 
            style="height: 38px"
            @click="toggleSettingsMenu"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="me-2" viewBox="0 0 24 24">
              <path d="M19.43 12.98c.04-.32.07-.66.07-1s-.03-.68-.07-1l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46a.5.5 0 0 0-.6-.22l-2.49 1a7.03 7.03 0 0 0-1.7-.98L14.5 2.5a.5.5 0 0 0-.5-.5h-4a.5.5 0 0 0-.5.5l-.38 2.53a7.03 7.03 0 0 0-1.7.98l-2.49-1a.5.5 0 0 0-.6.22l-2 3.46a.5.5 0 0 0 .12.64L4.57 11c-.04.32-.07.66-.07 1s.03.68.07 1L2.46 14.35a.5.5 0 0 0-.12.64l2 3.46c.14.24.44.34.7.22l2.49-1c.52.39 1.09.72 1.7.98l.38 2.53a.5.5 0 0 0 .5.5h4c.25 0 .46-.18.5-.42l.38-2.53a7.03 7.03 0 0 0 1.7-.98l2.49 1c.26.1.56.02.7-.22l2-3.46a.5.5 0 0 0-.12-.64l-2.11-1.65zM12 15.5A3.5 3.5 0 1 1 15.5 12 3.5 3.5 0 0 1 12 15.5z" />
            </svg>
            Cài đặt
          </button>
          <ul class="dropdown-menu" :class="{ show: showSettingsMenu }">
            <li>
              <a class="dropdown-item" href="#" @click="exportInvoices">
                <i class="bi bi-file-earmark-excel"></i> Xuất file
              </a>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- Thống kê nhanh -->
    <div class="row mb-4">
      <div class="col-md-3">
        <div class="card stats-card">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h6>Tổng hóa đơn</h6>
                <h4>{{ filteredInvoices.length }}</h4>
              </div>
              <div class="text-primary">
                <i class="fas fa-receipt fa-2x"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card stats-card">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h6>Đã hoàn thành</h6>
                <h4>{{ deliveredInvoices }}</h4>
              </div>
              <div class="text-success">
                <i class="fas fa-check-circle fa-2x"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card stats-card">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h6>Đã hủy</h6>
                <h4>{{ cancelledInvoices }}</h4>
              </div>
              <div class="text-danger">
                <i class="fas fa-times-circle fa-2x"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card stats-card">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h6>Tổng doanh thu</h6>
                <h4>{{ formatCurrency(totalRevenue) }}</h4>
              </div>
              <div class="text-warning">
                <i class="fas fa-dollar-sign fa-2x"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Đang tải -->
    <div v-if="loading" class="text-center my-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Đang tải...</span>
      </div>
    </div>

    <!-- không có dữ liệu -->
    <div v-else-if="!filteredInvoices.length" class="text-center my-5">
      <div class="alert alert-info">
        <i class="fas fa-info-circle me-2"></i>
        Không có hóa đơn nào để hiển thị.
      </div>
    </div>

    <!-- Bảng dữ liệu -->
    <div v-else style="margin-top: 32px" class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
          <tr>
            <th class="text-center" style="width: 5%">STT</th>
            <th class="text-center">OrderCode</th>
            <th class="text-center">Ngày Đặt</th>
            <th class="text-center">Khách Hàng</th>
            <th class="text-center">Email</th>
            <th class="text-center">Sản Phẩm</th>
            <th class="text-center">Tổng Tiền</th>
            <th class="text-center">Trạng Thái</th>
            <th class="text-center" style="width: 20%">Hành Động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(invoice, index) in paginatedInvoices" :key="invoice.orderId">
            <td class="text-center">{{ index + 1 + (currentPage - 1) * itemsPerPage }}</td>
            <td class="text-center">
              <span class="invoice-code">{{ invoice.orderCode }}</span>
            </td>
            <td class="text-center">{{ formatDate(invoice.orderDate) }}</td>
            <td>{{ invoice.customerName || 'N/A' }}</td>
            <td class="email-column">{{ invoice.email || 'N/A' }}</td>
            <td>{{ formatItems(invoice.items) }}</td>
            <td class="text-center">{{ formatCurrency(invoice.totalMoney) }}</td>
            <td class="text-center">
              <span 
                :class="invoice.status === 'COMPLETED' ? 'badge bg-success' : 'badge bg-danger'"
                style="font-size: 13px; padding: 5px 10px; border-radius: 20px"
              >
                {{ getStatusText(invoice.status) }}
              </span>
            </td>
            <td class="text-center">
              <div role="group">
                <button class="btn btn-sm btn-warning me-1" @click="viewInvoice(invoice)" >
                  <i class="bi bi-eye"></i>Chi Tiết
                </button>

                <button class="btn btn-sm btn-success" @click="showEmailModal(invoice.orderId)" >
                  <i class="bi bi-envelope"></i>Gửi Email
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Phân trang -->
    <div v-if="filteredInvoices.length" class="mt-4">
      <div class="pagination-wrapper">
        <!-- Left side: Page size selector -->
        <div class="page-size-selector">
          <span class="me-2 text-muted">Hiển thị</span>
          <select class="form-select form-select-sm me-2 custom-select" v-model="itemsPerPage">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">
              {{ size }}
            </option>
          </select>
          <span class="text-muted">hóa đơn</span>
        </div>

        <!-- Center: Pagination controls -->
        <div class="pagination-controls">
          <nav aria-label="Page navigation">
            <ul class="pagination pagination-sm mb-0 custom-pagination">
              <!-- Previous button -->
              <li class="page-item" :class="{ disabled: currentPage === 1 }">
                <button class="page-link custom-page-link" @click="changePage(currentPage - 1)" :disabled="currentPage === 1">
                  «
                </button>
              </li>

              <!-- Page numbers -->
              <li v-for="page in visiblePages" :key="page" class="page-item" :class="{active: page === currentPage, disabled: page === '...'}">
                <button
                    v-if="page !== '...'"
                    class="page-link custom-page-link"
                    :class="{ 'active-page': page === currentPage }"
                    @click="changePage(page)"
                >
                  {{ page }}
                </button>
                <span v-else class="page-link custom-page-link disabled">...</span>
              </li>

              <!-- Next button -->
              <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                <button class="page-link custom-page-link" @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages">
                  »
                </button>
              </li>
            </ul>
          </nav>
        </div>

        <!-- Right side: Pagination info -->
        <div class="pagination-info">
          <span class="text-muted">
            Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong tổng số {{ paginationInfo.total }} hóa đơn
          </span>
        </div>
      </div>
    </div>

    <!-- Modal chi tiết hóa đơn -->
    <div v-if="selectedInvoice && !isEditing" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Chi tiết hóa đơn {{ selectedInvoice.orderCode }}</h5>
            <button type="button" class="btn-close" @click="selectedInvoice = null"></button>
          </div>
          <div class="modal-body">
            <div class="row">
              <div class="col-md-6">
                <p><strong>Ngày đặt:</strong> {{ formatDate(selectedInvoice.orderDate) }}</p>
                <p><strong>Khách hàng:</strong> {{ selectedInvoice.customerName || 'N/A' }}</p>
                <p><strong>Số ĐT:</strong> {{ selectedInvoice.phoneNumber || 'N/A' }}</p>
                <p><strong>Email:</strong> {{ selectedInvoice.email || 'N/A' }}</p>
                <p><strong>Địa chỉ giao:</strong> {{ selectedInvoice.shippingAddress || 'N/A' }}</p>
                <p><strong>Ghi chú:</strong> {{ selectedInvoice.note || 'N/A' }}</p>
              </div>
              <div class="col-md-6">
                <p><strong>Mã giảm giá:</strong> {{ selectedInvoice.couponName || 'N/A' }}</p>
                <p><strong>Giá trị giảm:</strong> {{ formatCouponValue(selectedInvoice.couponValue) }}</p>
                <p><strong>Tổng tiền SP:</strong> {{ formatCurrency(selectedInvoice.totalItems) }}</p>
                <p><strong>Tổng thanh toán:</strong> {{ formatCurrency(selectedInvoice.totalMoney) }}</p>
                <p><strong>PT thanh toán:</strong> {{ selectedInvoice.paymentMethod || 'N/A' }}</p>
                <p><strong>Trạng thái:</strong> {{ getStatusText(selectedInvoice.status) }}</p>
              </div>
            </div>
            <h6 class="mt-3">Danh sách sản phẩm:</h6>
            <div class="table-responsive">
              <table class="table table-bordered table-sm">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Size</th>
                    <th>Màu</th>
                    <th>SL</th>
                    <th>Giá</th>
                    <th>Tổng</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in selectedInvoice.items || []" :key="item.productName">
                    <td>{{ item.productName || 'N/A' }}</td>
                    <td>{{ item.size || 'N/A' }}</td>
                    <td>{{ item.color || 'N/A' }}</td>
                    <td>{{ item.quantity || 'N/A' }}</td>
                    <td>{{ formatCurrency(item.price) }}</td>
                    <td>{{ formatCurrency(item.totalMoney) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="selectedInvoice = null">Đóng</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal nhập email -->
    <div v-if="showEmailModalFlag" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Gửi hóa đơn qua Gmail</h5>
            <button type="button" class="btn-close" @click="closeEmailModal"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label fw-semibold">Email người nhận</label>
              <input 
                v-model="recipientEmail" 
                type="email" 
                class="form-control" 
                placeholder="Nhập email (VD: example@gmail.com)" 
                :class="{ 'is-invalid': emailError }"
              >
              <div v-if="emailError" class="invalid-feedback">{{ emailError }}</div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeEmailModal">Hủy</button>
            <button type="button" class="btn btn-primary" @click="sendInvoiceEmail" :disabled="sendingEmail">
              <span v-if="sendingEmail" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
              {{ sendingEmail ? 'Đang gửi...' : 'Gửi' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Template ẩn để in hóa đơn -->
    <div v-if="invoicesToPrint.length" id="print-invoices" class="print-area d-none">
      <div v-for="invoice in invoicesToPrint" :key="invoice.orderId" class="invoice-print page-break">
        <div class="invoice-header text-center mb-4">
          <h3>Shoozy Shop</h3>
          <p>{{ invoice.shopAddress || '123 Đường ABC, TP. HCM' }}</p>
          <p>SĐT: {{ invoice.shopPhone || '0123 456 789' }} | Mã số thuế: 123456789</p>
          <h4>HÓA ĐƠN BÁN HÀNG</h4>
          <p>Mã hóa đơn: {{ invoice.orderCode }}</p>
        </div>
        <div class="invoice-info row mb-4">
          <div class="col-6">
            <p><strong>Khách hàng:</strong> {{ invoice.customerName || 'N/A' }}</p>
            <p><strong>Số ĐT:</strong> {{ invoice.phoneNumber || 'N/A' }}</p>
            <p><strong>Email:</strong> {{ invoice.email || 'N/A' }}</p>
            <p><strong>Địa chỉ giao:</strong> {{ invoice.shippingAddress || 'N/A' }}</p>
          </div>
          <div class="col-6 text-end">
            <p><strong>Ngày đặt:</strong> {{ formatDate(invoice.orderDate) }}</p>
            <p><strong>Ngày giao:</strong> {{ formatDate(invoice.shippingDate) }}</p>
            <p><strong>Trạng thái:</strong> {{ getStatusText(invoice.status) }}</p>
          </div>
        </div>
        <table class="table table-bordered">
          <thead>
            <tr>
              <th>STT</th>
              <th>Sản phẩm</th>
              <th>Kích cỡ</th>
              <th>Màu sắc</th>
              <th>Chất liệu</th>
              <th>Số lượng</th>
              <th>Đơn giá</th>
              <th>Tổng</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in invoice.items" :key="item.productName">
              <td>{{ idx + 1 }}</td>
              <td>{{ item.productName || 'N/A' }}</td>
              <td>{{ item.size || 'N/A' }}</td>
              <td>{{ item.color || 'N/A' }}</td>
              <td>{{ item.material || 'N/A' }}</td>
              <td>{{ item.quantity || 'N/A' }}</td>
              <td>{{ formatCurrency(item.price) }}</td>
              <td>{{ formatCurrency(item.totalMoney) }}</td>
            </tr>
          </tbody>
        </table>
        <div class="invoice-footer row mt-4">
          <div class="col-6">
            <p><strong>Ghi chú:</strong> {{ invoice.note || 'N/A' }}</p>
            <p><strong>Phương thức thanh toán:</strong> {{ invoice.paymentMethod || 'N/A' }}</p>
            <p><strong>Phương thức giao:</strong> {{ invoice.shippingMethod || 'N/A' }}</p>
          </div>
          <div class="col-6 text-end">
            <p><strong>Tổng tiền sản phẩm:</strong> {{ formatCurrency(invoice.totalItems) }}</p>
            <p><strong>Giảm giá:</strong> {{ formatCouponValue(invoice.couponValue) }}</p>
            <p><strong>Tổng thanh toán:</strong> {{ formatCurrency(invoice.totalMoney) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Thông báo -->
    <div class="position-fixed top-0 end-0 p-3 mt-5" style="z-index: 9999; right: 0;">
      <div
          class="toast align-items-center border-0"
          :class="{
            'bg-success': toastType === 'success',
            'bg-danger': toastType === 'error',
            'bg-info': toastType === 'info',
            'bg-warning': toastType === 'warning',
          }"
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
          ref="toastRef"
      >
        <div class="d-flex">
          <div class="toast-body text-white">{{ toastMessage }}</div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" @click="hideToast"></button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getInvoices, sendInvoiceEmail, exportInvoicesToExcel } from '@/service/InvoiceApi';

export default {
  name: 'InvoiceManagement',
  data() {
    return {
      selectedInvoice: null,
      isEditing: false,
      showSettingsMenu: false,
      invoices: [],
      invoicesToPrint: [],
      loading: false,
      currentPage: 1,
      itemsPerPage: 5,
      pageSizeOptions: [5, 10, 15, 20, 25],
      search: '',
      filterDateFrom: '',
      filterDateTo: '',
      filterStatus: '',
      showEmailModalFlag: false,
      recipientEmail: '',
      emailError: '',
      sendingEmail: false,
      emailOrderId: null,
      toastMessage: '',
      toastType: 'success',
    };
  },

  computed: {
    filteredInvoices() {
      debugger
      let filtered = this.invoices;
      if (this.filterStatus) {
        filtered = filtered.filter(invoice => invoice.status === this.filterStatus);
      } else {
        filtered = filtered.filter(invoice => invoice.status === 'COMPLETED' || invoice.status === 'CANCELLED');
      }
      if (this.search) {
        // Hàm chuẩn hóa: bỏ khoảng trắng và chuyển về chữ thường
        const normalize = str => str ? str.toString().toLowerCase().replace(/\s+/g, '') : '';
        const searchLower = normalize(this.search);
        filtered = filtered.filter(invoice => {
          const orderCode = invoice.orderCode ? invoice.orderCode : this.generateOrderCode(invoice.orderId);
          const fields = [
            invoice.customerName,
            invoice.phoneNumber,
            invoice.email,
            invoice.shippingAddress,
            invoice.note,
            invoice.couponName,
            invoice.shopName,
            invoice.shopAddress,
            invoice.shopPhone,
            invoice.paymentMethod,
            invoice.shippingMethod,
            invoice.status,
            ...(invoice.items || []).map(item => item.productName),
            orderCode,
            invoice.orderId.toString(),
          ].filter(field => field !== undefined && field !== null);
          // So sánh sau khi loại bỏ khoảng trắng
          return fields.some(field => normalize(field).includes(searchLower));
        });
      }
      if (this.filterDateFrom) {
        filtered = filtered.filter(invoice => new Date(invoice.orderDate) >= new Date(this.filterDateFrom));
      }
      if (this.filterDateTo) {
        filtered = filtered.filter(invoice => new Date(invoice.orderDate) <= new Date(this.filterDateTo));
      }
      // Sắp xếp hóa đơn mới nhất lên đầu
      return filtered.slice().sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate));
    },

    paginatedInvoices() {
      const start = (this.currentPage - 1) * this.itemsPerPage;
      const end = start + this.itemsPerPage;
      return this.filteredInvoices.slice(start, end);
    },

    totalPages() {
      return Math.ceil(this.filteredInvoices.length / this.itemsPerPage) || 1;
    },

    paginationInfo() {
      const start = (this.currentPage - 1) * this.itemsPerPage + 1;
      const end = Math.min(this.currentPage * this.itemsPerPage, this.filteredInvoices.length);
      return {
        start,
        end,
        total: this.filteredInvoices.length
      };
    },

    visiblePages() {
      const current = this.currentPage;
      const total = this.totalPages;
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
    },

    deliveredInvoices() {
      return this.filteredInvoices.filter(invoice => invoice.status === 'COMPLETED').length;
    },

    cancelledInvoices() {
      return this.filteredInvoices.filter(invoice => invoice.status === 'CANCELLED').length;
    },

    totalRevenue() {
      return this.filteredInvoices
        .filter(invoice => invoice.status === 'COMPLETED')
        .reduce((sum, invoice) => sum + (invoice.totalMoney || 0), 0);
    },
  },

  watch: {
    itemsPerPage() {
      this.currentPage = 1;
    },
  },

  methods: {
    async fetchInvoices() {
      this.loading = true;
      try {
        const response = await getInvoices();
        const orders = Array.isArray(response.data) ? response.data : [];
        if (!orders.length) {
          this.invoices = [];
          this.showAlert('Không có hóa đơn nào!', 'info');
          return;
        }
        this.invoices = orders.map(order => ({
          orderId: order.orderId,
          orderCode: order.orderCode || this.generateOrderCode(order.orderId),
          orderDate: order.orderDate,
          status: order.status,
          customerName: order.customerName,
          phoneNumber: order.phoneNumber,
          email: order.email,
          shippingAddress: order.shippingAddress,
          note: order.note,
          items: order.items,
          couponName: order.couponName,
          couponValue: order.couponValue,
          totalItems: order.totalItems,
          totalMoney: order.totalMoney,
          paymentMethod: order.paymentMethod,
          shippingMethod: order.shippingMethod,
          shippingDate: order.shippingDate,
          shopName: order.shopName,
          shopAddress: order.shopAddress,
          shopPhone: order.shopPhone,
          userId: order.userId,
          paymentMethodId: order.paymentMethodId,
          couponId: order.couponId,
        }));
        this.currentPage = 1;
      } catch (error) {
        console.error('Lỗi khi lấy danh sách hóa đơn:', error.message, error.response);
        this.invoices = [];
        this.showAlert('Lỗi khi lấy danh sách hóa đơn: ' + error.message, 'error');
      } finally {
        this.loading = false;
      }
    },

    generateOrderCode(id) {
      return `HD${String(id).padStart(3, '0')}`;
    },

    formatDate(dateString) {
      if (!dateString) return 'N/A';
      try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return 'N/A';
        return date.toLocaleDateString('vi-VN');
      } catch (error) {
        return 'N/A';
      }
    },

    formatCurrency(amount) {
      if (!amount && amount !== 0) return '0 ₫';
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    },

    formatCouponValue(value) {
      if (value === null || value === undefined) return 'N/A';
      if (typeof value === 'string' && value.trim() === '') return 'N/A';
      if (value < 1000) return `${value}%`;
      return this.formatCurrency(value);
    },

    formatItems(items) {
      if (!items || !Array.isArray(items) || !items.length) return 'N/A';
      if (items.length === 1) return `${items[0].productName} (${items[0].quantity})`;
      return `${items[0].productName} (${items[0].quantity}), ...`;
    },

    getStatusText(status) {
      if (status === 'COMPLETED') return 'Hoàn thành';
      if (status === 'CANCELLED') return 'Đã hủy';
      return status;
    },

    viewInvoice(invoice) {
      this.selectedInvoice = { ...invoice };
      this.isEditing = false;
    },


    printInvoices() {
      if (!this.filteredInvoices.length) {
        this.showAlert('Không có hóa đơn nào để in!', 'warning');
        return;
      }
      this.invoicesToPrint = [...this.filteredInvoices];
      this.$nextTick(() => {
        window.print();
        setTimeout(() => { this.invoicesToPrint = []; }, 100);
      });
    },

    exportInvoices: async function() {
      try {
        this.showAlert('Đang xuất file hóa đơn...', 'success');
        const response = await exportInvoicesToExcel();
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'danh_sach_hoa_don.xlsx');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        this.showAlert('Xuất file thành công!', 'success');
      } catch (error) {
        this.showAlert('Lỗi khi xuất file: ' + (error.message || 'Không xác định'), 'error');
      }
    },

    showEmailModal(orderId) {
      this.showEmailModalFlag = true;
      this.emailOrderId = orderId;
      const invoice = this.invoices.find(inv => inv.orderId === orderId);
      this.recipientEmail = invoice?.email || '';
      if (!invoice?.email) {
        this.emailError = 'Khách hàng chưa cung cấp email. Vui lòng nhập thủ công.';
      } else {
        this.emailError = '';
      }
      this.showSettingsMenu = false;
    },

    closeEmailModal() {
      this.showEmailModalFlag = false;
      this.recipientEmail = '';
      this.emailError = '';
      this.emailOrderId = null;
    },

    async sendInvoiceEmail() {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!this.recipientEmail || !emailRegex.test(this.recipientEmail)) {
        this.emailError = 'Vui lòng nhập email hợp lệ!';
        return;
      }
      this.sendingEmail = true;
      try {
        if (!this.emailOrderId) throw new Error('Không có hóa đơn nào được chọn');
        const response = await sendInvoiceEmail(this.emailOrderId, this.recipientEmail);
        this.showAlert(`Gửi email thành công: ${response}`, 'success');
        this.closeEmailModal();
      } catch (error) {
        const errorMessage = error.response?.data || error.message;
        this.showAlert(`Lỗi khi gửi email: ${errorMessage}`, 'error');
      } finally {
        this.sendingEmail = false;
      }
    },

    resetFilter() {
      this.search = '';
      this.filterDateFrom = '';
      this.filterDateTo = '';
      this.filterStatus = '';
      this.currentPage = 1;
    },

    showAlert(message, type = 'success') {
      this.toastMessage = message;
      this.toastType = type;
      this.$nextTick(() => {
        const toast = window.bootstrap ? new window.bootstrap.Toast(this.$refs.toastRef) : null;
        if (toast) toast.show();
      });
    },

    hideToast() {
      const toast = window.bootstrap ? new window.bootstrap.Toast(this.$refs.toastRef) : null;
      if (toast) toast.hide();
    },

    toggleSettingsMenu() {
      this.showSettingsMenu = !this.showSettingsMenu;
    },

    changePage(page) {
      if (page >= 1 && page <= this.totalPages && page !== '...') {
        this.currentPage = page;
      }
    },
  },

  mounted() {
    this.fetchInvoices();
  },
};
</script>

<style scoped>
/* Pagination wrapper styles */
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
  animation: fadeIn 0.3s ease-out;
}

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

/* Table styles */
.table-responsive {
  margin-top: 32px;
}

.table {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.3s ease-out;
}

.table th {
  background-color: #212529 !important;
  color: #ffffff !important;
  font-weight: 600;
  border: none;
  padding: 12px 8px;
  text-align: center;
  vertical-align: middle;
}

.table td {
  padding: 12px 8px;
  vertical-align: middle;
  border-top: 1px solid #dee2e6;
  text-align: center;
}

.table tbody tr:hover {
  background-color: #f8f9fa;
}

.table th.email-column,
.table td.email-column {
  min-width: 150px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Invoice code styling */
.invoice-code {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-weight: 600;
  color: #0d6efd;
  background-color: #f8f9fa;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

/* Modal styles */
.modal-dialog {
  max-width: 1000px;
  margin: 1.75rem auto;
}

.modal-body .table {
  width: 100%;
  table-layout: auto;
}

.modal-body .table th,
.modal-body .table td {
  min-width: 100px;
  text-align: left;
  vertical-align: middle;
  white-space: normal;
}

.modal-body .table td.text-end {
  min-width: 120px;
  text-align: right;
  white-space: nowrap;
}

.modal-body {
  overflow-x: auto;
}

/* Pagination */
.pagination {
  margin-top: 20px;
}

.page-item .page-link {
  color: #0d6efd;
  cursor: pointer;
}

.page-item.active .page-link {
  background-color: #0d6efd;
  border-color: #0d6efd;
  color: white;
}

.page-item.disabled .page-link {
  cursor: not-allowed;
  opacity: 0.5;
}

/* Stats card styles */
.stats-card {
  border-left: 4px solid #007bff;
  transition: transform 0.2s;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 1px solid #e9ecef;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stats-card .card-body {
  padding: 1.25rem;
}

/* Button styles */
.btn-group .btn {
  margin-right: 2px;
  border-radius: 6px;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 4px;
}

.btn-info {
  background-color: #0dcaf0;
  border-color: #0dcaf0;
  color: #000;
}


/* Badge styles */
.badge {
  font-size: 13px;
  padding: 5px 10px;
  border-radius: 20px;
}

.bg-success {
  background-color: #198754 !important;
}

.bg-danger {
  background-color: #dc3545 !important;
}

/* Form controls */
.row.gy-2.gx-3 {
  margin-bottom: 1rem;
}

.form-control, .form-select {
  border: 1px solid #ced4da;
  border-radius: 6px;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}

.form-control:focus, .form-select:focus {
  border-color: #86b7fe;
  outline: 0;
  box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
}

/* Animation for smooth transitions */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Print styles */
@media print {
  body * {
    visibility: hidden;
  }
  #print-invoices,
  #print-invoices * {
    visibility: visible;
  }
  #print-invoices {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    display: block !important;
  }
  .invoice-print {
    padding: 20px;
    font-size: 12pt;
    color: #000;
  }
  .invoice-header h3 {
    font-size: 18pt;
    margin-bottom: 10px;
  }
  .invoice-header h4 {
    font-size: 14pt;
    margin: 10px 0;
  }
  .invoice-info p {
    margin: 5px 0;
    font-size: 10pt;
  }
  .table th,
  .table td {
    border: 1px solid #000;
    padding: 8px;
    font-size: 10pt;
  }
  .invoice-footer p {
    margin: 5px 0;
    font-size: 10pt;
  }
  .page-break {
    page-break-after: always;
  }
  .page-break:last-child {
    page-break-after: auto;
  }
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

  .modal-dialog {
    max-width: 95%;
  }
  
  .modal-body .table th,
  .modal-body .table td {
    min-width: 80px;
    font-size: 14px;
  }
  
  .pagination {
    font-size: 14px;
  }
  
  .page-link {
    padding: 5px 10px;
  }

  .table th,
  .table td {
    font-size: 12px;
    padding: 8px 4px;
  }

  .btn-group .btn {
    margin-bottom: 4px;
  }
}
</style>