<script setup>
import {ref, computed, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import cartApi from "@/service/CartApi.js"
import ShowToastComponent from "@/components/ShowToastComponent.vue";

const router = useRouter()
const cartItems = ref([])
const isLoading = ref(true)
const user = ref(null)
const checkedItems = ref({})
const toastRef = ref(null)
const hasError = ref(false)

const goToProductDetail = (item) => {
  // Nếu có productId trực tiếp, sử dụng nó
  if (item.productId) {
    router.push(`/product-detail/${item.productId}`)
  } 
  // Nếu không có productId, có thể cần lấy từ idProductVariant
  // Hoặc có thể cần gọi API để lấy productId từ variantId
  else if (item.idProductVariant) {
    // Tạm thời sử dụng idProductVariant, có thể cần điều chỉnh sau
    console.warn('No productId found, using idProductVariant:', item.idProductVariant)
    router.push(`/product-detail/${item.idProductVariant}`)
  } else {
    console.error('Cannot navigate to product detail: no productId or idProductVariant found')
    toastRef.value?.showToast('Không thể xem chi tiết sản phẩm', 'error')
  }
}

const retryLoadCart = async () => {
  hasError.value = false
  isLoading.value = true
  
  try {
    const res = await cartApi.getCartItems(user.value.id)
    console.log('Cart API response (retry):', res)
    
    // Xử lý response với cấu trúc mới: { success, message, data }
    if (res?.data?.success === true && Array.isArray(res.data.data)) {
      cartItems.value = res.data.data
      hasError.value = false
    } else if (res?.data?.status === 200 && Array.isArray(res.data.data)) {
      // Fallback cho cấu trúc cũ
      cartItems.value = res.data.data
      hasError.value = false
    } else {
      console.warn('Unexpected response structure (retry):', res?.data)
      cartItems.value = []
      hasError.value = true
    }
    
    checkedItems.value = {}
    cartItems.value.forEach(item => {
      checkedItems.value[item.idCartItem] = false
    })
  } catch (err) {
    console.error('Error loading cart items (retry):', err)
    hasError.value = true
    cartItems.value = []
    checkedItems.value = {}
    
    if (toastRef.value) {
      toastRef.value.showToast('Vẫn không thể tải giỏ hàng. Vui lòng kiểm tra kết nối mạng.', 'error')
    }
  } finally {
    isLoading.value = false
  }
}

const handleOutOfStockError = (err, item)=>{
  const status = err?.response?.status;
  const data = err?.response?.data;
  if(status === 409 && data?.error==='OUT_OF_STOCK'){
    const cartQuantity = Number(data.carQuantity??0);
    const allowAdd = Number(data.allowAdd??0);
    const maxQuantity = Math.max(cartQuantity + allowAdd,0);

    const msg = allowAdd<=0
      ? `Không thể thêm sản phẩm vào giỏ hàng. Trong giỏ hàng hiện có ${cartQuantity}.`
      : `Không đủ hàng. Trong giỏ hàng hiện có ${cartQuantity}. Bạn chỉ có thể thêm tối đa ${allowAdd} sản phẩm nữa.`;
    toastRef.value?.showToast(msg, 'warning');

    if(item){
      item.availableQuantity = maxQuantity;
      if(item.quantity > maxQuantity){
        item.quantity = maxQuantity;
      }
      if(maxQuantity===0&& item.idCartItem){
        checkedItems.value[item.idCartItem] = false;
      }
    }
    return true;
  }
  return false;
}

// Trạng thái "chọn tất cả" - chỉ xét các sản phẩm còn hàng
const isAllSelected = computed(() => {
  const availableItems = cartItems.value.filter(item => !isOutOfStock(item))
  return Array.isArray(availableItems) && availableItems.length > 0 &&
      availableItems.every(item => checkedItems.value[item.idCartItem])
})

// Danh sách sản phẩm được chọn
const selectedCartItems = computed(() => {
  if (!Array.isArray(cartItems.value)) return []
  return cartItems.value.filter(item => checkedItems.value[item.idCartItem])
})

// Kiểm tra có sản phẩm nào vượt quá tồn kho trong danh sách được chọn
const hasOverstockItems = computed(() => {
  return selectedCartItems.value.some(item => 
    item.availableQuantity && item.quantity > item.availableQuantity
  )
})

// Lấy danh sách sản phẩm vượt quá tồn kho
const overstockItems = computed(() => {
  return selectedCartItems.value.filter(item => 
    item.availableQuantity && item.quantity > item.availableQuantity
  )
})

// Kiểm tra sản phẩm có hết hàng không
const isOutOfStock = (item) => {
  return !item.availableQuantity || item.availableQuantity <= 0
}

// Kiểm tra sản phẩm có tồn kho thấp hoặc vượt quá tồn không
const shouldShowStockWarning = (item) => {
  if (!item.availableQuantity) return false
  // Hiển thị khi: tồn kho < 10 hoặc số lượng trong giỏ > tồn kho
  return item.availableQuantity < 10 || item.quantity > item.availableQuantity
}

// Lấy thông báo cảnh báo tồn kho
const getStockWarningMessage = (item) => {
  if (!item.availableQuantity) return ''
  
  if (item.quantity > item.availableQuantity) {
    return `⚠️ Vượt quá tồn kho! Chỉ còn ${item.availableQuantity} sản phẩm`
  } else if (item.availableQuantity < 10) {
    return `⚠️ Tồn kho thấp! Chỉ còn ${item.availableQuantity} sản phẩm`
  }
  return ''
}

onMounted(async () => {
  user.value = JSON.parse(localStorage.getItem("user"))
  if (!user.value || !user.value.id) {
    router.replace("/")
    return
  }

  try {
    const res = await cartApi.getCartItems(user.value.id)
    console.log('Cart API response:', res)
    
    // Xử lý response với cấu trúc mới: { success, message, data }
    if (res?.data?.success === true && Array.isArray(res.data.data)) {
      cartItems.value = res.data.data
    } else if (res?.data?.status === 200 && Array.isArray(res.data.data)) {
      // Fallback cho cấu trúc cũ
      cartItems.value = res.data.data
    } else {
      console.warn('Unexpected response structure:', res?.data)
      cartItems.value = []
    }
    
    checkedItems.value = {}
    cartItems.value.forEach(item => {
      // Chỉ cho phép chọn những sản phẩm còn hàng, hết hàng thì luôn là false
      checkedItems.value[item.idCartItem] = false
    })
  } catch (err) {
    console.error('Error loading cart items:', err)
    
    // Hiển thị thông báo lỗi cho user
    if (toastRef.value) {
      toastRef.value.showToast('Không thể tải giỏ hàng. Vui lòng thử lại sau.', 'error')
    }
    
    // Đánh dấu có lỗi để hiển thị nút retry
    hasError.value = true
    cartItems.value = []
    checkedItems.value = {}
  } finally {
    isLoading.value = false
  }
})

const toggleSelectAll = () => {
  const newVal = !isAllSelected.value
  cartItems.value.forEach(item => {
    // Chỉ cho phép chọn những sản phẩm còn hàng
    if (!isOutOfStock(item)) {
      checkedItems.value[item.idCartItem] = newVal
    } else {
      // Sản phẩm hết hàng luôn là false
      checkedItems.value[item.idCartItem] = false
    }
  })
}

// Xóa hàm toggleItem vì không cần thiết nữa khi đã disabled

const increaseQuantity = async (item) => {
  // Sử dụng availableQuantity từ API response
  if (!item.availableQuantity || item.availableQuantity <= 0) {
    toastRef.value?.showToast('Sản phẩm này hiện đang hết hàng!', 'warning');
    return
  }

  if (item.quantity >= item.availableQuantity) {
    toastRef.value?.showToast('Không thể tăng thêm. Chỉ còn ' + item.availableQuantity + ' sản phẩm trong kho!', 'warning');
    return
  }

  try {
    await cartApi.changeCartItemQuantity(item.idCartItem, item.quantity + 1)
    item.quantity++
  } catch (err) {
    if(handleOutOfStockError(err,item)) return
    toastRef.value?.showToast('Có lỗi xảy ra khi tăng số lượng sản phẩm', 'error');
  }
}

const decreaseQuantity = async (item) => {
  if (item.quantity > 1) {
    try {
      await cartApi.changeCartItemQuantity(item.idCartItem, item.quantity - 1)
      item.quantity--
    } catch (err) {
      if(handleOutOfStockError(err,item)) return
      toastRef.value?.showToast('Có lỗi xảy ra khi giảm số lượng sản phẩm', 'error');
    }
  }
}

// Xử lý nhập tay số lượng
const handleQuantityInput = async (item, event) => {
  const inputValue = event.target.value.trim()
  
  // Cho phép input trống tạm thời khi đang nhập
  if (inputValue === '') {
    return
  }
  
  const newQuantity = parseInt(inputValue)
  
  // Kiểm tra input hợp lệ
  if (isNaN(newQuantity) || newQuantity < 1) {
    toastRef.value?.showToast('Số lượng phải là số nguyên dương', 'warning')
    return
  }
  
  // Kiểm tra sản phẩm hết hàng
  if (isOutOfStock(item)) {
    event.target.value = item.quantity
    toastRef.value?.showToast('Sản phẩm này hiện đang hết hàng!', 'warning')
    return
  }
  
  // Kiểm tra tồn kho
  if (item.availableQuantity && newQuantity > item.availableQuantity) {
    toastRef.value?.showToast(`Chỉ còn ${item.availableQuantity} sản phẩm trong kho!`, 'warning')
    return
  }
  
  // Cập nhật số lượng
  try {
    await cartApi.changeCartItemQuantity(item.idCartItem, newQuantity)
    item.quantity = newQuantity
  } catch (err) {
    if(handleOutOfStockError(err,item)) return
    // Reset về giá trị cũ nếu có lỗi
    event.target.value = item.quantity
    toastRef.value?.showToast('Có lỗi xảy ra khi cập nhật số lượng sản phẩm', 'error')
  }
}

// Xử lý khi blur khỏi input (khi người dùng hoàn thành nhập)
const handleQuantityBlur = (item, event) => {
  const inputValue = event.target.value.trim()
  
  // Nếu input trống, reset về giá trị hiện tại
  if (inputValue === '') {
    event.target.value = item.quantity
    return
  }
  
  const newQuantity = parseInt(inputValue)
  
  // Nếu input không hợp lệ, reset về giá trị hiện tại
  if (isNaN(newQuantity) || newQuantity < 1) {
    event.target.value = item.quantity
    toastRef.value?.showToast('Số lượng không hợp lệ, đã khôi phục về giá trị cũ', 'warning')
    return
  }
  
  // Kiểm tra tồn kho và cập nhật nếu cần
  if (item.availableQuantity && newQuantity > item.availableQuantity) {
    event.target.value = item.availableQuantity
    toastRef.value?.showToast(`Số lượng đã được điều chỉnh về ${item.availableQuantity} (tối đa có sẵn)`, 'info')
    // Cập nhật số lượng về giá trị tối đa có thể
    cartApi.changeCartItemQuantity(item.idCartItem, item.availableQuantity)
      .then(() => {
        item.quantity = item.availableQuantity
      })
      .catch(err => {
        if(handleOutOfStockError(err,item)) return
        event.target.value = item.quantity
        toastRef.value?.showToast('Có lỗi xảy ra khi cập nhật số lượng', 'error')
      })
  }
}

// Xử lý khi focus vào input
const handleQuantityFocus = (event) => {
  event.target.select() // Chọn toàn bộ text để dễ thay thế
}

// Xử lý khi nhấn Enter
const handleQuantityKeydown = (item, event) => {
  if (event.key === 'Enter') {
    event.target.blur() // Trigger blur để validate và cập nhật
  }
}

const removeItem = async (item) => {
  if (!item.idCartItem) {
    toastRef.value?.showToast("Không tìm thấy id sản phẩm để xóa!", 'error');
    return
  }
  try {
    await cartApi.deleteCartById(item.idCartItem)
    cartItems.value = cartItems.value.filter(i => i.idCartItem !== item.idCartItem)
    delete checkedItems.value[item.idCartItem]
  } catch (err) {
    console.error('Error removing item:', err)
    toastRef.value?.showToast('Có lỗi xảy ra khi xóa sản phẩm', 'error');
  }
}

const CheckOut = () => {
  const selectedIds = selectedCartItems.value.map(item => item.idCartItem)
  if (selectedIds.length === 0) {
    toastRef.value?.showToast("Vui lòng chọn ít nhất một sản phẩm để thanh toán.", 'warning');
    return
  }

  // Kiểm tra có sản phẩm vượt quá tồn kho không
  if (hasOverstockItems.value) {
    const overstockNames = overstockItems.value.map(item => item.productName).join(', ')
    toastRef.value?.showToast(
      `Không thể thanh toán! Các sản phẩm sau vượt quá tồn kho: ${overstockNames}. Vui lòng điều chỉnh số lượng.`, 
      'error'
    );
    return
  }

  router.push({
    name: 'CheckoutView',
    query: {ids: selectedIds.join(',')}
  })
}

const calculateTotal = computed(() => {
  return selectedCartItems.value.reduce((sum, item) => {
    const finalPrice = item.discountPercent ?
        item.price * (1 - item.discountPercent / 100) :
        item.price
    return sum + (finalPrice * item.quantity)
  }, 0)
})

const getFinalPrice = (item) => {
  if (item.discountPercent) {
    return item.price * (1 - item.discountPercent / 100)
  }
  return item.price
}

const getOriginalPrice = (item) => {
  return item.price
}


</script>

<template>
  <div class="cart-container" v-if="!isLoading">
    <div v-if="cartItems.length">
      <!-- Cart Header -->
      <div class="cart-header">
        <div class="form-check d-flex align-items-center">
          <div class="custom-checkbox">
            <input
                class="custom-checkbox-input"
                type="checkbox"
                id="selectAllCart"
                :checked="isAllSelected"
                @change="toggleSelectAll"
            >
            <label class="custom-checkbox-label" for="selectAllCart">
              <span class="checkmark">✓</span>
            </label>
          </div>
          <label class="form-check-label ms-2" for="selectAllCart" style="font-size: 1.12rem; font-weight:600; margin-bottom: 5px">
            Chọn tất cả
          </label>
        </div>
        <div class="cart-title">
          <i class="fas fa-shopping-cart me-2"></i>
          GIỎ HÀNG
        </div>
      </div>
      <!-- Main Content -->
      <div class="cart-content-area">
        <!-- Cart List -->
        <div class="cart-list">
          <div class="cart-item"
               v-for="item in cartItems"
               :key="item.idCartItem"
               :class="{ 'out-of-stock': isOutOfStock(item) }">
            <div class="custom-checkbox">
              <input
                  class="custom-checkbox-input"
                  type="checkbox"
                  :id="'cartItem'+item.idCartItem"
                  v-model="checkedItems[item.idCartItem]"
                  :disabled="isOutOfStock(item)"
              >
              <label class="custom-checkbox-label" :for="'cartItem'+item.idCartItem">
                <span class="checkmark">✓</span>
              </label>
            </div>
            <div class="cart-item-image-wrapper" @click="goToProductDetail(item)" :title="'Xem chi tiết ' + item.productName">
              <img :src="item.productImage" alt="" class="cart-item-img">
              <div v-if="isOutOfStock(item)" class="out-of-stock-overlay">
                <span class="out-of-stock-text">HẾT HÀNG</span>
              </div>
            </div>
            <div class="cart-item-details">
              <div class="cart-item-title" 
                   :class="{ 'strikethrough': isOutOfStock(item) }"
                   @click="goToProductDetail(item)" 
                   :title="'Xem chi tiết ' + item.productName">
                {{ item.productName }}
              </div>
              <div class="cart-item-desc" :class="{ 'strikethrough': isOutOfStock(item) }">
                Size: {{ item.size }} &nbsp;|&nbsp; Màu: {{ item.color }}
              </div>
              <!-- Hiển thị cảnh báo tồn kho -->
              <div v-if="shouldShowStockWarning(item)" class="stock-warning-message">
                {{ getStockWarningMessage(item) }}
              </div>
              <div class="cart-item-actions">
                <button class="qty-btn"
                        @click="decreaseQuantity(item)"
                        :disabled="isOutOfStock(item)">-</button>
                <input 
                  type="number" 
                  class="qty-input"
                  :class="{ 'strikethrough': isOutOfStock(item) }"
                  :value="item.quantity"
                  :disabled="isOutOfStock(item)"
                  min="1"
                  :max="item.availableQuantity || 999999"
                  @input="handleQuantityInput(item, $event)"
                  @blur="handleQuantityBlur(item, $event)"
                  @focus="handleQuantityFocus"
                  @keydown="handleQuantityKeydown(item, $event)"
                />
                <button class="qty-btn"
                        @click="increaseQuantity(item)"
                        :disabled="isOutOfStock(item)">+</button>
                <button class="remove-btn" title="Xoá" @click="removeItem(item)">
                  <i class="fa fa-trash"></i>
                </button>
              </div>
            </div>
            <div class="cart-item-price" :class="{ 'strikethrough': isOutOfStock(item) }">
              <div v-if="item.discountPercent" class="price-with-discount">
                <div class="original-price">{{ (getOriginalPrice(item) * item.quantity).toLocaleString() }}đ</div>
                <div class="discount-badge">-{{ item.discountPercent }}%</div>
                <div class="final-price">{{ (getFinalPrice(item) * item.quantity).toLocaleString() }}đ</div>
              </div>
              <div v-else class="price-no-discount">
                {{ (item.price * item.quantity).toLocaleString() }}đ
              </div>
            </div>
          </div>
        </div>
        <!-- Order Summary -->
        <div class="order-summary">
          <div v-if="selectedCartItems.some(item => item.discountPercent)" class="order-summary-row">
            <span>Tạm tính (chưa giảm giá)</span>
            <span>{{ selectedCartItems.reduce((sum, item) => sum + (item.price * item.quantity), 0).toLocaleString() }}đ</span>
          </div>
          <div v-if="selectedCartItems.some(item => item.discountPercent)" class="order-summary-row discount-total">
            <span>Tiết kiệm</span>
            <span>-{{ selectedCartItems.reduce((sum, item) => {
              if (item.discountPercent) {
                return sum + (item.price * item.quantity * item.discountPercent / 100)
              }
              return sum
            }, 0).toLocaleString() }}đ</span>
          </div>
          <div class="order-summary-row">
            <span>Tạm tính</span>
            <span>{{ calculateTotal.toLocaleString() }}đ</span>
          </div>
          <div class="order-summary-row total">
            <span>Tổng</span>
            <span>{{ calculateTotal.toLocaleString() }}đ</span>
          </div>
          
          <!-- Cảnh báo vượt quá tồn kho -->
          <div v-if="hasOverstockItems" class="overstock-warning">
            <div class="warning-icon">⚠️</div>
            <div class="warning-text">
              <strong>Không thể thanh toán!</strong><br>
              Có sản phẩm vượt quá tồn kho. Vui lòng điều chỉnh số lượng.
            </div>
          </div>
          
          <button
              class="btn-checkout"
              @click="CheckOut"
              :disabled="selectedCartItems.length === 0 || hasOverstockItems"
          >
            Thanh toán
          </button>
        </div>
      </div>
    </div>
    <div v-else-if="hasError">
      <div style="text-align:center; padding: 150px 0;">
        <div style="font-size: 4rem; margin-bottom: 20px;">⚠️</div>
        <p style="font-size:1.18rem; color: #dc3545; font-weight: 600; margin-bottom: 10px;">
          Không thể tải giỏ hàng
        </p>
        <p style="font-weight:400; font-size:0.95rem; color:#888; margin-bottom: 30px;">
          Có lỗi xảy ra khi tải dữ liệu giỏ hàng. Vui lòng thử lại.
        </p>
        <div style="display: flex; gap: 15px; justify-content: center; flex-wrap: wrap;">
          <button
              class="btn-checkout"
              style="width:auto; padding:12px 28px; font-size:1rem; background: #007bff;"
              @click="retryLoadCart"
              :disabled="isLoading"
          >
            <span v-if="isLoading" class="spinner-border spinner-border-sm me-2"></span>
            {{ isLoading ? 'Đang tải...' : 'Thử lại' }}
          </button>
          <button
              class="btn-checkout"
              style="width:auto; padding:12px 28px; font-size:1rem; background: #6c757d;"
              @click="router.replace('/')"
          >
            Về trang chủ
          </button>
        </div>
      </div>
    </div>
    <div v-else>
      <div style="text-align:center; padding: 150px 0;">
        <p style="font-size:1.18rem; color: #2a2a2a; font-weight: 600;">
          Giỏ hàng của bạn đang trống.<br>
          <span style="font-weight:400; font-size:0.95rem; color:#888;">
            Hãy thêm sản phẩm vào giỏ hàng để tiếp tục mua sắm!
          </span>
        </p>
        <button
            class="btn-checkout"
            style="width:auto; padding:12px 28px; font-size:1rem;"
            @click="router.replace('/')"
        >
          Về trang chủ
        </button>
      </div>
    </div>
  </div>

  <ShowToastComponent ref="toastRef" />
</template>

<style scoped>
.cart-container {
  width: 100%;
  max-width: 1330px;
  margin: 10px auto 40px auto;
  overflow: hidden;
  padding: 0;
  animation: cartAppear 0.28s;
  min-height: 44vh;
}

@keyframes cartAppear {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32px 38px 18px 38px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.cart-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #000;
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: 0.5px;
}

.cart-content-area {
  display: flex;
  gap: 32px;
  padding: 0 38px 28px 38px;
  flex-wrap: wrap;
}

.cart-list {
  flex: 2 1 420px;
}

.cart-item {
  display: flex;
  gap: 18px;
  align-items: center;
  padding: 22px 20px;
  border-bottom: 1px solid #eee;
  background: transparent;
  border-radius: 8px;
  position: relative;
  transition: all 0.3s ease;
}

.cart-item:hover {
  background: #f8f8f8;
  box-shadow: 0 1px 6px 0 #eee;
}

/* Styles cho sản phẩm hết hàng */
.cart-item.out-of-stock {
  opacity: 0.7;
  background: #f9f9f9;
}

.cart-item-image-wrapper {
  position: relative;
  display: inline-block;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.cart-item-image-wrapper:hover {
  transform: scale(1.02);
}

.cart-item-img {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  object-fit: cover;
  background: #f5f5f5;
  box-shadow: 0 1.5px 5px 0 #ececec;
  border: 1.2px solid #eee;
  transition: filter 0.3s ease;
}

.out-of-stock .cart-item-img {
  filter: grayscale(50%);
}

.out-of-stock-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}

.out-of-stock-text {
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  text-align: center;
  letter-spacing: 1px;
  padding: 2px 4px;
  background: rgba(255, 0, 0, 0.8);
  border-radius: 4px;
}

/* Strikethrough effect */
.strikethrough {
  position: relative;
  color: #999 !important;
}

.strikethrough::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #999;
  transform: translateY(-50%);
}

