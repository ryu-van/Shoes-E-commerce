<template>
  <div class="dashboard">
    <!-- Stats Cards Row -->
    <div class="stats-grid">
      <div class="stat-card orange">
        <div class="stat-header">
          <div class="stat-info">
            <h3>TẠI QUẦY</h3>
            <div class="value">{{ formatCurrency(dailyOverview.taiQuay?.revenue) }} </div>
            <div class="sub-value">{{ dailyOverview.taiQuay?.count || '0' }} đơn</div>
          </div>
          <div class="stat-icon orange">📊</div>
        </div>
      </div>

      <div class="stat-card cyan">
        <div class="stat-header">
          <div class="stat-info">
            <h3>BÁN ONLINE</h3>
            <div class="value">{{ formatCurrency(dailyOverview.banOnline?.revenue) }}</div>
            <div class="sub-value">{{ dailyOverview.banOnline?.count || '0' }} đơn</div>
          </div>
          <div class="stat-icon cyan">💰</div>
        </div>
      </div>

      <div class="stat-card blue">
        <div class="stat-header">
          <div class="stat-info">
            <h3>ĐƠN HÀNG</h3>
            <div class="value">{{ dailyOverview.donHang?.count || '0' }} đơn</div>
          </div>
          <div class="stat-icon blue">📄</div>
        </div>
      </div>

      <div class="stat-card green">
        <div class="stat-header">
          <div class="stat-info">
            <h3>DOANH THU TỔNG</h3>
            <div class="value">{{ formatCurrency(dailyOverview.doanhThuTong?.amount) }}</div>
            <div class="sub-value">{{ dailyOverview.doanhThuTong?.count || '0' }} đơn</div>
          </div>
          <div class="stat-icon green">📈</div>
        </div>
      </div>
    </div>

    <!-- Chart Section -->
    <div class="chart-section">
      <div class="chart-header">
        <div class="chart-info">
          <h2 class="chart-title">Thống kê doanh thu & lợi nhuận</h2>
          <div class="chart-legend">
            <div class="legend-item">
              <div class="legend-dot revenue"></div>
              <span>Doanh thu (₫)</span>
            </div>
            <div class="legend-item">
              <div class="legend-dot cost"></div>
              <span>Chi phí (₫)</span>
            </div>
            <div class="legend-item">
              <div class="legend-dot profit"></div>
              <span>Lợi nhuận (₫)</span>
            </div>
          </div>
        </div>
        <div class="chart-controls">
          <div class="chart-filters">
            <!-- Year selector -->
            <select v-model="chartFilters.year" class="filter-select">
              <option v-for="year in availableYears" :key="year" :value="year">
                {{ year }}
              </option>
            </select>

            <!-- Order type selector -->
            <select v-model="chartFilters.orderType" class="filter-select">
              <option value="ALL">Tất cả</option>
              <option value="ONLINE">Online</option>
              <option value="OFFLINE">Tại quầy</option>
            </select>

            <!-- Apply button -->
            <button @click="applyChartFilters" class="apply-btn">
              Áp dụng
            </button>
          </div>
        </div>
      </div>
      <div class="chart-container">
        <canvas ref="chartRef"></canvas>
      </div>
    </div>

    <!-- Tables Section Below Chart -->
    <div class="tables-section">
      <!-- Best Selling Products Section -->
      <div class="products-section">
        <div class="products-header">
          <h2 class="products-title">Sản phẩm bán chạy</h2>
          <!-- Bộ lọc sản phẩm bán chạy -->
          <div class="filter-buttons">
            <button
                v-for="filter in filterOptions"
                :key="filter.value"
                @click="changeFilter(filter.value)"
                :class="['filter-btn', { active: selectedFilter === filter.value }]"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>
        <table class="products-table">
          <thead>
          <tr>
            <th class="sno text-center">STT</th>
            <th class="text-center">Ảnh</th>
            <th class="text-center">Tên sản phẩm</th>
            <th class="text-center">Số lượng đã bán</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(product, index) in bestSellers" :key="product.id" class="product-row">
            <td class="sno text-center vertical-mid">{{ index + 1 }}</td>
            <td class="text-center vertical-mid">
              <img :src="product.thumbnail ?? 'http://localhost:9000/product-variant-images/10969556-0fcd-43ff-b7ee-3af88423d16d_488px-No-Image-Placeholder.svg_.png'" alt="Ảnh sản phẩm" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px;">
            </td>
            <td class="text-center vertical-mid">
              <span class="product-name">{{ product.productName }}</span>
            </td>
            <td style="font-weight: 700; font-size: 14px;" class="text-center vertical-mid">{{ product.totalSold }}</td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- Pie Chart Section -->
      <div class="pie-chart-section">
        <div class="pie-chart-header">
          <h2 class="pie-chart-title">Biểu đồ trạng thái đơn hàng</h2>
          <!-- Bộ lọc biểu đồ tròn -->
          <div class="filter-buttons">
            <button
                v-for="filter in pieFilterOptions"
                :key="filter.value"
                @click="changePieFilter(filter.value)"
                :class="['filter-btn', { active: selectedPieFilter === filter.value }]"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>
        <div class="pie-chart-container">
          <canvas ref="pieChartRef"></canvas>
        </div>
        <div class="pie-chart-legend">
          <div v-for="(item, index) in pieChartData" :key="index" class="pie-legend-item">
            <div
                class="pie-legend-dot" :style="{ backgroundColor: pieChartColors[item.status] || '#ccc' }"></div>
            <span class="pie-legend-text">{{ item.label }}</span>
            <span class="pie-legend-value">{{ item.percentage }}%</span>
          </div>
        </div>

      </div>
    </div>

    <!-- Low Stock Products Section - Moved Below -->
    <div class="low-stock-full-section">
      <div class="low-stock-section">
        <div class="low-stock-header">
          <h2 class="low-stock-title">Sản phẩm sắp hết hàng</h2>
          <div class="pagination-info">
            <span>Trang {{ currentPage }} / {{ totalPages }} ({{ totalLowStockProducts }} sản phẩm)</span>
          </div>
        </div>
        <table class="low-stock-table">
          <thead>
          <tr>
            <th class="sno text-center">STT</th>
            <th class="text-center">Ảnh</th>
            <th class="text-center">Tên sản phẩm</th>
            <th class="text-center">Tồn kho</th>
            <th class="text-center">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(product, index) in paginatedLowStockProducts" :key="product.productId" class="low-stock-row">
            <td class="sno text-center vertical-mid">{{ (currentPage - 1) * itemsPerPage + index + 1 }}</td>
            <td class="text-center vertical-mid">
              <img :src="product.thumbnail" alt="Ảnh sản phẩm" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px;">
            </td>
            <td class="text-center vertical-mid">
              <div class="product-info">
                <span class="product-name">{{ product.productName }}</span>
              </div>
            </td>
            <td class="text-center vertical-mid">
              <span class="stock-number">{{ product.totalQuantity }}</span>
            </td>
            <td class="text-center vertical-mid">
              <router-link :to="`/admin/products/${product.productId}`" class="btn btn-sm btn-primary me-2">
                Chi tiết
              </router-link>
            </td>
          </tr>
          </tbody>
        </table>

        <!-- Pagination Controls -->
        <div class="pagination-container" v-if="totalPages > 1">
          <div class="pagination">
            <button
                class="pagination-btn prev"
                :disabled="currentPage === 1"
                @click="goToPage(currentPage - 1)"
            >
              ‹ Trước
            </button>

            <button
                v-for="page in visiblePages"
                :key="page"
                class="pagination-btn"
                :class="{ active: page === currentPage }"
                @click="goToPage(page)"
            >
              {{ page }}
            </button>

            <button
                class="pagination-btn next"
                :disabled="currentPage === totalPages"
                @click="goToPage(currentPage + 1)"
            >
              Sau ›
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref, watch, computed} from 'vue'
import {
  getLowStockProducts,
  getTopSellingProducts,
  getStatusChart,
  getDailyOverview,
  getRevenueProfitChart // Import API mới
} from "@/service/StatisticalApi";

