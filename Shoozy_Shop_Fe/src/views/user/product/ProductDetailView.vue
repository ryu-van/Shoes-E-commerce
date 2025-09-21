<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import {getAllProductsByCategory, getProductById} from "@/service/ProductApi.js";
import { useRouter } from 'vue-router'
import ReviewForm from '../review/ReviewForm.vue'
import { getReviewsByProductId } from '@/service/ReviewApi.js'
import cartApi from "@/service/CartApi.js";
import ShowToastComponent from '@/components/ShowToastComponent.vue';

const router = useRouter()

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
})

const goToProductDetail = (id) => {
  router.push(`/product-detail/${id}`)
}

const selectedImageIndex = ref(0)
const selectedColor = ref(null)
const selectedSize = ref(null)
const quantity = ref(1)
const activeTab = ref('Thông tin chi tiết sản phẩm')
const sortBy = ref('latest')

const product = ref(null)

// Responsive Recommendation
const currentRecommendationIndex = ref(0)
const screenWidth = ref(window.innerWidth)
const itemsPerView = computed(() => {
  if (screenWidth.value <= 576) return 1      // Mobile: 1 item
  if (screenWidth.value <= 768) return 2      // Tablet: 2 items
  if (screenWidth.value <= 1200) return 3     // Desktop small: 3 items
  return 4                                     // Desktop large: 4 items
})

const maxRecommendationIndex = computed(() => {
  return Math.max(0, recommendations.value.length - itemsPerView.value)
})

const canGoNext = computed(() => {
  return currentRecommendationIndex.value < maxRecommendationIndex.value
})

const canGoPrev = computed(() => {
  return currentRecommendationIndex.value > 0
})

const visibleRecommendations = computed(() => {
  const start = currentRecommendationIndex.value
  const end = start + itemsPerView.value
  return recommendations.value.slice(start, end)
})

// Tabs
const tabs = ref(['Thông tin chi tiết sản phẩm', 'Đánh giá', 'Các câu hỏi thường gặp'])

// Reviews
const reviews = ref([])

const recommendations = ref([])

const availableColors = computed(() => {
  if (!product.value) return []
  const colorMap = new Map()
  product.value.variants.forEach(variant => {
    if (!colorMap.has(variant.color.id)) {
      colorMap.set(variant.color.id, variant.color)
    }
  })
  return Array.from(colorMap.values())
})

const availableSizes = computed(() => {
  if (!product.value || !selectedColor.value) return []
  return product.value.variants
      .filter(variant => variant.color.id === selectedColor.value.id)
      .map(variant => variant.size)
})

const currentPrice = computed(() => {
  if (!product.value || !selectedColor.value || !selectedSize.value) {
    return `${product.value?.minPrice?.toLocaleString()} - ${product.value?.maxPrice?.toLocaleString()}`
  }
  const variant = product.value.variants.find(v =>
      v.color.id === selectedColor.value.id && v.size.id === selectedSize.value.id
  )
  return variant ? variant.sellPrice.toLocaleString() : ''
})

// Discount calculations
const discountPercentage = computed(() => {
  if (selectedColor.value && selectedSize.value) {
    // Khi đã chọn màu và size cụ thể, lấy customValue từ variant
    const variant = product.value.variants.find(v =>
        v.color.id === selectedColor.value.id && v.size.id === selectedSize.value.id
    )
    if (variant && variant.promotions && variant.promotions.length > 0) {
      // Lấy promotion có giá trị cao nhất
      const maxDiscount = Math.max(...variant.promotions.map(p => p.customValue || p.originalValue || 0))
      return maxDiscount > 0 ? maxDiscount : null
    }
  } else {
    // Khi chưa chọn size/màu cụ thể, lấy value chung từ active_promotions
    if (product.value.active_promotions && product.value.active_promotions.length > 0) {
      const maxDiscount = Math.max(...product.value.active_promotions.map(p => p.value || 0))
      return maxDiscount > 0 ? maxDiscount : null
    }
  }
  return null
})

const discountedPrice = computed(() => {
  if (!discountPercentage.value) return null

  if (selectedColor.value && selectedSize.value) {
    const variant = product.value.variants.find(v =>
        v.color.id === selectedColor.value.id && v.size.id === selectedSize.value.id
    )
    if (variant) {
      const originalPrice = variant.sellPrice
      const discountAmount = (originalPrice * discountPercentage.value) / 100
      return originalPrice - discountAmount
    }
  } else {
    // Tính giá sau giảm cho khoảng giá
    const minDiscountAmount = (product.value.minPrice * discountPercentage.value) / 100
    const maxDiscountAmount = (product.value.maxPrice * discountPercentage.value) / 100
    return {
      min: product.value.minPrice - minDiscountAmount,
      max: product.value.maxPrice - maxDiscountAmount
    }
  }
  return null
})