.cart-item-details {
  flex: 1;
  min-width: 0;
}

.cart-item-title {
  font-size: 1.08rem;
  font-weight: 700;
  margin-bottom: 1.5px;
  color: #000;
  cursor: pointer;
  transition: color 0.2s ease;
  padding: 2px 0;
  border-radius: 4px;
}

.cart-item-title:hover {
  color: #007bff;
  background-color: rgba(0, 123, 255, 0.05);
}

.cart-item-desc {
  font-size: 0.93rem;
  color: #888;
  margin-bottom: 2px;
  letter-spacing: 0.01em;
}

.stock-warning-message {
  font-size: 0.85rem;
  color: #ff6b35;
  background: rgba(255, 107, 53, 0.1);
  border: 1px solid rgba(255, 107, 53, 0.3);
  border-radius: 6px;
  padding: 6px 8px;
  margin: 4px 0;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.cart-item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
}

.qty-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #bbb;
  border-radius: 50%;
  background: #fff;
  color: #000;
  font-weight: 700;
  text-align: center;
  font-size: 1.08rem;
  box-shadow: none;
  transition: background 0.19s, border-color 0.19s;
  cursor: pointer;
}

.qty-btn:hover:not(:disabled) {
  background: #eee;
  border-color: #222;
}

.qty-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f5f5f5;
  border-color: #ddd;
}