// Thêm ref cho daily overview
const dailyOverview = ref({
  taiQuay: { revenue: '0đ', count: '0 đơn' },
  banOnline: { revenue: '0đ', count: '0 đơn' },
  donHang: { count: '0 đơn' },
  doanhThuTong: { amount: '0đ', count: '0 đơn' }
})

const bestSellers = ref([])
const lowStockProducts = ref([])
const selectedFilter = ref('7days') // Mặc định là 7 ngày
const selectedPieFilter = ref('7days') // Bộ lọc cho biểu đồ tròn
const chartRef = ref(null)
const pieChartRef = ref(null)
let chart = null
let pieChart = null

// Updated chart filters to match API requirements
const chartFilters = ref({
  year: new Date().getFullYear(), // Năm hiện tại
  orderType: 'ALL'
})

// Generate available years (current year and previous years)
const availableYears = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = currentYear; i >= currentYear - 5; i--) {
    years.push(i)
  }
  return years
})

// Pagination variables
const currentPage = ref(1)
const itemsPerPage = ref(5)

// Dữ liệu biểu đồ tròn
const pieChartData = ref([])

const pieChartColors = {
  PENDING: '#FF6B6B',      // Đỏ - Chờ xác nhận
  CONFIRMED: '#FFD166',    // Vàng - Đã xác nhận
  PROCESSING: '#4ECDC4',   // Xanh cyan - Đang xử lý
  SHIPPING: '#45B7D1',     // Xanh dương - Đang vận chuyển
  DELIVERED: '#96CEB4',    // Xanh lá - Đã giao
  COMPLETED: '#A29BFE',    // Tím nhạt - Đã hoàn thành
  CANCELLED: '#B0BEC5'     // Xám - Đã hủy
}