const displayPrice = computed(() => {
  if (!discountedPrice.value) {
    // Không có giảm giá
    if (selectedColor.value && selectedSize.value) {
      return currentPrice.value + ' VNĐ'
    } else {
      return `${product.value?.minPrice?.toLocaleString()} - ${product.value?.maxPrice?.toLocaleString()} VNĐ`
    }
  } else {
    // Có giảm giá
    if (selectedColor.value && selectedSize.value) {
      return discountedPrice.value.toLocaleString() + ' VNĐ'
    } else {
      return `${discountedPrice.value.min.toLocaleString()} - ${discountedPrice.value.max.toLocaleString()} VNĐ`
    }
  }
})

const originalPrice = computed(() => {
  if (!discountedPrice.value) return null

  if (selectedColor.value && selectedSize.value) {
    return currentPrice.value + ' VNĐ'
  } else {
    return `${product.value?.minPrice?.toLocaleString()} - ${product.value?.maxPrice?.toLocaleString()} VNĐ`
  }
})

const currentImages = computed(() => {
  if (!product.value || !selectedColor.value) {
    return product.value?.variants[0]?.images || []
  }
  const variant = product.value.variants.find(v => v.color.id === selectedColor.value.id)
  return variant ? variant.images : []
})

const increaseQuantity = () => {
  if (currentVariantQuantity.value === null || quantity.value < currentVariantQuantity.value) {
    quantity.value++
  }
}

const decreaseQuantity = () => {
  if (quantity.value > 1) {
    quantity.value--
  }
}

const selectColor = (color) => {
  selectedColor.value = color
  selectedSize.value = null
  selectedImageIndex.value = 0
}

const selectSize = (size) => {
  selectedSize.value = size
}

const getMaterialInfo = () => {
  if (product.value && product.value.material) {
    const material = product.value.material;
    return `${material.name} - ${material.description}`;
  }
  return 'Chưa có thông tin';
}

const getSizeRange = () => {
  if (product.value.variants && product.value.variants.length > 0) {
    const sizes = product.value.variants.map(variant => variant.size.value)
    const uniqueSizes = [...new Set(sizes)]
    return uniqueSizes.join(', ')
  }
  return 'Chưa có thông tin'
}

const getColorOptions = () => {
  if (product.value.variants && product.value.variants.length > 0) {
    const colors = product.value.variants.map(variant => variant.color.name)
    const uniqueColors = [...new Set(colors)]
    return uniqueColors.join(', ')
  }
  return 'Chưa có thông tin'
}

const nextRecommendations = () => {
  if (canGoNext.value) {
    currentRecommendationIndex.value++
  }
}

const prevRecommendations = () => {
  if (canGoPrev.value) {
    currentRecommendationIndex.value--
  }
}

const getDisplayedStars = (rating) => {
  const fullStars = Math.floor(rating)
  const hasHalfStar = rating - fullStars >= 0.5 && rating - fullStars < 1
  return { fullStars, hasHalfStar }
}

const loadRecommendations = async (param) => {
  try {
    const res = await getAllProductsByCategory({
      categoryId: param.categoryId,
      productId: param.productId
    })
    recommendations.value = res.data.data
  } catch (error) {
    console.error('Error loading recommendations:', error)
    recommendations.value = []
  }
}

const fetchProductById = async (id) => {
  try {
    const res = await getProductById(id)
    console.log('Product data:', res)
    product.value = res.data.data || res
    await loadRecommendations({
      categoryId: product.value.category.id,
      productId: product.value.id
    })
    selectedColor.value = null
    selectedSize.value = null
    selectedImageIndex.value = 0
    // Lấy review trực tiếp từ product
    reviews.value = product.value.reviews || []
  } catch (error) {
    console.error('Error when loading product data:', error)
  }
}
// Toast component ref
const toastRef = ref(null)
const isAddingToCart = ref(false)
const currentVariantQuantity = computed(() => {
  if (!product.value || !selectedColor.value || !selectedSize.value) {
    return null
  }
  const variant = product.value.variants.find(v =>
      v.color.id === selectedColor.value.id && v.size.id === selectedSize.value.id
  )
  return variant ? variant.quantity : 0
})

// Thêm computed để kiểm tra còn hàng hay không
const isInStock = computed(() => {
  return currentVariantQuantity.value === null || currentVariantQuantity.value > 0
})

// Thêm hàm để lấy số lượng của variant cụ thể
const getVariantQuantity = (colorId, sizeId) => {
  if (!product.value) return 0
  const variant = product.value.variants.find(v =>
      v.color.id === colorId && v.size.id === sizeId
  )
  return variant ? variant.quantity : 0
}