.qty-number {
  min-width: 22px;
  text-align: center;
  font-size: 1rem;
  font-weight: 600;
  color: #222;
}

.qty-input {
  width: 50px;
  height: 30px;
  border: 1px solid #bbb;
  border-radius: 4px;
  background: #fff;
  color: #000;
  font-weight: 600;
  text-align: center;
  font-size: 1rem;
  padding: 0 4px;
  transition: border-color 0.19s, background 0.19s;
  outline: none;
}

.qty-input:hover:not(:disabled) {
  border-color: #222;
  background: #f8f8f8;
}

.qty-input:focus:not(:disabled) {
  border-color: #000;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
}

.qty-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f5f5f5;
  border-color: #ddd;
  color: #999;
}

.qty-input::-webkit-outer-spin-button,
.qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.qty-input[type=number] {
  -moz-appearance: textfield;
  appearance: textfield;
}

.remove-btn {
  background: none;
  border: none;
  color: #888;
  font-size: 1.13rem;
  margin-left: 5px;
  padding: 3px 6px;
  border-radius: 4px;
  transition: background 0.13s, color 0.13s;
  cursor: pointer;
}

.remove-btn:hover {
  background: #eee;
  color: #000;
}

.cart-item-price {
  font-size: 1.1rem;
  font-weight: 700;
  color: #000;
  min-width: 75px;
  text-align: right;
  letter-spacing: 0.5px;
}

