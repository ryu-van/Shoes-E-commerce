<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { getAllProducts } from '@/service/ProductApi.js'
import { getAllColors } from "@/service/ColorApi.js"
import { getAllSizes } from "@/service/SizeApi.js"
import { getAllCategories } from "@/service/CategoryApi.js"
import { getAllMaterials } from "@/service/MaterialApi.js"
import { getAllBrands } from "@/service/BrandApi.js"
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const goToProductDetail = (id) => {
  router.push(`/product-detail/${id}`)
}

// Responsive & UI state
const isMobile = ref(window.innerWidth <= 768)
const isFiltersOpen = ref(false)
const isLoading = ref(false)

// Filter state
const sortBy = ref('id')
const sortDirection = ref('asc')
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)
const totalItems = ref(0)

const selectedSizes = ref([])
const selectedColors = ref([])
const selectedMaterials = ref([])
const selectedBrands = ref([])
const selectedCategories = ref([])
const selectedPrice = ref(null)
const selectedGender = ref([])
const keyword = ref("")
const status = ref(true)

const rangePrice = [
  { id: 1, label: 'Dưới 1 triệu', min: 0, max: 1000000 },
  { id: 2, label: '1-3 triệu', min: 1000000, max: 3000000 },
  { id: 3, label: '3-5 triệu', min: 3000000, max: 5000000 },
  { id: 4, label: 'Trên 5 triệu', min: 5000000, max: Infinity }
];

// Filter panel visibility
const isMaterialFilterOpen = ref(false)
const isSizeFilterOpen = ref(false)
const isColorFilterOpen = ref(false)
const isPriceFilterOpen = ref(false)
const isCategoryFilterOpen = ref(false)
const isBrandFilterOpen = ref(false)
const isGenderFilterOpen = ref(false)

// Data
const products = ref([])
const colors = ref([])
const sizes = ref([])
const materials = ref([])
const categories = ref([])
const brands = ref([])
const gender = ref([])

// Computed properties
const activeFiltersCount = computed(() => {
  return selectedGender.value.length +
      selectedSizes.value.length +
      selectedColors.value.length +
      selectedCategories.value.length +
      selectedMaterials.value.length +
      selectedBrands.value.length +
      (selectedPrice.value ? 1 : 0)
})

const paginationInfo = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value + 1
  const end = Math.min(currentPage.value * pageSize.value, totalItems.value)
  return { start, end }
})

const getPaginationPages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) pages.push(i)
      pages.push('...', total)
    } else if (current >= total - 3) {
      pages.push(1, '...')
      for (let i = total - 4; i <= total; i++) pages.push(i)
    } else {
      pages.push(1, '...')
      for (let i = current - 1; i <= current + 1; i++) pages.push(i)
      pages.push('...', total)
    }
  }
  return pages
})

// Computed property for page title based on gender
const pageTitle = computed(() => {
  const genderParam = route.query.gender
  switch (genderParam) {
    case 'Male':
      return 'Giày Nam'
    case 'Female':
      return 'Giày Nữ'
    case 'Unisex':
      return 'Giày Unisex'
    case 'Kids':
      return 'Giày Trẻ Em'
    default:
      return 'Sản phẩm'
  }
})

const sortOptions = [
  { value: 'id', label: 'Mặc định', direction: 'asc' },
  { value: 'name', label: 'Tên A - Z', direction: 'asc' },
  { value: 'name', label: 'Tên Z - A', direction: 'desc' },
  { value: 'price', label: 'Giá thấp đến cao', direction: 'asc' },
  { value: 'price', label: 'Giá cao đến thấp', direction: 'desc' },
  { value: 'created_at', label: 'Mới nhất', direction: 'desc' },
  { value: 'createdAt', label: 'Cũ nhất', direction: 'asc' },
  { value: 'rating', label: 'Điểm đánh giá', direction: 'desc' }
]

// Computed property để tìm index của sort option hiện tại
const currentSortIndex = computed(() => {
  const index = sortOptions.findIndex(option =>
      option.value === sortBy.value && option.direction === sortDirection.value
  )
  return index >= 0 ? index : 0
})

