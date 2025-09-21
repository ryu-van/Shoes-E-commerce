<script setup>
import {onMounted, ref, computed} from "vue";
import {getAllBrands} from "@/service/BrandApi.js";
import {getAllProducts} from "@/service/ProductApi.js";
import {useRouter} from 'vue-router'

const router = useRouter()
const brands = ref([])
const newestProducts = ref([])
const topProducts = ref([])

const navigateTo = (path, query = {}) => {
  router.push({ path, query })
}

const fetchBrands = async () => {
  try {
    const response = await getAllBrands();
    brands.value = response.data.data
  } catch (error) {
    console.error('Lỗi lấy brands:', error)
  }
}

const getNewestProducts = async () => {
  try {
    const response = await getAllProducts({
      sortBy: 'created_at',
      sortDirection: 'desc',
      pageSize: 8, // Lấy nhiều hơn để có đủ sản phẩm sau khi lọc
      pageNo: 1
    });

    // Lọc chỉ lấy sản phẩm có giá > 0
    const validProducts = response.data.data.content.filter(product =>
        product.minPrice && product.minPrice > 0
    );

    // Chỉ lấy 4 sản phẩm đầu tiên sau khi lọc
    newestProducts.value = validProducts.slice(0, 4);
  } catch (error) {
    console.error('Error fetching newest products:', error);
  }
};

const getTopProducts = async () => {
  try {
    const response = await getAllProducts({
      sortBy: 'rating',
      sortDirection: 'desc',
      pageSize: 8, // Lấy nhiều hơn để có đủ sản phẩm sau khi lọc
      pageNo: 1
    });

    // Lọc chỉ lấy sản phẩm có giá > 0
    const validProducts = response.data.data.content.filter(product =>
        product.minPrice && product.minPrice > 0
    );

    // Chỉ lấy 4 sản phẩm đầu tiên sau khi lọc
    topProducts.value = validProducts.slice(0, 4);
  }catch (error) {
    console.error('Error fetching top products:', error);
  }
}

// Hàm tính tổng % giảm giá từ các promotions
const getTotalDiscountPercentage = (activePromotions) => {
  if (!activePromotions || activePromotions.length === 0) return 0;

  // Tính tổng % giảm giá từ tất cả promotions đang active
  const totalDiscount = activePromotions.reduce((total, promotion) => {
    return total + (promotion.value || 0);
  }, 0);

  return Math.min(totalDiscount, 100); // Giới hạn tối đa 100%
}

// Hàm tính giá sau khi giảm
const getDiscountedPrice = (originalPrice, discountPercentage) => {
  if (discountPercentage <= 0) return originalPrice;
  return originalPrice * (100 - discountPercentage) / 100;
}

// Hàm kiểm tra sản phẩm có hợp lệ không (có giá > 0)
const isValidProduct = (product) => {
  return product && product.minPrice && product.minPrice > 0;
}

const animateStars = () => {
  const stars = document.querySelectorAll('.decorative-star')
  stars.forEach(star => {
    const randomInterval = 2000 + Math.random() * 3000
    setInterval(() => {
      const rotate = Math.random() * 360
      const scale = 0.8 + Math.random() * 0.4
      star.style.transform = `rotate(${rotate}deg) scale(${scale})`
    }, randomInterval)
  })
}

const getDisplayedStars = (rating) => {
  const fullStars = Math.floor(rating)
  const hasHalfStar = rating - fullStars >= 0.5 && rating - fullStars < 1
  return {fullStars, hasHalfStar}
}

onMounted(() => {
  animateStars()
  // Fade-in effect for sections
  const sections = document.querySelectorAll('section')
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.style.opacity = '1'
        entry.target.style.transform = 'translateY(0)'
      }
    })
  })

  sections.forEach(section => {
    section.style.opacity = '0'
    section.style.transform = 'translateY(20px)'
    section.style.transition = 'all 0.6s ease'
    observer.observe(section)
  })

  // Hover effect for brand logos
  document.querySelectorAll('.brand-logo').forEach(logo => {
    logo.addEventListener('mouseenter', function () {
      this.style.transform = 'scale(1.1)'
      this.style.transition = 'transform 0.3s ease'
    })
    logo.addEventListener('mouseleave', function () {
      this.style.transform = 'scale(1)'
    })
  })

  fetchBrands();
  getNewestProducts();
  getTopProducts();
})