.price-with-discount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.original-price {
  font-size: 0.9rem;
  color: #888;
  text-decoration: line-through;
  font-weight: 500;
}

.discount-badge {
  background: #ff4757;
  color: white;
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  line-height: 1;
}

.final-price {
  color: #ff4757;
  font-weight: 700;
  font-size: 1.1rem;
}

.price-no-discount {
  color: #000;
}

/* Order Summary */
.order-summary {
  flex: 1 1 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2.5px 9px 0 #eee;
  padding: 22px 18px 14px 18px;
  min-width: 320px;
  margin-top: 15px;
  border: 1px solid #eee;
}

.order-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 1.01rem;
  font-weight: 500;
  color: #222;
}

.order-summary .total {
  font-weight: 800;
  font-size: 1.18rem;
  margin-top: 10px;
  margin-bottom: 16px;
  letter-spacing: 0.01em;
  color: #000;
}

.order-summary .discount-total {
  color: #28a745;
  font-weight: 600;
}

.btn-checkout {
  border-radius: 80px;
  background: #000;
  color: #fff;
  width: 100%;
  padding: 12px 0;
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 8px;
  box-shadow: none;
  letter-spacing: 0.8px;
  border: none;
  transition: background 0.16s, color 0.16s, opacity 0.16s;
}