// Hàm kiểm tra sản phẩm có giá hợp lệ
const isValidProduct = (product) => {
  // Kiểm tra minPrice
  if (product.minPrice && product.minPrice > 0) return true;

  // Kiểm tra price
  if (product.price && product.price > 0) return true;

  // Kiểm tra variants nếu có
  if (product.variants && product.variants.length > 0) {
    return product.variants.some(variant =>
        variant.sellPrice && variant.sellPrice > 0
    );
  }

  return false;
}

// Computed property để lọc sản phẩm có giá hợp lệ
const validProducts = computed(() => {
  return products.value.filter(product => isValidProduct(product));
})

// Methods
const formatPrice = (price) => {
  if (!price && price !== 0) return 'Liên hệ'
  return price.toLocaleString('vi-VN') + 'đ'
}

const getProductPrice = (product) => {
  if (product.variants?.length > 0) {
    const validPrices = product.variants
        .map(v => v.sellPrice || 0)
        .filter(price => price > 0);

    if (validPrices.length > 0) {
      const min = Math.min(...validPrices)
      const max = Math.max(...validPrices)
      return min === max ? formatPrice(min) : `${formatPrice(min)} - ${formatPrice(max)}`
    }
  }

  if (product.minPrice && product.minPrice > 0) {
    return formatPrice(product.minPrice)
  }

  if (product.price && product.price > 0) {
    return formatPrice(product.price)
  }

  return 'Liên hệ'
}

const handleImageError = (e) => {
  e.target.src = '/404.jpg'
}

const updateUrlParams = () => {
  const query = { ...route.query }

  // Xóa các params cũ liên quan đến filter
  delete query.brand
  delete query.category
  delete query.gender
  delete query.sortBy
  delete query.sortDirection

  // Thêm lại các params từ filter hiện tại
  if (selectedBrands.value.length > 0) {
    // Nếu chỉ có 1 item, dùng string đơn giản
    if (selectedBrands.value.length === 1) {
      query.brand = selectedBrands.value[0].toString()
    } else {
      // Nếu có nhiều items, dùng comma-separated string
      query.brand = selectedBrands.value.join(',')
    }
  }

  if (selectedCategories.value.length > 0) {
    if (selectedCategories.value.length === 1) {
      query.category = selectedCategories.value[0].toString()
    } else {
      query.category = selectedCategories.value.join(',')
    }
  }

  if (selectedGender.value.length > 0) {
    if (selectedGender.value.length === 1) {
      query.gender = selectedGender.value[0]
    } else {
      query.gender = selectedGender.value.join(',')
    }
  }

  // Thêm sort params
  if (sortBy.value !== 'id' || sortDirection.value !== 'asc') {
    query.sortBy = sortBy.value
    query.sortDirection = sortDirection.value
  }

  // Cập nhật URL mà không trigger watch
  router.replace({ query })
}

// Parse params to array
const parseQueryParam = (param) => {
  if (!param) return []
  if (Array.isArray(param)) return param
  if (typeof param === 'string' && param.includes(',')) {
    return param.split(',').filter(Boolean)
  }
  return [param]
}

const handleRouteParams = () => {
  // Reset filters trước khi áp dụng params mới
  selectedCategories.value = []
  selectedBrands.value = []
  selectedGender.value = []

  const genderParams = parseQueryParam(route.query.gender)
  if (genderParams.length > 0) {
    selectedGender.value = [...genderParams]
  }

  const brandParams = parseQueryParam(route.query.brand)
  if (brandParams.length > 0) {
    selectedBrands.value = brandParams.map(b => parseInt(b)).filter(b => !isNaN(b))
  }

  const categoryParams = parseQueryParam(route.query.category)
  if (categoryParams.length > 0) {
    selectedCategories.value = categoryParams.map(c => parseInt(c)).filter(c => !isNaN(c))
  }

  // Xử lý sort params
  if (route.query.sortBy) {
    sortBy.value = route.query.sortBy
  }

  if (route.query.sortDirection) {
    sortDirection.value = route.query.sortDirection
  }
}

