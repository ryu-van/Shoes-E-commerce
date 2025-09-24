<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getReturnDetailByAdmin, updateReturnStatus } from '@/service/ReturnApis'
import ShowToastComponent from '@/components/ShowToastComponent.vue'
import { Modal } from 'bootstrap'

// ===== State cho modal hoàn tiền =====
const refundModalEl = ref(null)
const refundMethod = ref('')
const refundReference = ref('')
const refundNote = ref('')

const showRefundModal = () => {
  const modal = new Modal(refundModalEl.value)
  modal.show()
}

const refundMethodTextMap = {
  BANK_TRANSFER: 'Chuyển khoản ngân hàng',
  EWALLET: 'Ví điện tử',
  CASH: 'Tiền mặt'
}
const getRefundMethodText = (m) => refundMethodTextMap[m] || (m ?? '—')

// Submit hoàn tiền
const submitRefund = async () => {
  if (!refundMethod.value?.trim()) {
    showToast('Vui lòng chọn phương thức hoàn tiền!', 'warning')
    return
  }
  // Gợi ý: nếu chuyển khoản thì nên nhập mã giao dịch
  if (refundMethod.value === 'BANK_TRANSFER' && !refundReference.value?.trim()) {
    showToast('Vui lòng nhập mã giao dịch ngân hàng.', 'warning')
    return
  }

  try {
    await updateReturnStatus(returnRequest.value.id, 'REFUNDED', {
      refundMethod: refundMethod.value,     // CASH | BANK_TRANSFER | EWALLET
      referenceCode: refundReference.value, // để trống nếu CASH -> BE tự sinh
      refundNote: refundNote.value          // đúng key với BE
    })

    showToast('💰 Hoàn tiền thành công!', 'success')

    // Đóng modal + reset form
    const modal = Modal.getInstance(refundModalEl.value)
    modal?.hide()
    refundMethod.value = ''
    refundReference.value = ''
    refundNote.value = ''

    await fetchReturnDetail()
  } catch (err) {
    console.error(err)
    showToast(err?.response?.data?.message || '❌ Hoàn tiền thất bại', 'error')
  }
}

// ===== Lý do hiển thị =====
const displayReason = computed(() => {
  const rr = returnRequest.value
  if (!rr) return ''
  if ((rr.returnItems?.length || 0) === 1) {
    return rr.returnItems?.[0]?.note || rr.reason || 'Không có'
  }
  return rr.reason || 'Không có'
})

// ===== Confirm modal đơn giản =====
const confirmModalEl = ref(null)
const confirmMessage = ref('')
let confirmResolver = null

const showConfirm = (message) => {
  confirmMessage.value = message
  return new Promise((resolve) => {
    confirmResolver = (result) => {
      resolve(result)
      confirmResolver = null
      const modal = Modal.getInstance(confirmModalEl.value)
      modal?.hide()
    }
    const modal = new Modal(confirmModalEl.value)
    modal.show()
  })
}

// ===== Data & helpers =====
const route = useRoute()
const router = useRouter()
const returnRequest = ref(null)
const loading = ref(true)
const toastRef = ref(null)

const goBack = () => router.back()
const showToast = (msg, type) => toastRef.value?.showToast(msg, type)

const statusTextMap = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  WAIT_FOR_PICKUP: 'Chờ lấy hàng',
  RETURNED: 'Đã nhận hàng',
  REFUNDED: 'Đã hoàn tiền',
  CANCELLED: 'Đã huỷ',
  COMPLETED: 'Hoàn tất'
}
const getStatusText = (s) => statusTextMap[s] || s

const fetchReturnDetail = async () => {
  try {
    const res = await getReturnDetailByAdmin(route.params.id)
    returnRequest.value = res.data.data
  } catch (err) {
    console.error('Lỗi reload chi tiết:', err)
  }
}

const handleStatusUpdate = async (newStatus) => {
  const confirmed = await showConfirm(
    `Bạn có chắc muốn chuyển sang trạng thái "${getStatusText(newStatus)}" không?`
  )
  if (!confirmed) return

  try {
    await updateReturnStatus(returnRequest.value.id, newStatus)
    showToast('✅ Cập nhật trạng thái thành công!', 'success')
    await fetchReturnDetail()
  } catch (err) {
    console.error(err)
    showToast(err?.response?.data?.message || '❌ Cập nhật trạng thái thất bại', 'error')
  }
}

onMounted(async () => {
  try {
    const res = await getReturnDetailByAdmin(route.params.id)
    returnRequest.value = res.data.data
  } catch (err) {
    console.error('Lỗi khi lấy chi tiết yêu cầu trả hàng:', err)
  } finally {
    loading.value = false
  }
})

function formatDate(dateStr) {
  return dateStr ? new Date(dateStr).toLocaleString('vi-VN') : '—'
}

