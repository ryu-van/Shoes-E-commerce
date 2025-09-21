<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getReturnDetailByAdmin, updateReturnStatus } from '@/service/ReturnApis';
import ShowToastComponent from '@/components/ShowToastComponent.vue';
import { Modal } from 'bootstrap';
import { computed } from "vue";


const displayReason = computed(() => {
  if (!returnRequest.value) return "";
  // Nếu có đúng 1 sản phẩm thì lấy note riêng của sản phẩm đó
  if (returnRequest.value.returnItems?.length === 1) {
    return returnRequest.value.returnItems[0].note || returnRequest.value.reason || "Không có";
  }
  // Nếu nhiều sản phẩm thì giữ nguyên lý do chung
  return returnRequest.value.reason || "Không có";
});


const confirmModalEl = ref(null);
const confirmMessage = ref('');
let confirmResolver = null;

const showConfirm = (message) => {
  confirmMessage.value = message;
  return new Promise((resolve) => {
    confirmResolver = (result) => {
      resolve(result);
      confirmResolver = null;
      const modal = Modal.getInstance(confirmModalEl.value);
      modal.hide();
    };
    const modal = new Modal(confirmModalEl.value);
    modal.show();
  });
};


const route = useRoute();
const router = useRouter();
const returnRequest = ref(null);
const loading = ref(true);
const toastRef = ref(null);

const goBack = () => router.back();

const showToast = (msg, type) => {
  toastRef.value?.showToast(msg, type);
};

const statusTextMap = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
  WAIT_FOR_PICKUP: "Chờ lấy hàng",
  RETURNED: "Đã nhận hàng",
  REFUNDED: "Đã hoàn tiền",
  CANCELLED: "Đã huỷ",
  COMPLETED: "Hoàn tất"
};

function getStatusText(status) {
  return statusTextMap[status] || status;
}

const fetchReturnDetail = async () => {
  try {
    const res = await getReturnDetailByAdmin(route.params.id);
    returnRequest.value = res.data.data;
  } catch (err) {
    console.error("Lỗi reload chi tiết:", err);
  }
};

const handleStatusUpdate = async (newStatus) => {
const confirmed = await showConfirm(`Bạn có chắc muốn chuyển sang trạng thái "${getStatusText(newStatus)}" không?`);

  if (confirmed) {
    try {
      await updateReturnStatus(returnRequest.value.id, newStatus);
      showToast("✅ Cập nhật trạng thái thành công!", "success");
      await fetchReturnDetail();
    } catch (err) {
      console.error(err);
      showToast("❌ Cập nhật trạng thái thất bại", "error");
    }
  }
};



onMounted(async () => {
  try {
    const res = await getReturnDetailByAdmin(route.params.id);
    returnRequest.value = res.data.data;
  } catch (err) {
    console.error("Lỗi khi lấy chi tiết yêu cầu trả hàng:", err);
  } finally {
    loading.value = false;
  }
});

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleString('vi-VN');
}

function getStatusClass(status) {
  switch (status) {
    case 'PENDING': return 'badge bg-warning text-dark';
    case 'APPROVED': return 'badge bg-success';
    case 'REJECTED': return 'badge bg-danger';
    case 'WAIT_FOR_PICKUP': return 'badge bg-info text-dark';
    case 'RETURNED': return 'badge bg-primary';
    case 'REFUNDED': return 'badge bg-secondary';
    case 'CANCELLED': return 'badge bg-dark';
    case 'COMPLETED': return 'badge bg-success';
    default: return 'badge bg-secondary';
  }
}
</script>

<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h3>Chi tiết yêu cầu trả hàng #{{ returnRequest?.id }}</h3>
      <button class="btn btn-secondary" @click="goBack">← Quay lại</button>
    </div>

    <div v-if="loading">Đang tải dữ liệu...</div>

    <div v-else-if="returnRequest">
      <div class="card mb-3 shadow-sm">
        <div class="card-body">
          <h5 class="card-title">Thông tin chung</h5>
          <p><strong>Trạng thái:</strong>
            <span :class="getStatusClass(returnRequest.status)">
              {{ getStatusText(returnRequest.status) }}
            </span>
          </p>
          <p><strong>Lý do:</strong> {{ displayReason }}</p>
          <p><strong>Ghi chú:</strong> {{ returnRequest.note || 'Không có' }}</p>
          <p><strong>Ngày tạo:</strong> {{ formatDate(returnRequest.createdAt) }}</p>
          <p><strong>Ngày cập nhật:</strong> {{ formatDate(returnRequest.updatedAt) }}</p>
        </div>
      </div>

    
     <!-- THAO TÁC TRẠNG THÁI -->