const fetchDataFilters = async () => {
  try {
    const [sizeRes, colorRes, categoryRes, materialRes, brandRes] = await Promise.all([
      getAllSizes(),
      getAllColors(),
      getAllCategories(),
      getAllMaterials(),
      getAllBrands()
    ])

    sizes.value = sizeRes.data.data || []
    colors.value = colorRes.data.data || []
    categories.value = categoryRes.data.data || []
    materials.value = materialRes.data.data || []
    brands.value = brandRes.data.data || []
  } catch (e) {
    console.error("Filter data error:", e)
  }
}

const fetchProducts = async () => {
  isLoading.value = true
  try {
    let minPrice, maxPrice
    if (selectedPrice.value) {
      const selectedPriceRange  = rangePrice.find(p => p.id === selectedPrice.value)
      if (selectedPriceRange) {
        minPrice = selectedPriceRange.min
        maxPrice = selectedPriceRange.max === Infinity ? undefined : selectedPriceRange.max
      }
    }

    const params = {
      keyword: keyword.value?.trim() || undefined,
      brand_id: selectedBrands.value.length ? selectedBrands.value : undefined,
      category_id: selectedCategories.value.length ? selectedCategories.value : undefined,
      gender: selectedGender.value.length ? selectedGender.value : undefined,
      minPrice: minPrice,
      maxPrice: maxPrice,
      size_id: selectedSizes.value.length ? selectedSizes.value.map(s => s.id) : undefined,
      color_id: selectedColors.value.length ? selectedColors.value.map(c => c.id) : undefined,
      material_id: selectedMaterials.value.length ? selectedMaterials.value : undefined,
      status: status.value,
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value,
      sortDirection: sortDirection.value
    }

    // Remove undefined values
    Object.keys(params).forEach(key => params[key] === undefined && delete params[key])

    const res = await getAllProducts(params)
    const data = res.data.data

    // Lọc sản phẩm có giá hợp lệ trước khi gán
    const allProducts = data.content || []
    products.value = allProducts.filter(product => isValidProduct(product))

    // Cập nhật pagination info dựa trên sản phẩm đã lọc
    totalPages.value = data.totalPages || 0
    totalItems.value = products.value.length
  } catch (e) {
    console.error("Product load error:", e)
    products.value = []
    totalPages.value = 0
    totalItems.value = 0
  } finally {
    isLoading.value = false
  }
}