// Cập nhật hàm addItemToCart để kiểm tra số lượng
const addItemToCart = async () => {
  if (!selectedColor.value || !selectedSize.value) {
    toastRef.value?.showToast('Vui lòng chọn màu sắc và kích thước trước khi thêm vào giỏ hàng', 'warning')
    return
  }

  if (!isInStock.value) {
    toastRef.value?.showToast('Sản phẩm đã hết hàng', 'error')
    return
  }

  if (quantity.value > currentVariantQuantity.value) {
    toastRef.value?.showToast(`Chỉ còn ${currentVariantQuantity.value} sản phẩm`, 'error')
    return
  }

  try {
    const variant = product.value.variants.find(v =>
        v.color.id === selectedColor.value.id && v.size.id === selectedSize.value.id
    )
    if (!variant) {
      toastRef.value?.showToast('Biến thể sản phẩm không tìm thấy', 'error')
      return
    }

    // Lấy userId từ localStorage
    const user = JSON.parse(localStorage.getItem("user"));
    const userId = user?.id;
    if (!userId) {
      toastRef.value?.showToast('Bạn cần đăng nhập trước khi thêm vào giỏ hàng', 'error')
      return;
    }

    isAddingToCart.value = true

    await cartApi.addToCart({
      userId: userId,
      productVariantId: variant.id,
      quantity: quantity.value
    })

    toastRef.value?.showToast('Thêm vào giỏ hàng thành công!', 'success')

    // Reset quantity after successful add
    quantity.value = 1

  } catch (error) {
    console.error('Error adding product to cart:', error)
    toastRef.value?.showToast('Không thể thêm vào giỏ hàng. Vui lòng thử lại', 'error')
  } finally {
    isAddingToCart.value = false
  }
}

const sortedReviews = computed(() => {
  if (sortBy.value === 'latest') {
    return [...reviews.value].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  }
  if (sortBy.value === 'oldest') {
    return [...reviews.value].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
  }
  if (sortBy.value === 'rating') {
    return [...reviews.value].sort((a, b) => b.rating - a.rating)
  }
  return reviews.value
})
// hiển thị form tạo bình luậnluận
const showReviewForm = ref(false)

// ProductDetailView.vue
const handleSubmitReview = async () => {
  showReviewForm.value = false;
  try {
    console.log('product.value.id:', product.value?.id);
    // Gọi lại API để lấy danh sách bình luận mới nhất
    const res = await getReviewsByProductId(product.value.id);
    reviews.value = res.data;
    // Tuỳ chọn: Cập nhật product để lấy averageRating mới
    const productResponse = await getProductById(props.id);
    product.value = productResponse.data;
  } catch (error) {
    console.error('Lỗi khi xử lý bình luận:', error, error.response || error.message);
    alert('Không thể hiển thị bình luận. Vui lòng thử lại.');
  }
};

const reviewsToShow = ref(4);
const visibleReviews = computed(() => sortedReviews.value.slice(0, reviewsToShow.value));

onMounted(() => {
  fetchProductById(props.id)

  // Add resize listener
  const handleResize = () => {
    screenWidth.value = window.innerWidth
    // Reset index nếu vượt quá giới hạn mới
    if (currentRecommendationIndex.value > maxRecommendationIndex.value) {
      currentRecommendationIndex.value = maxRecommendationIndex.value
    }
  }

  window.addEventListener('resize', handleResize)

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
  })
})

watch(() => props.id, (newId) => {
  fetchProductById(newId)
})

// Hàm xử lý content của reply nếu là chuỗi JSON
function parseReplyContent(content) {
  try {
    if (typeof content === 'string' && content.startsWith('"') && content.endsWith('"')) {
      return JSON.parse(content);
    }
    return content;
  } catch {
    return content;
  }
}

</script>

