<script setup>
import { ref, onMounted, watch } from 'vue'
import { getAllReturnsByUser } from '@/service/ReturnApis'
import noImage from '@/assets/img/no-image.png'

const props = defineProps({
  query: { type: String, default: '' }
})

const returnRequests = ref([])
const loading = ref(true)

const statusLabels = {
  PENDING: 'Đang chờ duyệt',
  APPROVED: 'Đã chấp nhận',
  REJECTED: 'Bị từ chối',
  WAIT_FOR_PICKUP: 'Chờ lấy hàng',
  RETURNED: 'Đã trả hàng',
  REFUNDED: 'Đã hoàn tiền',
  CANCELLED: 'Đã hủy',
  COMPLETED: 'Hoàn tất'
}

function debounce(fn, wait = 300) {
  let t
  return (...args) => {
    clearTimeout(t)
    t = setTimeout(() => fn(...args), wait)
  }
}

async function fetchReturns() {
  loading.value = true
  try {
    const res = await getAllReturnsByUser(props.query)
    returnRequests.value = res?.data?.data ?? []
  } catch (err) {
    console.error('❌ Lỗi khi tải lịch sử trả hàng:', err)
  } finally {
    loading.value = false
  }
}

onMounted(fetchReturns)

const doSearch = debounce(fetchReturns, 300)
watch(() => props.query, () => doSearch())

function formatDate(dateString) {
  const d = new Date(dateString)
  return isNaN(d) ? '' : d.toLocaleDateString('vi-VN')
}
</script>

<template>
  <div class="return-history-container">
    <h2 class="title">Lịch sử trả hàng</h2>

    <div v-if="loading" class="loading-text">Đang tải...</div>

    <div v-else-if="(returnRequests?.length || 0) === 0" class="empty">
      Chưa có yêu cầu trả hàng nào.
    </div>

    <div v-else class="table-wrapper">
      <table class="return-table">
        <thead>
          <tr>
            <th class="col-stt">STT</th>
            <th>Sản phẩm / Mã đơn hàng</th>
            <th class="col-status">Trạng thái</th>
            <th class="col-date">Ngày tạo</th>
            <th class="col-action"></th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="(item, index) in returnRequests" :key="item.id">
            <td class="cell-center">{{ index + 1 }}</td>

            <td>
              <div class="order-code">
                <strong>Mã đơn hàng:</strong> {{ item?.order?.orderCode ?? '---' }}
              </div>

              <ul class="product-list">
                <li v-for="p in (item.returnItems || [])" :key="p.id" class="product-item">
                  <img
                    class="product-thumb"
                    :src="p?.imageUrls?.[0] || noImage"
                    :alt="p?.productName || 'Ảnh sản phẩm'"
                    @error="$event.target.src = noImage"
                    loading="lazy"
                  />
                  <span class="product-name" :title="p?.productName">
                    {{ p?.productName || 'Sản phẩm' }} (x{{ p?.quantity ?? 0 }})
                  </span>
                </li>
              </ul>
            </td>

            <td class="cell-center">
              <span :class="['status-tag', (item?.status || '').toLowerCase()]">
                {{ statusLabels[item?.status] || item?.status || '—' }}
              </span>
            </td>

            <td class="cell-center">{{ formatDate(item?.createdAt) }}</td>

            <td class="cell-center">
              <router-link :to="`/return-requests/${item.id}`" class="action-button">
                Xem chi tiết
              </router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.return-history-container { padding: 20px; }
.title { font-size: 22px; font-weight: 600; margin-bottom: 16px; }
.loading-text { font-size: 16px; color: #666; }

.empty {
  padding: 16px;
  background: #f8f9fa;
  border: 1px dashed #dcdcdc;
  border-radius: 8px;
  color: #666;
}

/* ================== TABLE ================== */
.table-wrapper { overflow-x: auto; }

.return-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  table-layout: fixed;
}

thead { background: #f6f8fa; }

th, td {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  vertical-align: middle;       /* cân giữa theo chiều dọc */
}

th {
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  text-align: left;
}

tr:last-child td { border-bottom: 0; }
tr:hover { background: #fafbfd; }

/* cột fix width để bố cục đều hơn */
.col-stt   { width: 72px;  text-align: center; }
.col-status{ width: 170px; text-align: center; }
.col-date  { width: 130px; text-align: center; }
.col-action{ width: 128px; }

.cell-center { text-align: center; }

/* ================== ORDER + PRODUCTS ================== */
.order-code { margin-bottom: 8px; }

.product-list {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 8px;
  list-style: none;
  padding: 0;
  margin: 0;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0; /* để ellipsis hoạt động */
}

.product-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e3e3e3;
  flex-shrink: 0;
}

.product-name {
  font-size: 14px;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 260px; /* giới hạn để không tràn */
}

/* ================== STATUS BADGE & ACTION ================== */
.status-tag {
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 10px;
  display: inline-block;
  text-transform: uppercase;
  white-space: nowrap;
}

.status-tag.pending        { background:#fff3cd; color:#856404; }
.status-tag.approved       { background:#d4edda; color:#155724; }
.status-tag.rejected       { background:#f8d7da; color:#721c24; }
.status-tag.completed      { background:#cce5ff; color:#004085; }
.status-tag.wait_for_pickup{ background:#e2e3ff; color:#383d7c; }
.status-tag.refunded       { background:#d1ecf1; color:#0c5460; }
.status-tag.returned       { background:#e6f0fb; color:#2d4a68; border:1px solid #d7e2f3; }
.status-tag.cancelled      { background:#f5c6cb; color:#721c24; }

.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  height: 32px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: #0d6efd;
  border-radius: 8px;
  text-decoration: none;
  transition: transform .05s ease, background-color .2s ease;
}
.action-button:hover { background: #0b5ed7; }
.action-button:active { transform: translateY(1px); }

/* responsive: nhiều sản phẩm thì chia cột cho gọn */
@media (min-width: 640px) { .product-list { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (min-width: 960px) { .product-list { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
</style>
