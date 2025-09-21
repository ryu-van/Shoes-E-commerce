<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { getAllReturnsByUser } from '@/service/ReturnApis'
import noImage from '@/assets/img/no-image.png'

// ✅ nhận query từ OrderManagement
const props = defineProps({
  query: { type: String, default: '' }
})

const returnRequests = ref([])
const loading = ref(true)

// Mapping trạng thái sang tiếng Việt
const statusLabels = {
  pending: 'Đang chờ duyệt',
  approved: 'Đã chấp nhận',
  rejected: 'Bị từ chối',
  wait_for_pickup: 'Chờ lấy hàng',
  returned: 'Đã trả hàng',
  refunded: 'Đã hoàn tiền',
  cancelled: 'Đã hủy',
  completed: 'Hoàn tất',
}

// --- Hàm debounce mini (tránh gọi API liên tục khi gõ search)
function debounce(fn, wait = 300) {
  let t
  return (...args) => {
    clearTimeout(t)
    t = setTimeout(() => fn(...args), wait)
  }
}

// --- gọi API
async function fetchReturns() {
  loading.value = true
  try {
    const res = await getAllReturnsByUser(props.query)
    const raw = res?.data?.data ?? []

    returnRequests.value = raw.map(req => {
      const details = req?.order?.orderDetails || []

      const byOrderDetailId = new Map(
        details.map(d => [(d?.id ?? d?.orderDetailId), d])
      )
      const byVariantId = new Map(
        details.map(d => [d?.productVariantId, d])
      )

      const enrichedItems = (req.returnItems || []).map(ri => {
        const matched =
          byOrderDetailId.get(ri?.orderDetailId) ||
          byVariantId.get(ri?.productVariantId) ||
          null

        return {
          ...ri,
          __thumb: matched?.thumbnail || null,
          __name: matched?.productName || ri?.productName || 'Sản phẩm',
        }
      })

      return { ...req, returnItems: enrichedItems }
    })
  } catch (err) {
    console.error('Lỗi khi tải lịch sử trả hàng:', err)
  } finally {
    loading.value = false
  }
}

// --- gọi API lần đầu
onMounted(fetchReturns)

// --- gọi lại khi query thay đổi (debounce 300ms)
const doSearch = debounce(() => {
  fetchReturns()
}, 300)

watch(() => props.query, () => {
  doSearch()
})

function formatDate(dateString) {
  const d = new Date(dateString)
  return isNaN(d) ? '' : d.toLocaleDateString('vi-VN')
}
</script>


<template>
  <div class="return-history-container">
    <h2 class="title">Lịch sử trả hàng</h2>

    <div v-if="loading" class="loading-text">Đang tải...</div>

    <table v-else class="return-table">
      <thead>
        <tr>
          <th>STT</th>
          <th>Sản phẩm / Mã đơn hàng</th>
          <th>Trạng thái</th>
          <th>Ngày tạo</th>
          <th></th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="(item, index) in returnRequests" :key="item.id">
          <td>{{ index + 1 }}</td>

          <!-- Mã đơn + danh sách sản phẩm có ảnh sản phẩm gốc -->
          <td>
            <div class="order-code">
              <strong>Mã đơn hàng:</strong> {{ item?.order?.orderCode ?? '---' }}
            </div>

            <ul class="product-list">
              <li
                v-for="p in item.returnItems || []"
                :key="p.id"
                class="product-item"
              >
                <img
                  :src="p.__thumb || noImage"
                  :alt="p.__name"
                  class="product-image"
                  @error="$event.target.src = noImage"
                />
                <span class="product-text">
                  {{ p.__name }} (x{{ p.quantity }})
                </span>
              </li>
            </ul>
          </td>

          <td>
            <span :class="['status-tag', (item.status || '').toLowerCase()]">
              {{ statusLabels[(item.status || '').toLowerCase()] || item.status }}
            </span>
          </td>

          <td>{{ formatDate(item.createdAt) }}</td>

          <td>
            <router-link
              :to="`/return-requests/${item.id}`"
              class="action-button"
            >
              Xem chi tiết
            </router-link>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.return-history-container {
  padding: 20px;
}

.title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 16px;
}

.loading-text {
  font-size: 16px;
  color: #666;
}

.return-table {
  width: 100%;
  border-collapse: collapse;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

thead {
  background-color: #f6f8fa;
}

th,
td {
  padding: 12px 16px;
  text-align: left;
}

th {
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
}

td {
  border-bottom: 1px solid #f0f0f0;
}

tr:hover {
  background-color: #f9f9f9;
}

/* Mã đơn + danh sách sản phẩm */
.order-code {
  margin-bottom: 6px;
}

.product-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.product-image {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.product-text {
  line-height: 1.2;
}

/* Nút xem chi tiết */
.action-button {
  display: inline-block;
  padding: 6px 12px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  background-color: #007bff;
  border-radius: 6px;
  text-decoration: none;
  transition: background-color 0.2s ease;
}

.action-button:hover {
  background-color: #0056b3;
}

/* Badge trạng thái */
.status-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  display: inline-block;
  text-transform: uppercase;
}

.status-tag.pending { background-color: #fff3cd; color: #856404; }
.status-tag.approved { background-color: #d4edda; color: #155724; }
.status-tag.rejected { background-color: #f8d7da; color: #721c24; }
.status-tag.completed { background-color: #cce5ff; color: #004085; }
.status-tag.wait_for_pickup { background-color: #e2e3ff; color: #383d7c; }
.status-tag.refunded { background-color: #d1ecf1; color: #0c5460; }
.status-tag.returned { background-color: #cddef1; color: #383d3d; border: 1px solid #ddd; }
.status-tag.cancelled { background-color: #f5c6cb; color: #721c24; }
</style>