const statusLabels = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Chờ vận chuyển',
  SHIPPING: 'Đang vận chuyển',
  DELIVERED: 'Đã giao',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy'
}

// Các tùy chọn bộ lọc
const filterOptions = ref([
  {value: '7days', label: '7 ngày'},
  {value: 'month', label: 'Tháng'},
  {value: 'year', label: 'Năm'}
])

// Các tùy chọn bộ lọc cho biểu đồ tròn
const pieFilterOptions = ref([
  {value: '7days', label: '7 ngày'},
  {value: 'month', label: 'Tháng'},
  {value: 'year', label: 'Năm'}
])

// Computed properties for pagination
const totalLowStockProducts = computed(() => lowStockProducts.value.length)

const totalPages = computed(() => Math.ceil(totalLowStockProducts.value / itemsPerPage.value))

const paginatedLowStockProducts = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value
  const end = start + itemsPerPage.value
  return lowStockProducts.value.slice(start, end)
})

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 5) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    if (current <= 3) {
      for (let i = 1; i <= 5; i++) {
        pages.push(i)
      }
    } else if (current >= total - 2) {
      for (let i = total - 4; i <= total; i++) {
        pages.push(i)
      }
    } else {
      for (let i = current - 2; i <= current + 2; i++) {
        pages.push(i)
      }
    }
  }

  return pages
})

// Pagination methods
function goToPage(page) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

// Hàm gọi API daily overview
async function fetchDailyOverview() {
  try {
    const res = await getDailyOverview();
    console.log('Kết quả daily overview:', res.data.data);
    dailyOverview.value = res.data.data;
  } catch (err) {
    console.error('Lỗi khi lấy thống kê tổng quan hôm nay:', err)
    dailyOverview.value = {
      taiQuay: { revenue: '0đ', count: '0 đơn' },
      banOnline: { revenue: '0đ', count: '0 đơn' },
      donHang: { count: '0 đơn' },
      doanhThuTong: { amount: '0đ', count: '0 đơn' }
    }
  }
}

async function fetchBestSellers(filterType) {
  try {
    const res = await getTopSellingProducts(filterType)
    bestSellers.value = res.data.data
  } catch (err) {
    console.error('Lỗi khi lấy sản phẩm bán chạy:', err)
  }
}

// Hàm thay đổi bộ lọc
async function changeFilter(filterType) {
  selectedFilter.value = filterType
  await fetchBestSellers(filterType)
}

// Hàm thay đổi bộ lọc cho biểu đồ tròn
async function changePieFilter(filterType) {
  selectedPieFilter.value = filterType
  await fetchPieChartData(filterType)
}

async function fetchPieChartData(filterType) {
  try {
    const res = await getStatusChart(filterType)

    pieChartData.value = res.data.data.map(item => ({
      status: item.status,
      label: statusLabels[item.status] || item.status,
      value: item.totalOrders,
      percentage: item.percentage
    }))

    updatePieChart()
  } catch (err) {
    console.error('Lỗi khi lấy dữ liệu biểu đồ tròn:', err)
  }
}

function formatCurrency(amount) {
  if (!amount || amount === 0) return '0đ';
  return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
}