<div class="card mb-3 shadow-sm" v-if="!['COMPLETED','REFUNDED','REJECTED'].includes(returnRequest.status)">
  <div class="card-body">
    <h5 class="card-title mb-3">Thao tác xử lý</h5>
    <div class="d-flex flex-wrap gap-2">
      <template v-if="returnRequest.status === 'PENDING'">
        <button class="btn btn-success" @click="handleStatusUpdate('APPROVED')">✅ Duyệt</button>
        <button class="btn btn-danger" @click="handleStatusUpdate('REJECTED')">❌ Từ chối</button>
      </template>

      <template v-else-if="returnRequest.status === 'APPROVED'">
        <button class="btn btn-primary" @click="handleStatusUpdate('WAIT_FOR_PICKUP')">📦 Tạo lệnh lấy hàng</button>
        <button class="btn btn-dark" @click="handleStatusUpdate('CANCELLED')">❌ Huỷ yêu cầu</button>
      </template>

      <template v-else-if="returnRequest.status === 'WAIT_FOR_PICKUP'">
        <button class="btn btn-info" @click="handleStatusUpdate('RETURNED')">📥 Đã nhận hàng</button>
      </template>

      <template v-else-if="returnRequest.status === 'RETURNED'">
        <button class="btn btn-success" @click="handleStatusUpdate('COMPLETED')">✅ Hoàn tất đổi hàng</button>
        <button class="btn btn-warning" @click="handleStatusUpdate('REFUNDED')">💰 Hoàn tiền</button>
      </template>

      <template v-else>
        <span class="text-muted">Không còn thao tác nào có thể thực hiện.</span>
      </template>
    </div>
  </div>
</div>


      <div class="card mb-3 shadow-sm">
        <div class="card-body">
          <h5 class="card-title">Thông tin đơn hàng</h5>
          <p><strong>Mã đơn hàng:</strong> {{ returnRequest.order.orderCode }}</p>
          <p><strong>Khách hàng:</strong> {{ returnRequest.order.fullname }}</p>
          <p><strong>SĐT:</strong> {{ returnRequest.order.phoneNumber }}</p>
          <p><strong>Địa chỉ giao hàng:</strong> {{ returnRequest.order.shippingAddress }}</p>
          <p><strong>Tổng tiền:</strong> {{ returnRequest.order.totalMoney.toLocaleString('vi-VN') }} ₫</p>
        </div>
      </div>

      <div class="card shadow-sm">
        <div class="card-body">
          <h5 class="card-title">Danh sách sản phẩm trả</h5>
          <div v-if="returnRequest.returnItems.length === 0">
            <em>Không có sản phẩm nào</em>
          </div>
          <div v-else class="table-responsive">
            <table class="table table-bordered align-middle">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Sản phẩm</th>
                  <th>Số lượng</th>
                  <th>Ghi chú</th>
                  <th>Hình ảnh</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in returnRequest.returnItems" :key="item.id">
                  <td>{{ index + 1 }}</td>
                  <td>{{ item.productName }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ item.note || 'Không có' }}</td>
                  <td>
                    <div v-if="item.imageUrls.length === 0">Không có</div>
                    <div v-else class="d-flex gap-2 flex-wrap">
                      <img
                        v-for="(url, idx) in item.imageUrls"
                        :key="idx"
                        :src="url"
                        alt="Ảnh sản phẩm"
                        style="width: 80px; height: 80px; object-fit: cover; border: 1px solid #ccc;"
                      />
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div v-else>
      <p class="text-danger">Không tìm thấy dữ liệu yêu cầu trả hàng.</p>
    </div>

    <ShowToastComponent ref="toastRef" />
  </div>
  <!-- Modal Confirm -->
<div class="modal fade" id="confirmModal" tabindex="-1" aria-hidden="true" ref="confirmModalEl">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">
        <i class="fas fa-question-circle text-primary me-2"></i> Xác nhận
    </h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        {{ confirmMessage }}
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" @click="confirmResolver(false)">Hủy</button>
        <button type="button" class="btn btn-primary" @click="confirmResolver(true)">Đồng ý</button>
      </div>
    </div>
  </div>
</div>

</template>
<style scoped>
.modal-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 600; /* Dày hơn một chút */
  color: #212529; /* Màu chữ tiêu đề */
}

.modal-body {
  font-family: 'Inter', sans-serif;
  color: #495057; /* Màu chữ nội dung */
}

.btn-primary {
  background-color: #007bff; /* Thay bằng màu chủ đạo bạn thích */
  border-color: #007bff;
  transition: all 0.3s ease; /* Hiệu ứng chuyển đổi mượt mà */
}

.btn-primary:hover {
  background-color: #0056b3; /* Sẫm hơn khi rê chuột vào */
  border-color: #0056b3;
  transform: translateY(-2px); /* Hiệu ứng nhấc nhẹ lên */
}

.btn-secondary {
  background-color: #6c757d; /* Thay bằng màu xám nhạt hơn, tinh tế hơn */
  border-color: #6c757d;
}

.modal-content {
  border-radius: 12px; /* Tăng độ bo góc */
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.15); /* Đổ bóng nhẹ */
  border: none; /* Bỏ đường viền mặc định */
}

.modal-header,
.modal-footer {
  border-bottom: none; /* Bỏ đường viền */
  border-top: none;
}
</style>