const duplicatedBrands = computed(() => {
  // Nhân đôi brands để tạo hiệu ứng seamless loop
  return [...brands.value, ...brands.value]
})

// Computed để lọc sản phẩm mới có giá hợp lệ
const validNewestProducts = computed(() => {
  return newestProducts.value.filter(product => isValidProduct(product));
})

// Computed để lọc sản phẩm nổi bật có giá hợp lệ
const validTopProducts = computed(() => {
  return topProducts.value.filter(product => isValidProduct(product));
})

</script>

<template>
  <div class="home-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-lg-6">
            <div class="hero-content">
              <h1>FIND SHOES<br>THAT MATCHES<br>YOUR STYLE</h1>
              <p>Khám phá bộ sưu tập giày đa dạng, được thiết kế tỉ mỉ để thể hiện cá tính và gu thẩm mỹ riêng biệt của
                bạn.</p>
              <router-link class="btn-shop-now" to="/products">Mua ngay</router-link>

              <div class="row text-center hero-stats">
                <div class="col-4">
                  <div class="stat-item">
                    <div class="stat-number">200+</div>
                    <div class="stat-label">Thương hiệu quốc tế</div>
                  </div>
                </div>
                <div class="col-4 border-start border-secondary border-opacity-25">
                  <div class="stat-item">
                    <div class="stat-number">2,000+</div>
                    <div class="stat-label">Sản phẩm chất lượng</div>
                  </div>
                </div>
                <div class="col-4 border-start border-secondary border-opacity-25">
                  <div class="stat-item">
                    <div class="stat-number">30,000+</div>
                    <div class="stat-label">Khách hàng hài lòng</div>
                  </div>
                </div>
              </div>

            </div>
          </div>
          <div class="col-lg-6">
            <div class="hero-image">
              <img src="@/assets/img/pexels-melvin-buezo-1253763-2529148-Photoroom.png" alt="Giày thời trang">
              <div class="decorative-star star-1">✦</div>
              <div class="decorative-star star-2">✦</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Brand Section -->
    <section class="brand-section">
      <div class="container">
        <div class="brand-logos">
          <div class="brand-track">
            <!-- Duplicate brands for seamless loop -->
            <div class="brand-logo" v-for="(brand, index) in duplicatedBrands" :key="`${brand.id}-${index}`">
              {{ brand.name.toUpperCase() }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- New Arrivals -->
    <section class="py-5" v-if="validNewestProducts.length > 0">
      <div class="container">
        <h2 class="section-title">HÀNG MỚI VỀ</h2>
        <div class="row">
          <div class="col-lg-3 col-md-6" v-for="newProduct in validNewestProducts" :key="newProduct.id">
            <div class="product-card" @click="navigateTo(`/product-detail/${newProduct.id}`)">
              <div class="product-image-container">
                <img :src="newProduct.thumbnail || 'http://localhost:9000/product-variant-images/10969556-0fcd-43ff-b7ee-3af88423d16d_488px-No-Image-Placeholder.svg_.png'" :alt="newProduct.name" class="product-image"/>

                <!-- Discount Badge -->
                <div v-if="getTotalDiscountPercentage(newProduct.active_promotions) > 0" class="discount-badge">
                  -{{ Math.round(getTotalDiscountPercentage(newProduct.active_promotions)) }}%
                </div>
              </div>

              <div class="product-info">
                <h5 class="product-title">{{ newProduct.name }}</h5>
                <div class="rating">
                  <span v-for="n in 5" :key="n" class="star" :class="{
                    filled: n <= getDisplayedStars(newProduct.averageRating || 0).fullStars,
                    half: n === getDisplayedStars(newProduct.averageRating || 0).fullStars + 1 &&
                          getDisplayedStars(newProduct.averageRating || 0).hasHalfStar
                  }">★</span>
                  <span class="rating-text">{{ ( Number(newProduct.averageRating).toFixed(1) || 0) }}/5</span>
                </div>

                <!-- Price with discount -->
                <div class="product-price">
                  <template v-if="getTotalDiscountPercentage(newProduct.active_promotions) > 0">
                    <span class="current-price">
                      {{ Number(getDiscountedPrice(newProduct.minPrice, getTotalDiscountPercentage(newProduct.active_promotions))).toLocaleString() }}đ
                    </span>
                    <span class="old-price">
                      {{ Number(newProduct.minPrice || 0).toLocaleString() }}đ
                    </span>
                  </template>
                  <template v-else>
                    <span class="current-price">
                      {{ Number(newProduct.minPrice || 0).toLocaleString() }}đ
                    </span>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="text-center mt-4">
          <button class="btn-rounded-outline"
                  @click="navigateTo('/products', { sortBy: 'created_at', sortDirection: 'desc' })">
            Xem tất cả
          </button>
        </div>
      </div>
    </section>

    <div class="section-separator"></div>

    <!-- Top Selling -->
    <section class="py-5 bg-white" v-if="validTopProducts.length > 0">
      <div class="container">
        <h2 class="section-title">SẢN PHẨM NỔI BẬT</h2>
        <div class="row">
          <div class="col-lg-3 col-md-6" v-for="topProduct in validTopProducts" :key="topProduct.id">
            <div class="product-card" @click="navigateTo(`/product-detail/${topProduct.id}`)">
              <div class="product-image-container">
                <img :src="topProduct.thumbnail || 'http://localhost:9000/product-variant-images/10969556-0fcd-43ff-b7ee-3af88423d16d_488px-No-Image-Placeholder.svg_.png'" :alt="topProduct.name" class="product-image"/>

                <!-- Discount Badge -->
                <div v-if="getTotalDiscountPercentage(topProduct.active_promotions) > 0" class="discount-badge">
                  -{{ Math.round(getTotalDiscountPercentage(topProduct.active_promotions)) }}%
                </div>
              </div>

              <div class="product-info">
                <h5 class="product-title">{{ topProduct.name }}</h5>
                <div class="rating">
                  <span v-for="n in 5" :key="n" class="star" :class="{
                    filled: n <= getDisplayedStars(topProduct.averageRating || 0).fullStars,
                    half: n === getDisplayedStars(topProduct.averageRating || 0).fullStars + 1 &&
                          getDisplayedStars(topProduct.averageRating || 0).hasHalfStar
                  }">★</span>
                  <span class="rating-text">{{ ( ( Number(topProduct.averageRating)).toFixed(1) || 0) }}/5</span>
                </div>

                <!-- Price with discount -->
                <div class="product-price">
                  <template v-if="getTotalDiscountPercentage(topProduct.active_promotions) > 0">
                    <span class="current-price">
                      {{ Number(getDiscountedPrice(topProduct.minPrice, getTotalDiscountPercentage(topProduct.active_promotions))).toLocaleString() }}đ
                    </span>
                    <span class="old-price">
                      {{ Number(topProduct.minPrice || 0).toLocaleString() }}đ
                    </span>
                  </template>
                  <template v-else>
                    <span class="current-price">
                      {{ Number(topProduct.minPrice || 0).toLocaleString() }}đ
                    </span>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="text-center mt-4">
          <button class="btn-rounded-outline"
                  @click="navigateTo('/products', { sortBy: 'rating', sortDirection: 'desc' })">
            Xem tất cả
          </button>
        </div>
      </div>
    </section>

    <!-- Browse by Style -->
    <section class="category-section">
      <div class="container">
        <h2 class="section-title">KHÁM PHÁ THEO PHONG CÁCH</h2>
        <div class="category-grid">
          <div class="category-card casual">
            <img src="@/assets/img/giay_thuong_ngay.jpg" alt="Phong cách thường ngày"
                 class="category-image">
            <div class="category-overlay">
              <h3 class="category-title">Thường ngày</h3>
            </div>
          </div>
          <div class="category-card formal">
            <img src="@/assets/img/giay-oxford-cho-bo.jpeg" alt="Phong cách trang trọng"
                 class="category-image">
            <div class="category-overlay">
              <h3 class="category-title">Trang trọng</h3>
            </div>
          </div>
          <div class="category-card party">
            <img src="@/assets/img/Giay-da-di-du-tiec-brogue.webp" alt="Phong cách tiệc tùng"
                 class="category-image">
            <div class="category-overlay">
              <h3 class="category-title">Dự tiệc</h3>
            </div>
          </div>
          <div class="category-card gym">
            <img src="@/assets/img/giay_the_thao_2.jpg" alt="Phong cách tập luyện"
                 class="category-image">
            <div class="category-overlay">
              <h3 class="category-title">Thể thao</h3>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Customer Reviews -->
    <section class="reviews-section">
      <div class="container">
        <h2 class="section-title">CẢM NHẬN CỦA KHÁCH HÀNG</h2>
        <div class="row">
          <div class="col-lg-4 col-md-6">
            <div class="review-card">
              <div class="review-rating">
                <i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i><i
                  class="fas fa-star"></i><i class="fas fa-star"></i>
              </div>
              <div class="review-text">
                "Tôi thực sự bất ngờ với chất lượng và phong cách của những đôi giày từ Shoozy Shop Từ sneaker thường
                ngày đến bốt sang trọng, tất cả đều vượt mong đợi!"
              </div>
              <div class="reviewer-name">Sarah M. <span class="verified-badge">✓ Đã xác thực</span></div>
            </div>
          </div>
          <div class="col-lg-4 col-md-6">
            <div class="review-card">
              <div class="review-rating">
                <i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i><i
                  class="fas fa-star"></i><i class="fas fa-star"></i>
              </div>
              <div class="review-text">
                "Tìm được đôi giày vừa thoải mái vừa thời trang từng là thách thức với tôi – cho đến khi tôi biết đến
                Shop.co. Giờ đây, đây là điểm đến yêu thích của tôi!"
              </div>
              <div class="reviewer-name">Alex K. <span class="verified-badge">✓ Đã xác thực</span></div>
            </div>
          </div>
          <div class="col-lg-4 col-md-6">
            <div class="review-card">
              <div class="review-rating">
                <i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i><i
                  class="fas fa-star"></i><i class="fas fa-star"></i>
              </div>
              <div class="review-text">
                "Là người luôn tìm kiếm những đôi giày độc đáo, tôi rất vui vì đã phát hiện ra Shop.co. Mẫu mã đa dạng
                và luôn bắt kịp xu hướng!"
              </div>
              <div class="reviewer-name">James L. <span class="verified-badge">✓ Đã xác thực</span></div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* Hero Section */