<template>
  <div class="product-page" v-if="product">
    <!-- Product Section -->
    <div class="product-section">
      <!-- Product Images -->
      <div class="product-images">
        <div class="image-wrapper">
          <!-- Thumbnail List -->
          <div class="thumbnail-list">
            <div
                v-for="(image, index) in currentImages"
                :key="image.id"
                class="thumbnail"
                :class="{ active: selectedImageIndex === index }"
                @click="selectedImageIndex = index"
            >
              <img :src="image.url" :alt="`Product view ${index + 1}`" />
            </div>
          </div>
          <!-- Main Image -->
          <div class="main-image">
            <img
                :src="currentImages[selectedImageIndex]?.url || product.thumbnail || 'http://localhost:9000/product-variant-images/10969556-0fcd-43ff-b7ee-3af88423d16d_488px-No-Image-Placeholder.svg_.png' "
                :alt="product.name"
            />
          </div>
        </div>
      </div>

      <!-- Product Info -->
      <div class="product-info">
        <h1 class="product-title">{{ product.name }}</h1>

        <!-- Rating -->
        <div class="rating">
                  <span v-for="n in 5" :key="n" class="star" :class="{
                    filled: n <= getDisplayedStars(product.averageRating || 0).fullStars,
                    half: n === getDisplayedStars(product.averageRating || 0).fullStars + 1 &&
                          getDisplayedStars(product.averageRating || 0).hasHalfStar
                  }">★</span>
          <span class="rating-text">{{ ( Number(product.averageRating).toFixed(1) || 0) }}/5</span>
        </div>

        <!-- Price with Discount Badge -->
        <div class="price-section">
          <div class="price">
            <span class="current-price">{{ displayPrice }}</span>
            <span v-if="discountPercentage" class="discount-badge">-{{ discountPercentage }}%</span>
          </div>

          <!-- Original price when there's a discount -->
          <div v-if="originalPrice" class="original-price-section">
            <span class="original-price">{{ originalPrice }}</span>
          </div>
        </div>

        <!-- Description -->
        <p class="description">{{ product.description }}</p>

        <!-- Color Selection -->
        <div class="option-group">
          <label>Chọn màu</label>
          <div class="color-options">
            <div
                v-for="color in availableColors"
                :key="color.id"
                class="color-option"
                :class="{ active: selectedColor?.id === color.id }"
                :style="{ backgroundColor: color.hexCode }"
                @click="selectColor(color)"
                :title="color.name"
            ></div>
          </div>
        </div>

        <!-- Size Selection -->
        <div class="option-group" v-if="selectedColor && availableSizes.length > 0">
          <label>Chọn size</label>
          <div class="size-options">
            <button
                v-for="size in availableSizes"
                :key="size.id"
                class="size-option"
                :class="{
          active: selectedSize?.id === size.id,
          'out-of-stock': getVariantQuantity(selectedColor.id, size.id) === 0
        }"
                @click="selectSize(size)"
                :disabled="getVariantQuantity(selectedColor.id, size.id) === 0"
            >
              {{ size.value }}
            </button>
          </div>

          <!-- Hiển thị thông tin số lượng khi đã chọn size -->
          <div v-if="selectedSize" class="stock-info-card">
            <div class="stock-content">
              <span class="stock-icon">📦</span>
              <div class="stock-text">
                <span class="stock-label">Số lượng còn lại:</span>
                <span class="stock-quantity" :class="{
          'low-stock': currentVariantQuantity <= 5,
          'out-of-stock': currentVariantQuantity === 0
        }">
          {{ currentVariantQuantity }} đôi
        </span>
              </div>
            </div>
            <div v-if="currentVariantQuantity <= 5 && currentVariantQuantity > 0" class="stock-warning">
              ⚠️ Chỉ còn ít hàng
            </div>
          </div>
        </div>

        <!-- Quantity and Add to Cart -->
        <div class="purchase-section">
          <div class="quantity-selector">
            <button @click="decreaseQuantity" :disabled="quantity <= 1">-</button>
            <span class="quantity">{{ quantity }}</span>
            <button @click="increaseQuantity">+</button>
          </div>
          <button
              class="add-to-cart-btn"
              :class="{ 'loading': isAddingToCart }"
              @click="addItemToCart"
              :disabled="isAddingToCart"
          >
            <span v-if="!isAddingToCart">Thêm vào giỏ hàng</span>
            <span v-else class="loading-text">
              <span class="spinner"></span>
              Đang thêm...
            </span>
          </button>
        </div>
      </div>
    </div>

    <!-- Navigation Tabs -->
    <div class="tabs">
      <button
          v-for="tab in tabs" :key="tab" class="tab"
          :class="{ active: activeTab === tab }"
          @click="activeTab = tab"
      >
        {{ tab }}
      </button>
    </div>

    <div v-if="activeTab === 'Thông tin chi tiết sản phẩm'" class="product-description-section">
      <div>
        <h5 class="fw-bold mb-3">Mô tả chi tiết</h5>
        <p>{{ product.description }}</p>

        <!-- Thông số sản phẩm -->
        <h5 class="fw-bold mt-4 mb-3">Thông số sản phẩm</h5>
        <div class="specifications-grid">
          <div class="spec-row">
            <span class="spec-label">Thương hiệu:</span>
            <span class="spec-value">{{ product.brand.name }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">Model:</span>
            <span class="spec-value">{{ product.name }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">Danh mục:</span>
            <span class="spec-value">{{ product.category.name }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">Chất liệu:</span>
            <span class="spec-value">{{ getMaterialInfo() }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">Cân nặng:</span>
            <span class="spec-value">{{ product.weight }} kg</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">Kích thước:</span>
            <span class="spec-value">{{ getSizeRange() }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">Màu sắc:</span>
            <span class="spec-value">{{ getColorOptions() }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">Xuất xứ:</span>
            <span class="spec-value">{{ product.brand.country }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">Loại giày:</span>
            <span class="spec-value">{{ product.category.description }}</span>
          </div>

        </div>
      </div>
    </div>

    <!-- Reviews Section -->
    <div v-if="activeTab === 'Đánh giá'" class="reviews-section">
      <div class="reviews-header">
        <h3>Tất cả các đánh giá ({{ reviews.length }})</h3>
        <div class="reviews-controls">
          <select v-model="sortBy" class="sort-select">
            <option value="latest">Mới nhất</option>
            <option value="oldest">Cũ nhất</option>
            <option value="rating">Đánh giá</option>
          </select>
        </div>
      </div>
      <ReviewForm v-if="showReviewForm" :productId="product.id" @submitReview="handleSubmitReview" />
      <div class="reviews-list">
        <div v-for="review in visibleReviews" :key="review.id" class="review-card">
          <div class="review-header">
            <div class="stars">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= review.rating }">★</span>
            </div>
            <button class="review-menu">⋯</button>
          </div>
          <div class="reviewer-info">
            <span class="reviewer-name">{{ review.userFullname || 'Ẩn danh' }}</span>
            <span class="verified">✓</span>
          </div>
          <p class="review-text">{{ review.content }}</p>
          <div class="review-date">Đăng ngày {{ new Date(review.createdAt).toLocaleDateString('vi-VN') }}</div>

          <!-- Hiển thị replies nếu có -->
          <div v-if="review.replies && review.replies.length" class="review-replies ms-3">
            <div v-for="reply in review.replies" :key="reply.id" class="reply-item">
              <div class="d-flex align-items-center gap-2 mb-1">
                <strong class="text-primary">ShoozyShop</strong>
                <small class="text-muted">{{ reply.createdAt ? new Date(reply.createdAt).toLocaleDateString('vi-VN') : '' }}</small>
              </div>
              <p class="mb-1">{{ parseReplyContent(reply.content) }}</p>
            </div>
          </div>
        </div>
        <button v-if="reviewsToShow < sortedReviews.length" class="load-more-btn" @click="reviewsToShow += 4">
          Load more review
        </button>
        <button v-if="reviewsToShow >= sortedReviews.length && sortedReviews.length > 4" class="load-more-btn" @click="reviewsToShow = 4">
          Thu gọn
        </button>
      </div>
    </div>

    <!-- You Might Also Like -->
    <div class="recommendations" v-if="recommendations.length > 0">
      <h2>BẠN CŨNG CÓ THỂ THÍCH</h2>
      <div class="recommendations-carousel">
        <button class="carousel-btn carousel-btn-prev" @click="prevRecommendations" :disabled="!canGoPrev">&#8249;</button>

        <div class="carousel-container">
          <div class="product-grid-carousel">
            <div v-for="item in visibleRecommendations" :key="item.id" class="recommendation-card" @click="goToProductDetail(item.id)">
              <div class="recommendation-image">
                <img :src="item.variants[0]?.thumbnail || 'http://localhost:9000/product-variant-images/10969556-0fcd-43ff-b7ee-3af88423d16d_488px-No-Image-Placeholder.svg_.png'" :alt="item.name" />
              </div>
              <h4 class="recommendation-name">{{ item.name }}</h4>
              <div class="recommendation-rating">
                <div class="rating">
                  <span v-for="n in 5" :key="n" class="star" :class="{
                    filled: n <= getDisplayedStars(item.averageRating || 0).fullStars,
                    half: n === getDisplayedStars(item.averageRating || 0).fullStars + 1 &&
                          getDisplayedStars(item.averageRating || 0).hasHalfStar
                  }">★</span>
                  <span class="rating-text">{{ ( Number(item.averageRating).toFixed(1) || 0) }}/5</span>
                </div>
                <span class="rating-count">{{ item.reviewCount }}</span>
              </div>
              <div class="recommendation-price">
                <span class="current-price">{{ Number(item.minPrice || 0).toLocaleString() }}đ</span>
              </div>
            </div>
          </div>
        </div>

        <button class="carousel-btn carousel-btn-next" @click="nextRecommendations" :disabled="!canGoNext">&#8250;</button>

        <div class="carousel-indicators" v-if="recommendations.length > itemsPerView">
          <div class="indicator-container">
            <div
                v-for="index in maxRecommendationIndex + 1"
                :key="index - 1"
                class="indicator"
                :class="{ active: currentRecommendationIndex === index - 1 }"
                @click="currentRecommendationIndex = index - 1"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Toast Component -->
  <ShowToastComponent ref="toastRef" />
</template>

<style scoped>
.product-page {
  max-width: 1350px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Nunito', sans-serif;
  margin-top: 40px;
}

/* Product Section */
.product-section {
  display: flex;
  gap: 30px;
  margin-bottom: 60px;
  align-items: stretch;
}

/* Product Images */
.product-images {
  flex: 1;
}

.image-wrapper {
  display: flex;
  gap: 20px;
  height: 100%;
}

.thumbnail-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 120px;
  height: 450px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #ccc transparent;
  margin-right: -10px;
  flex-shrink: 0;
}

.thumbnail {
  width: 100px;
  height: 100px;
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s;
  background-color: #f0f0f0;
  flex-shrink: 0;
}

.thumbnail-list::-webkit-scrollbar {
  width: 6px;
}

.thumbnail-list::-webkit-scrollbar-track {
  background: transparent;
}

.thumbnail-list::-webkit-scrollbar-thumb {
  background-color: #ccc;
  border-radius: 3px;
}

.thumbnail-list::-webkit-scrollbar-thumb:hover {
  background-color: #999;
}

.main-image {
  flex: 1;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-color: #f0f0f0;
  height: 450px;
  min-width: 0; /* Allow shrinking */
}

.main-image img {
  max-height: 100%;
  max-width: 100%;
  object-fit: cover;
}

.thumbnail {
  width: 100px;
  height: 100px;
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s;
  background-color: #f0f0f0;
  flex-shrink: 0;
}

.thumbnail.active {
  border-color: #000;
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Product Info */
.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-top: -10px;
}

.product-title {
  font-size: 46px;
  font-family: 'Montserrat', sans-serif !important;
  font-weight: 900;
  margin: 0 0 10px 0;
  text-transform: uppercase;
}

.rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.stars {
  display: flex;
  gap: 2px;
}

.star {
  color: #ddd;
  font-size: 18px;
}

.star.filled {
  color: #FFD700;
}

.stars.small .star {
  font-size: 14px;
}

.rating-count {
  color: #666;
  font-size: 14px;
}

/* Price Section with Discount */
.price-section {
  margin-bottom: 20px;
}

.price {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 5px;
  flex-wrap: wrap;
}

.current-price {
  font-size: 28px;
  font-weight: bold;
  color: #000;
}

.discount-badge {
  background: #ff4444;
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: bold;
  animation: pulse-badge 2s infinite;
}

@keyframes pulse-badge {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

.original-price-section {
  margin-top: 5px;
}

.original-price {
  font-size: 18px;
  text-decoration: line-through;
  color: #999;
  font-weight: 500;
}

.price-range {
  font-size: 16px;
  color: #666;
  font-weight: normal;
}

.description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 24px;
}

/* Options */
.option-group {
  margin-bottom: 16px;
}

.option-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.color-options {
  display: flex;
  gap: 12px;
}

.color-option {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid gray;
  cursor: pointer;
  transition: border-color 0.2s, transform 0.2s;
  position: relative;
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.active {
  border-color: #000;
  transform: scale(1.1);
}

.color-option.active::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-weight: bold;
  font-size: 16px;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.8);
}

.size-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.size-option {
  padding: 12px 20px;
  border: 1px solid #ddd;
  background: #f9f9f9;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  min-width: 50px;
  text-align: center;
}

.size-option:hover {
  border-color: #999;
}

.size-option.active {
  background: #000;
  color: white;
  border-color: #000;
}

/* Purchase Section */
.purchase-section {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-top: 5px;
}

.quantity-selector {
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 24px;
  overflow: hidden;
}

.quantity-selector button {
  width: 40px;
  height: 40px;
  border: none;
  background: #f9f9f9;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.quantity-selector button:hover:not(:disabled) {
  background: #e9e9e9;
}

.quantity-selector button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quantity {
  width: 60px;
  text-align: center;
  font-weight: 500;
}

.add-to-cart-btn {
  flex: 1;
  background: #000;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.add-to-cart-btn:hover:not(:disabled) {
  background: #333;
}

.add-to-cart-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.add-to-cart-btn.loading {
  background: #666;
  cursor: not-allowed;
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Tabs */
.tabs {
  display: flex;
  border-bottom: 1px solid #ddd;
  margin-bottom: 40px;
}

.tab {
  padding: 16px 24px;
  border: none;
  background: none;
  font-size: 16px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: border-color 0.2s;
}

.tab:hover {
  background: #f9f9f9;
}

.tab.active {
  border-bottom-color: #000;
  font-weight: 500;
}

/* Product Description Section */
.product-description-section {
  font-size: 1rem;
  color: #212529;
  line-height: 1.7;
  margin-bottom: 100px;
}

.product-description-section h5 {
  font-size: 1.25rem !important;
  font-weight: 700 !important;
  margin-bottom: 1rem !important;;
}

.product-description-section h6 {
  font-size: 1.1rem !important;;
  font-weight: 700 !important;;
  margin-top: 2rem !important;;
  margin-bottom: 1rem !important;;
}

.product-description-section p {
  margin-bottom: 1rem !important;
  color: #212529 !important;
}

.product-description-section ul {
  padding-left: 0;
  list-style: none !important;
  margin: 0;
}

.product-description-section li {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px !important;
  font-weight: 500 !important;
}

.product-description-section li i {
  color: #28a745 !important;
  font-size: 1rem !important;
}

.specifications-grid {
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 1.5rem;
}

.spec-row {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #e9ecef;
}

.spec-row:last-child {
  border-bottom: none;
}

.spec-row:nth-child(even) {
  background: #ffffff;
}

.spec-label {
  font-weight: 600;
  min-width: 150px;
  color: #495057;
}

.spec-value {
  color: #212529;
  flex: 1;
}

/* Reviews Section */
.reviews-section {
  margin-bottom: 60px;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  flex-wrap: wrap;
  gap: 16px;
}

.reviews-header h3 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.reviews-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}

.sort-select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  font-size: 14px;
}

.write-review-btn {
  padding: 8px 16px;
  background: #000;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.review-card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 24px;
  background: white;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.review-menu {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #666;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.reviewer-name {
  font-weight: 600;
  font-size: 16px;
}

.verified {
  color: #28a745;
  font-size: 14px;
}

.review-text {
  color: #444;
  line-height: 1.6;
  margin-bottom: 12px;
  font-style: italic;
}

.review-date {
  color: #666;
  font-size: 14px;
}

.btn-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.load-more-btn {
  padding: 12px 32px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 24px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.2s;
}

.load-more-btn:hover {
  background: #f9f9f9;
  border-color: #999;
}

/* Recommendations Section */
.recommendations {
  margin-bottom: 60px;
  padding: 40px 0;
}

.recommendations h2 {
  text-align: center;
  font-size: 32px;
  font-weight: 700;
  font-family: 'Poppins', sans-serif;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 40px;
  color: #000;
}

.recommendations-carousel {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.carousel-btn {
  background: #000;
  color: white;
  border: none;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 24px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  flex-shrink: 0;
  z-index: 2;
}

.carousel-btn:hover:not(:disabled) {
  background: #333;
  transform: scale(1.1);
}

.carousel-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
}

.carousel-btn-prev {
  order: -1;
}

.carousel-btn-next {
  order: 1;
}

.carousel-container {
  flex: 1;
  overflow: hidden;
  padding: 75px 10px;
}

.product-grid-carousel {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  transition: transform 0.3s ease;
  width: 100%;
}

.recommendation-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  cursor: pointer;
}

.recommendation-card:hover {
  transform: translateY(-2px);
}

.recommendation-image {
  width: 100%;
  height: 270px;
  overflow: hidden;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recommendation-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.recommendation-name {
  font-size: 18px;
  font-weight: bold;
  color: #000;
  margin: 16px 16px 8px 16px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recommendation-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 16px 10px 16px;
}

.recommendation-price {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: -10px 16px 10px 16px !important;
  flex-wrap: wrap;
  font-size: 18px;
  font-weight: bold;
  color: #000;
}

.recommendation-price .current-price {
  font-size: 18px;
  font-weight: 700;
  color: #000;
}

.recommendation-price {
  font-size: 18px;
  font-weight: bold;
  color: #000;
}

.recommendation-price .discount {
  background: #ff4444;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

/* Carousel Indicators */
.carousel-indicators {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.indicator-container {
  display: flex;
  gap: 8px;
}

.indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ddd;
  cursor: pointer;
  transition: all 0.3s ease;
}

.indicator.active {
  background: #000;
  animation: pulse 1.5s infinite;
}

.indicator:hover {
  background: #666;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

/* Responsive Design */
@media (max-width: 1200px) {
  .product-grid-carousel {
    grid-template-columns: repeat(3, 1fr);
  }

  .carousel-btn {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .product-section {
    flex-direction: column;
    gap: 30px;
  }

  .image-wrapper {
    flex-direction: column;
    gap: 15px;
  }

  .thumbnail-list {
    flex-direction: row;
    width: 100%; /* Use full width */
    height: auto;
    overflow-x: auto;
    overflow-y: hidden;
    gap: 10px;
    padding: 10px 0;
    margin-right: 0;
    justify-content: flex-start;
    -webkit-overflow-scrolling: touch;
  }

  .thumbnail {
    width: calc((100% - 40px) / 5); /* Dynamic width for 5 thumbnails per row with gaps */
    min-width: 80px; /* Minimum size for usability */
    max-width: 120px; /* Maximum size to prevent oversizing */
    height: 110px;
    flex-shrink: 0;
    aspect-ratio: 1/1; /* Maintain square ratio */
  }

  .main-image {
    height: 350px;
    width: 100%;
  }

  /* Custom scrollbar cho horizontal scroll */
  .thumbnail-list::-webkit-scrollbar {
    height: 6px;
    width: auto;
  }

  .thumbnail-list::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  .thumbnail-list::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 3px;
  }

  .thumbnail-list::-webkit-scrollbar-thumb:hover {
    background: #999;
  }

  .product-title {
    font-size: 28px;
  }

  .current-price {
    font-size: 24px;
  }

  .purchase-section {
    flex-direction: row;
    align-items: center;
    gap: 12px;
  }

  .add-to-cart-btn {
    flex: 1;
    min-width: 120px;
  }

  .quantity-selector {
    flex-shrink: 0;
  }

  .product-grid-carousel {
    grid-template-columns: repeat(2, 1fr);
  }

  .carousel-container {
    padding: 20px 5px;
    margin-bottom: 30px;
  }

  .recommendation-image {
    height: 270px;
  }

  .recommendation-name {
    font-size: 16px;
  }

  .recommendation-price .current-price {
    font-size: 16px;
  }
}

@media (max-width: 576px) {
  .thumbnail {
    width: calc((100% - 32px) / 4); /* 4 thumbnails per row on small mobile */
    min-width: 70px;
    max-width: 110px;
    height: 100px;
  }

  .thumbnail-list {
    gap: 8px;
    padding: 8px 0;
  }

  .main-image {
    height: 300px;
  }

  .product-grid-carousel {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .recommendation-card {
    width: 100%;
    max-width: 300px;
    margin-bottom: 30px;
  }

  .recommendation-image {
    height: 270px;
  }

}

@media (max-width: 480px) {
  .thumbnail {
    width: calc((100% - 24px) / 3); /* 3 thumbnails per row on very small screens */
    min-width: 60px;
    max-width: 110px;
    height: 100px;
  }

  .thumbnail-list {
    gap: 6px;
    padding: 6px 0;
  }

  .main-image {
    height: 280px;
  }

  .purchase-section {
    gap: 10px;
  }

  .quantity-selector button {
    width: 35px;
    height: 35px;
    font-size: 16px;
  }

  .quantity {
    width: 50px;
  }

  .add-to-cart-btn {
    padding: 10px 16px;
    font-size: 14px;
  }

  .product-grid-carousel {
    grid-template-columns: 1fr; /* Chỉ hiển thị 1 cột */
  }

  .carousel-container {
    padding: 15px 5px;
  }

  .recommendation-card {
    max-width: 280px;
    height: auto;
    margin: 30px auto;
  }

  .recommendation-image {
    height: 270px;
  }

  .recommendation-name {
    font-size: 14px;
    margin: 12px 12px 6px 12px;
  }

  .recommendation-rating {
    margin: 0 12px 6px 12px;
  }

  .recommendation-price {
    margin: -15px 12px 12px 12px !important;
    font-size: 16px;
  }

  .carousel-btn {
    width: 35px;
    height: 35px;
    font-size: 18px;
  }

  .recommendations h2 {
    font-size: 24px;
    margin-bottom: 30px;
  }
}

.thumbnail-list {
  scroll-behavior: smooth;
}

.review-replies {
  border-left: 2px solid #eee;
  margin-left: 10px;
  padding-left: 10px;
  margin-top: 10px;
}
.reply-item {
  margin-bottom: 8px;
  font-size: 0.97em;
  color: #555;
  background: #f8f9fa;
  border-radius: 6px;
  padding: 8px 12px;
}
.reply-item:last-child {
  margin-bottom: 0;
}

/* Stock Info Card */
.stock-info-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: 1px solid #dee2e6;
  border-radius: 12px;
  padding: 16px;
  margin-top: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.stock-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stock-icon {
  font-size: 20px;
  opacity: 0.8;
}

.stock-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-label {
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}

.stock-quantity {
  font-size: 18px;
  font-weight: 700;
  transition: color 0.3s ease;
}

.stock-quantity.low-stock {
  color: #ffc107;
}

.stock-quantity.out-of-stock {
  color: #dc3545;
}

.stock-warning {
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.3);
  border-radius: 8px;
  font-size: 13px;
  color: #856404;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Update size option styles for out of stock */
.size-option.out-of-stock {
  background: #f8f9fa;
  color: #6c757d;
  border-color: #dee2e6;
  cursor: not-allowed;
  opacity: 0.6;
  position: relative;
}

.size-option.out-of-stock::after {
  content: 'Hết hàng';
  position: absolute;
  top: -8px;
  right: -8px;
  background: #dc3545;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: 500;
}

.size-option:disabled {
  pointer-events: none;
}

</style>