function getStatusClass(status) {
  switch (status) {
    case 'PENDING': return 'badge bg-warning text-dark'
    case 'APPROVED': return 'badge bg-success'
    case 'REJECTED': return 'badge bg-danger'
    case 'WAIT_FOR_PICKUP': return 'badge bg-info text-dark'
    case 'RETURNED': return 'badge bg-primary'
    case 'REFUNDED': return 'badge bg-secondary'
    case 'CANCELLED': return 'badge bg-dark'
    case 'COMPLETED': return 'badge bg-success'
    default: return 'badge bg-secondary'
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
      <!-- Thông tin chung -->
      <div class="card mb-3 shadow-sm">
        <div class="card-body">
          <h5 class="card-title">Thông tin chung</h5>
          <p><strong>Trạng thái:</strong>
            <span :class="getStatusClass(returnRequest.status)">
              {{ getStatusText(returnRequest.status) }}
            </span>
          </p>
           <p>
          <strong>Số tiền hoàn dự kiến:</strong>
          {{ returnRequest.refundAmount?.toLocaleString('vi-VN') || 0 }} đ
        </p>
          <p><strong>Lý do:</strong> {{ displayReason }}</p>
          <p><strong>Ghi chú:</strong> {{ returnRequest.note || 'Không có' }}</p>
          <p><strong>Ngày tạo:</strong> {{ formatDate(returnRequest.createdAt) }}</p>
          <p><strong>Ngày cập nhật:</strong> {{ formatDate(returnRequest.updatedAt) }}</p>
        </div>
      </div>

      <!-- CARD: Thông tin đích nhận tiền KH đã cung cấp -->
      <div class="card mb-3 shadow-sm" v-if="returnRequest?.refundInfo">
        <div class="card-body">
          <h5 class="card-title">📮 Thông tin tài khoản KH</h5>
          <p>
            <strong>Phương thức:</strong>
            {{ getRefundMethodText(returnRequest?.refundInfo?.method) }}
          </p>

          <template v-if="returnRequest?.refundInfo?.method === 'BANK_TRANSFER'">
            <p><strong>Ngân hàng:</strong> {{ returnRequest?.refundInfo?.bankName || '—' }}</p>
            <p><strong>Số tài khoản:</strong> {{ returnRequest?.refundInfo?.accountNumber || '—' }}</p>
            <p><strong>Chủ tài khoản:</strong> {{ returnRequest?.refundInfo?.accountHolder || '—' }}</p>
          </template>

          <template v-else-if="returnRequest?.refundInfo?.method === 'EWALLET'">
            <p><strong>Ví:</strong> {{ returnRequest?.refundInfo?.walletProvider || '—' }}</p>
            <p><strong>Tài khoản ví:</strong> {{ returnRequest?.refundInfo?.walletAccount || '—' }}</p>
          </template>

          <template v-else>
            <p>Khách chọn nhận bằng <strong>tiền mặt</strong>.</p>
          </template>
        </div>
      </div>

      <!-- Thông tin hoàn tiền -->
      <div class="card mb-3 shadow-sm" v-if="returnRequest?.refundTransaction">
        <div class="card-body">
          <h5 class="card-title">💰 Thông tin hoàn tiền</h5>
          <p><strong>Số tiền hoàn:</strong>
            {{ (returnRequest?.refundTransaction?.amount || 0).toLocaleString('vi-VN') }} ₫
          </p>
          <p><strong>Phương thức:</strong>
            {{ getRefundMethodText(returnRequest?.refundTransaction?.method) }}
          </p>

          <p v-if="returnRequest?.refundTransaction?.referenceCode">
            <strong>Mã tham chiếu:</strong> {{ returnRequest?.refundTransaction?.referenceCode }}
          </p>
          <p v-if="returnRequest?.refundTransaction?.note">
            <strong>Ghi chú:</strong> {{ returnRequest?.refundTransaction?.note }}
          </p>
          <p class="text-muted">
            Tạo bởi {{ returnRequest?.refundTransaction?.createdBy || '—' }} lúc
            {{ formatDate(returnRequest?.refundTransaction?.createdAt) }}
          </p>
        </div>
      </div>

      <!-- Thao tác trạng thái -->
      <div
        class="card mb-3 shadow-sm"
        v-if="!['COMPLETED','REFUNDED','REJECTED'].includes(returnRequest.status)"
      >
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
              <button class="btn btn-warning" @click="showRefundModal">💰 Hoàn tiền</button>
            </template>

            <template v-else>
              <span class="text-muted">Không còn thao tác nào có thể thực hiện.</span>
            </template>
          </div>
        </div>
      </div>

      <!-- Thông tin đơn hàng -->
      <div class="card mb-3 shadow-sm">
        <div class="card-body">
          <h5 class="card-title">Thông tin đơn hàng</h5>
          <p><strong>Mã đơn hàng:</strong> {{ returnRequest?.order?.orderCode || '—' }}</p>
          <p><strong>Khách hàng:</strong> {{ returnRequest?.order?.fullname || '—' }}</p>
          <p><strong>SĐT:</strong> {{ returnRequest?.order?.phoneNumber || '—' }}</p>
          <p><strong>Địa chỉ giao hàng:</strong> {{ returnRequest?.order?.shippingAddress || '—' }}</p>
          <p>
            <strong>Tổng tiền:</strong>
            {{ (returnRequest?.order?.totalMoney || 0).toLocaleString('vi-VN') }} ₫
          </p>
        </div>
      </div>

      <!-- Danh sách sản phẩm trả -->
      <div class="card shadow-sm">
        <div class="card-body">
          <h5 class="card-title">Danh sách sản phẩm trả</h5>
          <div v-if="(returnRequest.returnItems?.length || 0) === 0">
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
                <tr v-for="(item, index) in (returnRequest.returnItems || [])" :key="item.id ?? index">
                  <td>{{ index + 1 }}</td>
                  <td>{{ item?.productName || '—' }}</td>
                  <td>{{ item?.quantity ?? '—' }}</td>
                  <td>{{ item?.note || 'Không có' }}</td>
                  <td>
                    <div v-if="(item?.imageUrls?.length || 0) === 0">Không có</div>
                    <div v-else class="d-flex gap-2 flex-wrap">
                      <img
                        v-for="(url, idx) in (item?.imageUrls || [])"
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

    <!-- Modal Refund -->
    <div class="modal fade" id="refundModal" tabindex="-1" aria-hidden="true" ref="refundModalEl">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">

          <!-- CARD: Thông tin đích nhận tiền KH đã cung cấp (trong modal) -->
          <div class="card mb-0 shadow-none border-0" v-if="returnRequest?.refundInfo">
            <div class="card-body pb-0">
              <h5 class="card-title">📮 Thông tin tài khoản khách hàng</h5>
              <p>
                <strong>Phương thức:</strong>
                {{ getRefundMethodText(returnRequest?.refundInfo?.method) }}
              </p>
 <p>
          <strong>Số tiền hoàn dự kiến:</strong>
          {{ returnRequest.refundAmount?.toLocaleString('vi-VN') || 0 }} đ
        </p>
              <template v-if="returnRequest?.refundInfo?.method === 'BANK_TRANSFER'">
                <p class="mb-1"><strong>Ngân hàng:</strong> {{ returnRequest?.refundInfo?.bankName || '—' }}</p>
                <p class="mb-1"><strong>Số tài khoản:</strong> {{ returnRequest?.refundInfo?.accountNumber || '—' }}</p>
                <p class="mb-1"><strong>Chủ tài khoản:</strong> {{ returnRequest?.refundInfo?.accountHolder || '—' }}</p>
              </template>

              <template v-else-if="returnRequest?.refundInfo?.method === 'EWALLET'">
                <p class="mb-1"><strong>Ví:</strong> {{ returnRequest?.refundInfo?.walletProvider || '—' }}</p>
                <p class="mb-1"><strong>Tài khoản ví:</strong> {{ returnRequest?.refundInfo?.walletAccount || '—' }}</p>
              </template>

              <template v-else>
                <p class="mb-1">Khách chọn nhận bằng <strong>tiền mặt</strong>.</p>
              </template>
            </div>
          </div>

          <div class="modal-header">
            <h5 class="modal-title">💰 Xử lý hoàn tiền</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>

          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label">Phương thức hoàn tiền</label>
              <select v-model="refundMethod" class="form-select">
                <option value="">-- Chọn --</option>
                <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
                <option value="CASH">Tiền mặt</option>
                <option value="EWALLET">Ví điện tử</option>
              </select>
            </div>

            <div class="mb-3">
              <label class="form-label">Mã tham chiếu</label>
              <input v-model="refundReference" type="text" class="form-control" placeholder="Ví dụ: Mã giao dịch ngân hàng" />
            </div>

            <div class="mb-3">
              <label class="form-label">Ghi chú</label>
              <textarea v-model="refundNote" rows="3" class="form-control" placeholder="Thêm ghi chú nếu có"></textarea>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
            <button type="button" class="btn btn-primary" @click="submitRefund">Xác nhận hoàn tiền</button>
          </div>
        </div>
      </div>
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
.modal-title { font-family: 'Poppins', sans-serif; font-weight: 600; color: #212529; }
.modal-body { font-family: 'Inter', sans-serif; color: #495057; }
.btn-primary { background-color: #007bff; border-color: #007bff; transition: all 0.3s ease; }
.btn-primary:hover { background-color: #0056b3; border-color: #0056b3; transform: translateY(-2px); }
.btn-secondary { background-color: #6c757d; border-color: #6c757d; }
.modal-content { border-radius: 12px; box-shadow: 0 10px 20px rgba(0,0,0,.15); border: none; }
.modal-header, .modal-footer { border-bottom: none; border-top: none; }
</style>