.hero-section {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 80px 0;
  position: relative;
}

.hero-content h1 {
  font-size: 4.5rem;
  font-family: 'Montserrat', sans-serif;
  font-weight: 900;
  line-height: 1.2;
  margin-bottom: 20px;
  color: #000;
}

.hero-content p {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
  max-width: 400px;
}

.btn-shop-now {
  background-color: #000;
  color: white;
  padding: 12px 40px;
  border-radius: 25px;
  text-decoration: none;
  font-weight: 500;
  border: none;
  transition: all 0.3s ease;
}

.btn-shop-now:hover {
  background-color: #333;
  color: white;
  transform: translateY(-2px);
}

.hero-stats {
  margin-top: 40px;
  margin-left: -50px;
}

.vertical-divider {
  width: 1px;
  height: 40px;
  background-color: #ccc;
  opacity: 0.6;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 2rem;
  font-weight: bold;
  color: #000;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.hero-image {
  position: relative;
}

.hero-image img {
  width: 100%;
  height: 500px;
  object-fit: cover;
  border-radius: 15px;
}

.decorative-star {
  position: absolute;
  font-size: 24px;
  color: #000;
}

.star-1 {
  top: 20%;
  right: 20%;
}

.star-2 {
  bottom: 30%;
  left: 10%;
}