const toggleCategory = (category) => {
  const index = selectedCategories.value.indexOf(category.id)
  if (index >= 0) {
    selectedCategories.value = selectedCategories.value.filter(id => id !== category.id)
  } else {
    selectedCategories.value = [...selectedCategories.value, category.id]
  }
  updateUrlParams()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const toggleBrand = (brand) => {
  const index = selectedBrands.value.indexOf(brand.id)
  if (index >= 0) {
    selectedBrands.value = selectedBrands.value.filter(id => id !== brand.id)
  } else {
    selectedBrands.value = [...selectedBrands.value, brand.id]
  }
  updateUrlParams()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const toggleGender = (genderValue) => {
  const index = selectedGender.value.indexOf(genderValue)
  if (index >= 0) {
    selectedGender.value = selectedGender.value.filter(g => g !== genderValue)
  } else {
    selectedGender.value = [...selectedGender.value, genderValue]
  }
  updateUrlParams()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const toggleColor = (color) => {
  const exists = selectedColors.value.some(c => c.id === color.id)
  selectedColors.value = exists
      ? selectedColors.value.filter(c => c.id !== color.id)
      : [...selectedColors.value, color]
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const toggleSize = (size) => {
  const exists = selectedSizes.value.some(s => s.id === size.id)
  selectedSizes.value = exists
      ? selectedSizes.value.filter(s => s.id !== size.id)
      : [...selectedSizes.value, size]
}

const toggleMaterial = (materialId) => {
  const exists = selectedMaterials.value.includes(materialId)
  selectedMaterials.value = exists
      ? selectedMaterials.value.filter(m => m !== materialId)
      : [...selectedMaterials.value, materialId]
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const togglePrice = (price) => {
  if (selectedPrice.value === price.id) {
    selectedPrice.value = null
  } else {
    selectedPrice.value = price.id
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const clearFilters = () => {
  selectedSizes.value = []
  selectedColors.value = []
  selectedMaterials.value = []
  selectedCategories.value = []
  selectedPrice.value = null
  selectedBrands.value = []
  selectedGender.value = []
  keyword.value = ""
  sortBy.value = 'id'
  sortDirection.value = 'asc'
  currentPage.value = 1

  // Xóa tất cả query params
  router.replace({ query: {} })
}

const applyFilters = () => {
  currentPage.value = 1
  fetchProducts()
  if (isMobile.value) isFiltersOpen.value = false
}

const goToPage = (page) => {
  if (page !== '...' && page !== currentPage.value) {
    currentPage.value = page
    fetchProducts()
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchProducts()
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchProducts()
  }
}

const getDisplayedStars = (rating) => {
  const fullStars = Math.floor(rating)
  const hasHalfStar = rating - fullStars >= 0.5 && rating - fullStars < 1
  return { fullStars, hasHalfStar }
}

const changeSortBy = (option) => {
  sortBy.value = option.value
  sortDirection.value = option.direction
  currentPage.value = 1
  updateUrlParams()
  fetchProducts()
}

const changePageSize = () => {
  currentPage.value = 1
  fetchProducts()
}

const toggleFilters = () => {
  isFiltersOpen.value = !isFiltersOpen.value
}

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) isFiltersOpen.value = false
}

// Watch for route changes - chỉ khi từ bên ngoài (navigation)
watch(() => route.query, (newQuery, oldQuery) => {
  if (JSON.stringify(newQuery) !== JSON.stringify(oldQuery)) {
    currentPage.value = 1
    handleRouteParams()
    fetchProducts()
  }
}, { immediate: true })

// Watch cho các filter khác (không liên quan đến URL)
watch([selectedColors, selectedSizes, selectedMaterials, selectedPrice], () => {
  currentPage.value = 1
  fetchProducts()
})

watch(pageSize, changePageSize)

let searchTimeout = null
watch(keyword, () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    currentPage.value = 1
    fetchProducts()
  }, 500)
})

// Lifecycle
onMounted(async () => {
  await fetchDataFilters()
  handleRouteParams()
  fetchProducts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (searchTimeout) clearTimeout(searchTimeout)
})

</script>

<template>
  <div class="product-list-page">
    <div class="container">
      <!-- Mobile Filter Toggle -->
      <div class="mobile-filter-toggle" v-if="isMobile">
        <button @click="toggleFilters" class="filter-toggle-btn">
          <span class="filter-icon">⚙️</span>
          Bộ lọc
          <span class="filter-count" v-if="activeFiltersCount > 0">
            ({{ activeFiltersCount }})
          </span>
        </button>
      </div>

      <!-- Mobile Filters Overlay -->
      <div v-if="isMobile && isFiltersOpen" class="mobile-filters-overlay" @click="toggleFilters"></div>

      <div class="main-content" :class="{ 'mobile-layout': isMobile }">
        <!-- Sidebar Filters -->
        <aside class="sidebar" :class="{ 'mobile-sidebar': isMobile, 'open': isFiltersOpen }">
          <div class="filters">
            <div class="filter-header">
              <h3>Bộ lọc</h3>
              <div class="filter-actions">
                <button @click="clearFilters" class="clear-filters">Xóa tất cả</button>
                <button v-if="isMobile" @click="toggleFilters" class="close-filters">✕</button>
              </div>
            </div>

            <!-- Search Filter -->
            <div class="filter-section">
              <input v-model="keyword" type="text" placeholder="Tìm kiếm sản phẩm..." class="search-input"
                     @keyup.enter="applyFilters" />
            </div>

            <!-- Gender Filter - Chỉ hiển thị khi không có gender trong route -->
            <div class="filter-section" v-if="!route.query.gender">
              <div class="filter-section-header" @click="isGenderFilterOpen = !isGenderFilterOpen">
                <h4>Giới tính</h4>
                <span class="arrow" :class="{ 'open': isGenderFilterOpen }">›</span>
              </div>
              <div class="filter-content" v-show="isGenderFilterOpen">
                <div class="filter-item"
                     :class="{ 'active': selectedGender.includes('Male') }"
                     @click="toggleGender('Male')">
                  Nam
                </div>
                <div class="filter-item"
                     :class="{ 'active': selectedGender.includes('Female') }"
                     @click="toggleGender('Female')">
                  Nữ
                </div>
                <div class="filter-item"
                     :class="{ 'active': selectedGender.includes('Unisex') }"
                     @click="toggleGender('Unisex')">
                  Unisex
                </div>
                <div class="filter-item"
                     :class="{ 'active': selectedGender.includes('Kids') }"
                     @click="toggleGender('Kids')">
                  Trẻ em
                </div>
              </div>
            </div>

            <!-- Category Filter -->
            <div class="filter-section">
              <div class="filter-section-header" @click="isCategoryFilterOpen = !isCategoryFilterOpen">
                <h4>Danh mục</h4>
                <span class="arrow" :class="{ 'open': isCategoryFilterOpen }">›</span>
              </div>
              <div class="filter-content" v-show="isCategoryFilterOpen">
                <div v-for="category in categories" :key="category.id" class="filter-item"
                     :class="{ 'active': selectedCategories.includes(category.id) }" @click="toggleCategory(category)">
                  {{ category.name }}
                </div>
              </div>
            </div>

            <!-- Brand Filter -->
            <div class="filter-section">
              <div class="filter-section-header" @click="isBrandFilterOpen = !isBrandFilterOpen">
                <h4>Thương hiệu</h4>
                <span class="arrow" :class="{ 'open': isBrandFilterOpen }">›</span>
              </div>
              <div class="filter-content" v-show="isBrandFilterOpen">
                <div v-for="brand in brands" :key="brand.id" class="filter-item"
                     :class="{ 'active': selectedBrands.includes(brand.id) }" @click="toggleBrand(brand)">
                  {{ brand.name }}
                </div>
              </div>
            </div>

            <!-- Price Filter -->
            <div class="filter-section">
              <div class="filter-section-header" @click="isPriceFilterOpen = !isPriceFilterOpen">
                <h4>Mức giá</h4>
                <span class="arrow" :class="{ 'open': isPriceFilterOpen }">›</span>
              </div>
              <div class="filter-content" v-show="isPriceFilterOpen">
                <div v-for="price in rangePrice" :key="price.id" class="filter-item"
                     :class="{ 'active': selectedPrice === price.id }" @click="togglePrice(price)">
                  {{ price.label }}
                </div>
              </div>
            </div>

            <!-- Colors Filter -->
            <div class="filter-section">
              <div class="filter-section-header" @click="isColorFilterOpen = !isColorFilterOpen">
                <h4>Màu sắc</h4>
                <span class="arrow" :class="{ 'open': isColorFilterOpen }">›</span>
              </div>
              <div class="filter-content" v-show="isColorFilterOpen">
                <div class="color-grid">
                  <div v-for="color in colors" :key="color.id" class="color-swatch"
                       :class="{ active: selectedColors.some(c => c.id === color.id) }"
                       :style="{ backgroundColor: color.hexCode || '#000000' }" @click="toggleColor(color)"
                       :title="color.name">
                    <span v-if="selectedColors.some(c => c.id === color.id)" class="color-check">✓</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Size Filter -->
            <div class="filter-section">
              <div class="filter-section-header" @click="isSizeFilterOpen = !isSizeFilterOpen">
                <h4>Kích thước</h4>
                <span class="arrow" :class="{ 'open': isSizeFilterOpen }">›</span>
              </div>
              <div class="filter-content" v-show="isSizeFilterOpen">
                <div class="size-grid">
                  <button v-for="size in sizes" :key="size.id" class="size-button"
                          :class="{ active: selectedSizes.some(s => s.id === size.id) }" @click="toggleSize(size)">
                    {{ size.value }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Material Filter -->
            <div class="filter-section">
              <div class="filter-section-header" @click="isMaterialFilterOpen = !isMaterialFilterOpen">
                <h4>Chất liệu</h4>
                <span class="arrow" :class="{ 'open': isMaterialFilterOpen }">›</span>
              </div>
              <div class="filter-content" v-show="isMaterialFilterOpen">
                <div v-for="material in materials" :key="material.id" class="filter-item"
                     :class="{ 'active': selectedMaterials.includes(material.id) }"
                     @click="toggleMaterial(material.id)">
                  {{ material.name }}
                </div>
              </div>
            </div>

            <button class="apply-filter" @click="applyFilters">Áp dụng bộ lọc</button>
          </div>
        </aside>

        <!-- Product Grid -->
        <main class="product-section">
          <div class="section-header">
            <h1>{{ pageTitle }}</h1>
            <div class="section-meta">
              <span class="product-count">
                Hiển thị {{ paginationInfo.start }}-{{ paginationInfo.end }} của {{ totalItems }} sản phẩm
              </span>
              <div class="sort-dropdown">
                <label>Sắp xếp theo:</label>
                <select :value="currentSortIndex" @change="changeSortBy(sortOptions[$event.target.value])">
                  <option v-for="(option, index) in sortOptions" :key="index" :value="index">
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </div>
          </div>

          <!-- Loading State -->
          <div v-if="isLoading" class="loading-state">
            <div class="loading-spinner"></div>
            <p>Đang tải sản phẩm...</p>
          </div>

          <!-- Empty State -->
          <div v-else-if="validProducts.length === 0" class="empty-state">
            <div class="empty-icon">📦</div>
            <h3>Không tìm thấy sản phẩm</h3>
            <p>Hãy thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm</p>
            <button @click="clearFilters" class="reset-filters-btn">Xóa bộ lọc</button>
          </div>

          <!-- Product Grid -->
          <div v-else class="product-grid">
            <div v-for="product in validProducts" :key="product.id" class="product-card"
                 @click="goToProductDetail(product.id)">
              <img :src="product.thumbnail || '/404.jpg'" :alt="product.name" class="product-image"
                   @error="handleImageError"/>
              <div class="product-discount" v-if="product.discount">
                -{{ product.discount }}%
              </div>
              <div class="product-info">
                <h5 class="product-title">{{ product.name }}</h5>
                <div class="rating">
                  <span v-for="n in 5" :key="n" class="star" :class="{
                          filled: n <= getDisplayedStars(product.averageRating || 0).fullStars,
                          half: n === getDisplayedStars(product.averageRating || 0).fullStars + 1 &&
                          getDisplayedStars(product.averageRating || 0).hasHalfStar
                  }">★</span>
                  <span class="rating-text">{{ (Number(product.averageRating).toFixed(1) || 0) }}/5</span>
                </div>
                <div class="product-price">
                  {{ getProductPrice(product) }}
                  <span v-if="product.originalPrice" class="old-price">
                    {{ formatPrice(product.originalPrice) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div class="pagination" v-if="totalPages >= 1 && !isLoading">
            <div class="pagination-left">
              <div class="items-per-page">
                <span>Hiển thị</span>
                <select v-model="pageSize" class="items-select">
                  <option value="10">10</option>
                  <option value="15">15</option>
                  <option value="20">20</option>
                  <option value="50">50</option>
                </select>
                <span>sản phẩm</span>
              </div>
            </div>

            <div class="pagination-center">
              <button class="pagination-btn prev" @click="prevPage" :disabled="currentPage === 1">‹</button>
              <div class="pagination-numbers">
                <button v-for="page in getPaginationPages" :key="page" class="pagination-number"
                        :class="{ active: page === currentPage }" @click="goToPage(page)" v-if="page !== '...'">
                  {{ page }}
                </button>
                <span v-else class="pagination-ellipsis">...</span>
              </div>
              <button class="pagination-btn next" @click="nextPage" :disabled="currentPage === totalPages">›</button>
            </div>

            <div class="pagination-right">
              <span class="pagination-info">
                Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong tổng số {{ totalItems }} sản phẩm
              </span>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Container and Layout */
.product-list-page {
  min-height: 100vh;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.main-content {
  display: flex;
  gap: 24px;
  position: relative;
}

.main-content.mobile-layout {
  flex-direction: column;
}

/* Mobile Filter Toggle */
.mobile-filter-toggle {
  margin-bottom: 20px;
  display: none;
}

.filter-toggle-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  width: 100%;
  justify-content: center;
}

.filter-toggle-btn:hover {
  background: #f8f9fa;
  border-color: #007bff;
}

.filter-count {
  background: #007bff;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: bold;
}

.mobile-filters-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 998;
  display: none;
}

/* Sidebar */
.sidebar {
  width: 300px;
  height: fit-content;
  position: sticky;
  top: 20px;
}

.mobile-sidebar {
  position: fixed;
  top: 0;
  left: -100%;
  width: 280px;
  height: 100vh;
  z-index: 999;
  transition: left 0.3s ease;
  overflow-y: auto;
  background: white;
}

.mobile-sidebar.open {
  left: 0;
}

.filters {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
  background: #f8f9fa;
}

.filter-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.filter-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.clear-filters,
.close-filters {
  padding: 6px 12px;
  border: 1px solid #ddd;
  background: black;
  color: white;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.clear-filters:hover,
.close-filters:hover {
  background: #474747;
  border-color: #999;
}

/* Filter Sections */
.filter-section {
  border-bottom: 1px solid #e9ecef;
}

.filter-section:last-child {
  border-bottom: none;
}

.search-input {
  width: 100%;
  padding: 12px 16px;
  border: none;
  border-bottom: 1px solid #e9ecef;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #007bff;
}

.filter-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.filter-section-header:hover {
  background: #e9ecef;
}

.filter-section-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.arrow {
  font-size: 14px;
  color: #666;
  transition: transform 0.2s ease;
  transform: rotate(0deg);
}

.arrow.open {
  transform: rotate(90deg);
}

.filter-content {
  background: white;
}

.filter-item {
  padding: 12px 20px;
  cursor: pointer;
  font-size: 14px;
  color: #555;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-item:hover {
  background: #f8f9fa;
  color: #333;
}

.filter-item.active {
  background: #e3f2fd;
  color: #000000;
  border-left-color: #555;
  font-weight: 500;
}

/* Price Range */
.price-range {
  padding: 16px 20px;
}

.price-inputs {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.price-input-group {
  flex: 1;
}

.price-input-group label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.price-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
  box-sizing: border-box;
}

.price-input:focus {
  border-color: #007bff;
}

.price-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
}

/* Color Grid */
.color-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  padding: 16px 20px;
}

.color-swatch {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #ddd;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  position: relative;
}

.color-swatch:hover {
  transform: scale(1.1);
  border-color: #3a3a3a;
}

.color-swatch.active {
  border-color: #3a3a3a;
  border-width: 3px;
}

.color-check {
  color: white;
  font-size: 14px;
  font-weight: bold;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5);
}

.color-swatch[style*="rgb(255, 255, 255)"] .color-check,
.color-swatch[style*="#FFFFFF"] .color-check,
.color-swatch[style*="#ffffff"] .color-check {
  color: #333;
  text-shadow: 0 0 2px rgba(255, 255, 255, 0.8);
}

/* Size Grid */
.size-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 16px 20px;
}

