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

// Kiểm tra sản phẩm có hết hàng không
const isOutOfStock = (item) => {
  return !item.availableQuantity || item.availableQuantity <= 0
}

onMounted(async () => {
  user.value = JSON.parse(localStorage.getItem("user"))
  if (!user.value || !user.value.id) {
    router.replace("/")
    return
  }

  try {
    const res = await cartApi.getCartItems(user.value.id)
    cartItems.value = Array.isArray(res.data?.data) ? res.data.data : []
    checkedItems.value = {}
    cartItems.value.forEach(item => {
      // Chỉ cho phép chọn những sản phẩm còn hàng, hết hàng thì luôn là false
      checkedItems.value[item.idCartItem] = false
    })
  } catch (err) {
    console.error('Error loading cart items:', err)
    router.replace("/")
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
    console.error('Error increasing quantity:', err)
    toastRef.value?.showToast('Có lỗi xảy ra khi tăng số lượng sản phẩm', 'error');
  }
}

const decreaseQuantity = async (item) => {
  if (item.quantity > 1) {
    try {
      await cartApi.changeCartItemQuantity(item.idCartItem, item.quantity - 1)
      item.quantity--
    } catch (err) {
      console.error('Error decreasing quantity:', err)
      toastRef.value?.showToast('Có lỗi xảy ra khi giảm số lượng sản phẩm', 'error');
    }
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
            <div class="cart-item-image-wrapper">
              <img :src="item.productImage" alt="" class="cart-item-img">
              <div v-if="isOutOfStock(item)" class="out-of-stock-overlay">
                <span class="out-of-stock-text">HẾT HÀNG</span>
              </div>
            </div>
            <div class="cart-item-details">
              <div class="cart-item-title" :class="{ 'strikethrough': isOutOfStock(item) }">
                {{ item.productName }}
              </div>
              <div class="cart-item-desc" :class="{ 'strikethrough': isOutOfStock(item) }">
                Size: {{ item.size }} &nbsp;|&nbsp; Màu: {{ item.color }}
              </div>
              <div class="cart-item-actions">
                <button class="qty-btn"
                        @click="decreaseQuantity(item)"
                        :disabled="isOutOfStock(item)">-</button>
                <span class="mx-1 qty-number"
                      :class="{ 'strikethrough': isOutOfStock(item) }">{{ item.quantity }}</span>
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
          <button
              class="btn-checkout"
              @click="CheckOut"
              :disabled="selectedCartItems.length === 0"
          >
            Thanh toán
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
}

.cart-item-desc {
  font-size: 0.93rem;
  color: #888;
  margin-bottom: 2px;
  letter-spacing: 0.01em;
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
</style>