async function fetchLowStockProducts() {
  try {
    const res = await getLowStockProducts();
    lowStockProducts.value = res.data.data
    currentPage.value = 1
  } catch (err) {
    console.error('Lỗi khi lấy sản phẩm sắp hết hàng:', err)
  }
}

// Updated chart data fetching function to match API requirements
async function fetchChartData() {
  try {
    const res = await getRevenueProfitChart(chartFilters.value.year, chartFilters.value.orderType);
    const data = res.data.data

    updateChart(data)
  } catch (err) {
    console.error('Lỗi khi lấy dữ liệu biểu đồ:', err)
  }
}

// Hàm áp dụng filter cho chart
async function applyChartFilters() {
  await fetchChartData()
}

function updateChart(chartData) {
  const ctx = chartRef.value?.getContext('2d')
  if (!ctx) return

  if (chart) chart.destroy()

  // Convert month number to month name for better display
  const monthNames = [
    'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
    'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
  ]

  const labels = chartData.map(item => monthNames[parseInt(item.period) - 1] || `Tháng ${item.period}`)
  const revenues = chartData.map(item => parseFloat(item.revenue) || 0)
  const costs = chartData.map(item => parseFloat(item.cost) || 0)
  const profits = chartData.map(item => parseFloat(item.profit) || 0)

  chart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [
        {
          label: 'Doanh thu',
          data: revenues,
          backgroundColor: 'rgba(102, 126, 234, 0.8)',
          borderColor: 'rgba(102, 126, 234, 1)',
          borderWidth: 2,
          borderRadius: 8,
          maxBarThickness: 30,
          yAxisID: 'y'
        },
        {
          label: 'Chi phí',
          data: costs,
          backgroundColor: 'rgba(255, 193, 7, 0.8)',
          borderColor: 'rgba(255, 193, 7, 1)',
          borderWidth: 2,
          borderRadius: 8,
          maxBarThickness: 30,
          yAxisID: 'y'
        },
        {
          label: 'Lợi nhuận',
          data: profits,
          type: 'line',
          backgroundColor: 'rgba(220, 53, 69, 0.1)',
          borderColor: 'rgba(220, 53, 69, 1)',
          borderWidth: 3,
          fill: false,
          tension: 0.4,
          pointBackgroundColor: 'rgba(220, 53, 69, 1)',
          pointBorderColor: 'white',
          pointBorderWidth: 2,
          pointRadius: 6,
          yAxisID: 'y1'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false
      },
      plugins: {
        legend: {display: false},
        tooltip: {
          callbacks: {
            label: context => {
              const label = context.dataset.label
              const val = context.parsed.y
              return `${label}: ${formatCurrency(val)}`
            }
          }
        }
      },
      scales: {
        x: {
          grid: {display: false},
          ticks: {
            color: '#64748b',
            font: {weight: '500', size: 12}
          }
        },
        y: {
          beginAtZero: true,
          position: 'left',
          ticks: {
            color: '#667eea',
            callback: value => formatCurrency(value)
          },
          title: {
            display: true,
            text: 'Doanh thu & Chi phí (₫)',
            color: '#667eea',
            font: {weight: '600', size: 12}
          }
        },
        y1: {
          beginAtZero: true,
          position: 'right',
          grid: {drawOnChartArea: false},
          ticks: {
            color: '#dc3545',
            callback: value => formatCurrency(value)
          },
          title: {
            display: true,
            text: 'Lợi nhuận (₫)',
            color: '#dc3545',
            font: {weight: '600', size: 12}
          }
        }
      }
    }
  })
}

function updatePieChart() {
  const ctx = pieChartRef.value?.getContext('2d')
  if (!ctx) return

  const backgroundColors = pieChartData.value.map(item =>
      pieChartColors[item.status] || '#ccc'
  )

  if (pieChart) pieChart.destroy()

  pieChart = new Chart(ctx, {
    type: 'pie',
    data: {
      labels: pieChartData.value.map(item => item.label),
      datasets: [{
        data: pieChartData.value.map(item => item.value),
        backgroundColor: backgroundColors,
        borderColor: '#ffffff',
        borderWidth: 2,
        hoverOffset: 10
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: context => {
              const label = context.label
              const value = context.parsed
              const total = pieChartData.value.reduce((sum, item) => sum + item.value, 0)
              const percentage = Math.round((value / total) * 100)
              return `${label}: ${value} (${percentage}%)`
            }
          }
        }
      }
    }
  })
}