.size-button {
  padding: 8px 12px;
  border: 1px solid #ddd;
  background: white;
  color: #555;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  text-align: center;
}

.size-button:hover {
  background: #f8f9fa;
  border-color: #3a3a3a;
}

.size-button.active {
  background: black;
  color: white;
  border-color: #3a3a3a;
}

/* Apply Filter Button */
.apply-filter {
  width: 100%;
  padding: 12px 16px;
  background: black;
  color: white;
  border: none;
  border-radius: 0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
  margin-top: 16px;
}

.apply-filter:hover {
  background: #131313;
}

/* Product Section */
.product-section {
  flex: 1;
  min-width: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.section-meta {
  display: flex;
  align-items: center;
  gap: 20px;
}

.product-count {
  font-size: 14px;
  color: #666;
}

.sort-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.sort-dropdown label {
  color: #666;
}

.sort-dropdown select {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  cursor: pointer;
}

/* Loading and Empty States */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #333;
}

.empty-state p {
  margin: 0 0 24px;
  color: #666;
  font-size: 14px;
}

.reset-filters-btn {
  padding: 10px 20px;
  background: #000000;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s ease;
}

.reset-filters-btn:hover {
  background: #333;
}

/* Product Grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.product-card {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  transition: transform 0.3s ease;
  margin-bottom: 30px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
}

.product-card:hover {
  transform: translateY(-2px);
}

.product-image {
  width: 100%;
  height: 300px;
  object-fit: cover;
  background-color: #f0f0f0;
}

.product-discount {
  position: absolute;
  top: 12px;
  right: 12px;
  background: #ff4757;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  z-index: 1;
}

.product-info {
  padding: 20px 15px;
}

.rating {
  color: #ffc107;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.star {
  font-size: 18px;
  color: #ddd;
  position: relative;
}

.star.filled {
  color: #FFD700;
}

.star.half::before {
  content: '★';
  position: absolute;
  left: 0;
  width: 50%;
  overflow: hidden;
  color: #FFD700;
}

.rating-text {
  font-size: 15px;
  color: #666;
  margin-left: 3px !important;
}

.product-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #000;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-price {
  font-size: 18px;
  font-weight: bold;
  color: #000;
}

.product-price .old-price {
  color: #999;
  text-decoration: line-through;
  font-weight: normal;
  margin-left: 10px;
  font-size: 14px;
}

/* Pagination */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  gap: 20px;
}