/* Brand Section */
.brand-section {
  background-color: #000;
  padding: 30px 0;
  overflow: hidden;
  position: relative;
}

/* Gradient fade effect ở 2 bên */
.brand-section::before,
.brand-section::after {
  content: '';
  position: absolute;
  top: 0;
  width: 80px;
  height: 100%;
  z-index: 2;
  pointer-events: none;
}

.brand-section::before {
  left: 0;
  background: linear-gradient(to right, #000, transparent);
}

.brand-section::after {
  right: 0;
  background: linear-gradient(to left, #000, transparent);
}

.brand-logos {
  display: flex;
  width: 100%;
}

.brand-track {
  display: flex;
  align-items: center;
  animation: scroll 25s linear infinite;
  width: max-content;
}

.brand-logo {
  color: white;
  font-size: 18px;
  font-weight: bold;
  padding: 10px 20px;
  margin: 0 30px;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.brand-logo:hover {
  color: #ccc;
  transform: scale(1.1);
}

@keyframes scroll {
  0% {
    transform: translateX(0);
  }

  100% {
    transform: translateX(-50%);
  }
}

/* Tạm dừng khi hover */
.brand-track:hover {
  animation-play-state: paused;
}

/* Responsive */
@media (max-width: 768px) {
  .brand-logo {
    font-size: 16px;
    margin: 0 20px;
    padding: 8px 15px;
  }

  .brand-track {
    animation-duration: 20s;
  }

  .product-image {
    width: 100%;
    height: 300px;
    object-fit: cover;
    background-color: #f0f0f0;
  }
}

/* Product Sections */
.section-title {
  text-align: center;
  font-size: 2.5rem;
  font-family: 'Montserrat', sans-serif;
  font-weight: 900;
  margin-bottom: 40px;
  color: #000;
}

.product-card {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  transition: transform 0.3s ease;
  margin-bottom: 30px;
  border: 1px solid #f0f0f0;
}

.product-card:hover {
  transform: translateY(-2px);
}

/* Product Image Container with Badge */
.product-image-container {
  position: relative;
  overflow: hidden;
}

.product-image {
  width: 100%;
  height: 300px;
  object-fit: cover;
  background-color: #f0f0f0;
  transition: transform 0.3s ease;
}

.product-card:hover .product-image {
  transform: scale(1.05);
}

/* Discount Badge */
.discount-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #ff4757, #ff3742);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(255, 71, 87, 0.3);
  z-index: 2;
  animation: pulse 2s infinite;
}

@keyframes pulse {
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

.product-info {
  padding: 20px 15px;
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

/* Updated Price Styles */
.product-price {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.product-price .current-price {
  font-size: 18px;
  font-weight: bold;
  color: #000;
}

.product-price .old-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
  font-weight: normal;
}

.rating {
  color: #ffc107;
  margin-bottom: 10px;
}

.rating-text {
  font-size: 15px;
  color: #666;
  margin-left: 3px !important;
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

/* Button */
.btn-rounded-outline {
  border: 1px solid #ccc;
  border-radius: 999px;
  padding: 10px 60px;
  background-color: white;
  color: #333;
  font-weight: 600;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.btn-rounded-outline:hover {
  border-color: #999;
}

.section-separator {
  height: 1px;
  background-color: #e0e0e0;
  margin: 40px auto;
  width: 77%;
}

/* Category Section */
.category-section {
  padding: 80px 0;
}

.category-section .container {
  background-color: #f0f0f0;
  padding: 60px 0px;
  border-radius: 25px;
  max-width: 1300px;
}

.category-section .section-title {
  text-align: center;
  font-size: 3rem;
  margin-bottom: 60px;
  color: #000;
  letter-spacing: -1px;
}

.category-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 300px 300px;
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.category-card {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease;
  background: white;
}

.category-card:hover {
  transform: translateY(-2px);
}

.category-card.casual {
  grid-column: 1;
  grid-row: 1;
}

.category-card.formal {
  grid-column: 2;
  grid-row: 1;
}

.category-card.party {
  grid-column: 1;
  grid-row: 2;
}

.category-card.gym {
  grid-column: 2;
  grid-row: 2;
}

.category-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.1s ease;
}

.category-card:hover .category-image {
  transform: scale(1.05);
}

.category-overlay {
  position: absolute;
  top: 20px;
  left: 20px;
  background: rgba(255, 255, 255, 0.9);
  padding: 10px 20px;
  border-radius: 25px;
  backdrop-filter: blur(10px);
}

.category-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #000;
  margin: 0;
}

/* Responsive Design for Category Section */
@media (max-width: 768px) {
  .category-grid {
    grid-template-columns: 1fr;
    grid-template-rows: repeat(4, 250px);
    gap: 15px;
  }

  .category-card.casual,
  .category-card.formal,
  .category-card.party,
  .category-card.gym {
    grid-column: 1;
  }

  .category-card.casual {
    grid-row: 1;
  }

  .category-card.formal {
    grid-row: 2;
  }

  .category-card.party {
    grid-row: 3;
  }

  .category-card.gym {
    grid-row: 4;
  }

  .category-section .section-title {
    font-size: 2rem;
    margin-bottom: 40px;
  }

  .discount-badge {
    top: 8px;
    left: 8px;
    padding: 4px 8px;
    font-size: 11px;
  }
}

/* Customer Reviews */
.reviews-section {
  padding: 30px 0;
}

.review-card {
  background: white;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.review-rating {
  color: #ffc107;
  margin-bottom: 15px;
}

.review-text {
  font-style: italic;
  margin-bottom: 15px;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  /* Giới hạn 3 dòng */
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reviewer-name {
  font-weight: bold;
  color: #000;
}

.verified-badge {
  color: #28a745;
  font-size: 12px;
}

</style>