onMounted(() => {
  fetchDailyOverview()
  fetchBestSellers(selectedFilter.value)
  fetchLowStockProducts()
  fetchChartData() // Load với năm hiện tại
  fetchPieChartData(selectedPieFilter.value)
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  color: #333;
  padding: 24px;
}

.dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.chart-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.tables-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.low-stock-full-section {
  width: 100%;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 24px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 16px 16px 0 0;
}

.stat-card.orange::before {
  background: linear-gradient(135deg, #ff6b6b, #ee5a52);
}

.stat-card.cyan::before {
  background: linear-gradient(135deg, #4ecdc4, #44a08d);
}

.stat-card.blue::before {
  background: linear-gradient(135deg, #45b7d1, #3498db);
}

.stat-card.green::before {
  background: linear-gradient(135deg, #96ceb4, #2ecc71);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-info h3 {
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-info .value {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-info .sub-value {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  line-height: 1;
  margin-top: 10px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
}

.chart-info {
  flex: 1;
}

.chart-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 12px;
}

.chart-legend {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.revenue {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.legend-dot.cost {
  background: linear-gradient(135deg, #ffc107, #ff9800);
}

.legend-dot.profit {
  background: linear-gradient(135deg, #dc3545, #c82333);
}

.chart-controls {
  flex-shrink: 0;
}

.chart-filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-select {
  padding: 8px 12px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  background: white;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.apply-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.apply-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.chart-container {
  position: relative;
  height: 350px;
}

.pie-chart-container {
  position: relative;
  height: 250px;
  margin-bottom: 16px;
}

.pie-chart-legend {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.pie-legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.pie-legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.pie-legend-text {
  flex: 1;
  font-weight: 500;
}

.pie-legend-value {
  font-weight: 700;
  color: #1e293b;
}

.products-section, .low-stock-section, .pie-chart-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.products-header, .low-stock-header, .pie-chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.products-title, .low-stock-title, .pie-chart-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.pagination-info {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.filter-buttons {
  display: flex;
  gap: 8px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
}

.filter-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.filter-btn.active {
  background: #667eea;
  color: white;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
}

.products-table, .low-stock-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.products-table th, .products-table td,
.low-stock-table th, .low-stock-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.products-table th, .low-stock-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.product-row, .low-stock-row {
  transition: background-color 0.2s ease;
}

.product-row:hover, .low-stock-row:hover {
  background: rgba(102, 126, 234, 0.02);
}

.sno {
  width: 60px;
  font-weight: 700;
  color: #667eea;
}

.text-center {
  text-align: center;
}

.vertical-mid {
  vertical-align: middle;
}

.product-name {
  font-weight: 600;
  color: #1e293b;
}

.product-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stock-number {
  font-weight: 700;
  color: #dc2626;
  font-size: 14px;
}

.btn {
  display: inline-block;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  text-decoration: none;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5a67d8;
  transform: translateY(-1px);
}

.btn-sm {
  padding: 4px 8px;
  font-size: 11px;
}

.me-2 {
  margin-right: 8px;
}

/* Pagination Styles */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.pagination {
  display: flex;
  gap: 4px;
  align-items: center;
}

.pagination-btn {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  background: white;
  color: #475569;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  min-width: 40px;
  text-align: center;
}

.pagination-btn:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #667eea;
  color: #667eea;
}

.pagination-btn.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-btn.prev, .pagination-btn.next {
  padding: 8px 16px;
  font-weight: 600;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .tables-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .chart-legend {
    flex-direction: column;
    gap: 8px;
  }

  .products-header, .low-stock-header, .pie-chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .pie-chart-legend {
    grid-template-columns: 1fr;
  }

  .pagination {
    flex-wrap: wrap;
    gap: 8px;
  }

  .products-table, .low-stock-table {
    font-size: 12px;
  }

  .products-table th, .products-table td,
  .low-stock-table th, .low-stock-table td {
    padding: 8px;
  }
}

@media (max-width: 480px) {
  .dashboard {
    padding: 16px;
  }

  .chart-section, .products-section, .low-stock-section, .pie-chart-section {
    padding: 16px;
  }

  .stat-card {
    padding: 16px;
  }

  .chart-container {
    height: 250px;
  }

  .pie-chart-container {
    height: 200px;
  }
}

</style>