.pagination-left,
.pagination-right {
  flex: 1;
}

.pagination-center {
  display: flex;
  align-items: center;
  gap: 8px;
}

.items-per-page {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.items-select {
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
}

.pagination-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: white;
  color: #666;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 16px;
}

.pagination-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #212529 0%, #000000 100%) !important;
  border-color: #212529 !important;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-numbers {
  display: flex;
  gap: 4px;
}

.pagination-number {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: white;
  color: #666;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
}

.pagination-number:hover {
  border-color: #212529 !important;
}

.pagination-number.active {
  background: linear-gradient(135deg, #212529 0%, #000000 100%) !important;
  border-color: #212529 !important;
  color: white;
}

.pagination-ellipsis {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 14px;
}

.pagination-info {
  font-size: 14px;
  color: #666;
  text-align: right;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .mobile-filter-toggle {
    display: block;
  }

  .mobile-filters-overlay {
    display: block;
  }

  .sidebar:not(.mobile-sidebar) {
    display: none;
  }

  .container {
    padding: 16px;
  }

  .main-content {
    gap: 16px;
  }

  .section-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .section-meta {
    width: 100%;
    justify-content: space-between;
    gap: 12px;
  }

  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }

  .product-image {
    height: 200px;
  }

  .product-info {
    padding: 12px;
  }

  .product-name {
    font-size: 14px;
  }

  .current-price {
    font-size: 14px;
  }

  .color-grid {
    grid-template-columns: repeat(8, 1fr);
  }

  .color-swatch {
    width: 28px;
    height: 28px;
  }

  .size-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .pagination {
    flex-direction: column;
    gap: 16px;
  }

  .pagination-left,
  .pagination-right {
    width: 100%;
  }

  .pagination-info {
    text-align: center;
  }

  .pagination-center {
    justify-content: center;
  }

  .pagination-numbers {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .product-image {
    height: 160px;
  }

  .product-info {
    padding: 8px;
  }

  .product-name {
    font-size: 13px;
  }

  .current-price {
    font-size: 13px;
  }

  .color-grid {
    grid-template-columns: repeat(6, 1fr);
    gap: 6px;
  }

  .color-swatch {
    width: 24px;
    height: 24px;
  }

  .size-button {
    padding: 6px 8px;
    font-size: 12px;
  }
}
</style>