.btn-checkout:hover:not(:disabled) {
  background: #222;
  color: #fff;
}

.btn-checkout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.overstock-warning {
  background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
  border: 2px solid #f44336;
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: pulse-warning 2s infinite;
}

.warning-icon {
  font-size: 24px;
  flex-shrink: 0;
  margin-top: 2px;
}

.warning-text {
  color: #d32f2f;
  font-size: 14px;
  line-height: 1.4;
  font-weight: 500;
}

.warning-text strong {
  font-weight: 700;
  font-size: 15px;
}

@keyframes pulse-warning {
  0% {
    box-shadow: 0 0 0 0 rgba(244, 67, 54, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(244, 67, 54, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(244, 67, 54, 0);
  }
}

/* Custom Checkbox Styles */
.custom-checkbox {
  position: relative;
  display: inline-block;
  vertical-align: middle;
}

.custom-checkbox-input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.custom-checkbox-input:disabled {
  cursor: not-allowed;
}

.custom-checkbox-label {
  position: relative;
  display: inline-block;
  width: 19px;
  height: 19px;
  background-color: #fff;
  border: 1.5px solid #222;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.custom-checkbox-label:hover {
  border-color: #000;
  background-color: #f8f8f8;
}

.custom-checkbox-input:disabled + .custom-checkbox-label {
  background-color: #f5f5f5;
  border-color: #ddd;
  cursor: not-allowed;
}

.custom-checkbox-input:disabled + .custom-checkbox-label:hover {
  background-color: #f5f5f5;
  border-color: #ddd;
}

.checkmark {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
  font-weight: bold;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.custom-checkbox-input:checked + .custom-checkbox-label {
  background-color: #000;
  border-color: #000;
}

.custom-checkbox-input:checked + .custom-checkbox-label .checkmark {
  opacity: 1;
}

.custom-checkbox-input:disabled:checked + .custom-checkbox-label {
  background-color: #ccc;
  border-color: #ccc;
}

/* Alignment fixes */
.form-check {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-check-label {
  color: #222;
  margin: 0;
  line-height: 1.2;
}

@media (max-width: 1020px) {
  .cart-content-area {
    flex-direction: column;
    gap: 24px;
    padding: 0 9px 20px 9px;
  }

  .order-summary {
    min-width: 0;
    margin-top: 9px;
  }
}

@media (max-width: 700px) {
  .cart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    padding: 13px 6px 6px 6px;
  }

  .cart-content-area {
    flex-direction: column;
    gap: 14px;
    padding: 0 4px 9px 4px;
  }

  .cart-list {
    min-width: 0;
  }

  .order-summary {
    margin-top: 6px;
    padding: 12px 6px 7px 6px;
  }

  .out-of-stock-text {
    font-size: 0.6rem;
    padding: 4px 18px;
    letter-spacing: 0.8px;
  }
}

/* Spinner styles */
.spinner-border {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  vertical-align: text-bottom;
  border: 0.125em solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spinner-border 0.75s linear infinite;
}

.spinner-border-sm {
  width: 0.875rem;
  height: 0.875rem;
  border-width: 0.1em;
}

@keyframes spinner-border {
  to {
    transform: rotate(360deg);
  }
}
</style>