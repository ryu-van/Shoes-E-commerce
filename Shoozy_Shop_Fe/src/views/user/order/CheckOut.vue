<script setup>
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCheckoutItems, createOrder } from '@/service/OrderApi.js'
import paymentMethodApi from '@/service/PaymentMethodApi.js'
import couponApi from '@/service/CouponApi'
import { createVnPayPayment } from '@/service/PaymentService.js'
import ShowToastComponent from '@/components/ShowToastComponent.vue'
import ListAddressModal from '@/components/ListAddressModal.vue'
import { getSelectedAddress } from '@/service/AddressApi.js'
import { ShippingApi } from '@/service/ShippingApi'

// 🔌 WS utils
import { connectWebSocket, subscribeCouponEvents } from '@/service/Websocket'

const route = useRoute()
const router = useRouter()

// ====== STATE ======
const user = ref(null)
const cartItems = ref([])
const isLoading = ref(true)
const isSubmitting = ref(false)

const paymentMethods = ref([])
const selectedPaymentMethodId = ref(null)

const fullnameInput = ref('')
const phoneInput = ref('')
const noteInput = ref('')

// Địa chỉ hiển thị (được bind vào input readonly)
const addressInput = ref('')

// Lưu object địa chỉ đang dùng để tính phí
const selectedAddressObj = ref(null)

// Coupon
const couponInput = ref('')              // luôn ưu tiên chứa CODE
const couponPreview = ref(null)          // { id, code, name, type, condition, value, valueLimit, status, discountAmount }
const couponLoading = ref(false)
const couponError = ref('')
const couponSuccess = ref('')

// Danh sách mã giảm giá khả dụng
const availableCoupons = ref([])
const showCouponList = ref(false)
const couponListLoading = ref(false)

// Shipping
const shippingFee = ref(0)
const isCalculatingShipping = ref(false)
const shippingError = ref('')

// Modal địa chỉ
const showAddressModal = ref(false)

// Toast
const toastRef = ref(null)

// Subscription coupon WS
let couponSub = null

// ====== UTILS ======
const formatPrice = (val) => ((val || 0).toLocaleString('vi-VN') + '₫')

// Tính tổng khối lượng dựa trên biến thể có mặt và số lượng
const getCartWeight = () => {
  const total = cartItems.value.reduce(
    (sum, item) => {
      const itemWeightKg = item.weight || 0.8
      const itemWeightGrams = itemWeightKg * 1000
      return sum + (itemWeightGrams * (item.quantity || 0))
    },
    0
  )
  return total > 0 ? total : 800
}

const toNum = (v) => {
  const n = Number(v)
  return Number.isFinite(n) ? n : undefined
}

// build payload cho /shipping/fee-from-openapi
const buildOpenApiPayloadFromAddress = (addr = {}) => {
  if (!addr.provinceName || !addr.districtName || !addr.wardName) {
    console.warn('Thiếu thông tin địa chỉ để tính phí vận chuyển:', addr)
  }

  const cartWeight = getCartWeight()
  console.log('Shipping calculation - Total weight (kg → gram × quantity):', cartWeight, 'grams')
  console.log('Cart items weight breakdown:', cartItems.value.map(item => {
    const itemWeightKg = item.weight || 0.8
    const itemWeightGrams = itemWeightKg * 1000
    return {
      name: item.productName,
      variant: `${item.size || ''} ${item.color || ''}`.trim(),
      weightKg: itemWeightKg,
      weightGrams: itemWeightGrams,
      quantity: item.quantity,
      totalWeightGrams: itemWeightGrams * (item.quantity || 0)
    }
  }))

  return {
    provinceCode: toNum(addr.provinceCode),
    districtCode: toNum(addr.districtCode),
    wardCode: addr.wardCode != null ? String(addr.wardCode) : undefined,
    provinceName: addr.provinceName,
    districtName: addr.districtName,
    wardName: addr.wardName,
    weight: cartWeight,
    serviceTypeId: null,
    insuranceValue: 0
  }
}

// Gọi API tính phí
const calculateShippingFeeFromAddress = async (addressObj) => {
  if (!addressObj) return
  isCalculatingShipping.value = true
  shippingError.value = ''
  try {
    if (!addressObj.provinceName || !addressObj.districtName || !addressObj.wardName) {
      throw new Error('Địa chỉ không đủ thông tin để tính phí vận chuyển')
    }
    const payload = buildOpenApiPayloadFromAddress(addressObj)
    console.log('Calculating shipping fee with payload:', payload)
    const res = await ShippingApi.calcFeeFromOpenApi(payload)
    const data = res?.data?.data || res?.data
    console.log('Shipping fee response:', data)
    const fee = Number(data?.total)
    shippingFee.value = Number.isFinite(fee) ? fee : 0
    console.log('Set shipping fee to:', shippingFee.value)
  } catch (e) {
    console.error('Shipping fee error:', e)
    shippingFee.value = 0
    shippingError.value =
      e?.response?.data?.message ||
      e?.message ||
      'Không tính được phí vận chuyển, vui lòng kiểm tra lại địa chỉ'
  } finally {
    isCalculatingShipping.value = false
  }
}

// Tổng trước giảm
const totalBeforeDiscount = computed(() =>
  cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
)

// Tổng sau khi áp dụng khuyến mãi sản phẩm
const totalAfterProductPromotion = computed(() =>
  cartItems.value.reduce((sum, item) => {
    const finalPrice = (item.valuePromotion && item.valuePromotion > 0 && item.valuePromotion <= 100)
      ? item.price * (1 - item.valuePromotion / 100)
      : item.price
    return sum + (finalPrice * item.quantity)
  }, 0)
)

// Tổng xem trước (chưa ship)
const previewTotal = computed(() => {
  let total = totalAfterProductPromotion.value
  if (couponPreview.value?.discountAmount) {
    total -= couponPreview.value.discountAmount
  }
  return total > 0 ? total : 0
})

// Tổng cộng (đã ship)
const finalTotal = computed(() => {
  return previewTotal.value + shippingFee.value
})

// Giảm giá từ coupon
const couponDiscount = computed(() => {
  return Number(couponPreview.value?.discountAmount || 0)
})

// Giá cuối của item
const getFinalProductPrice = (item) => {
  return (item.valuePromotion && item.valuePromotion > 0 && item.valuePromotion <= 100)
    ? item.price * (1 - item.valuePromotion / 100)
    : item.price
}

// Tổng từng item
const getProductTotal = (item) => {
  const price = getFinalProductPrice(item)
  return price * item.quantity
}

// ====== WS subscribe helper (có retry) ======
const subscribeCurrentCoupon = (couponIdOrCode) => {
  if (couponSub) {
    try { couponSub.unsubscribe() } catch {}
    couponSub = null
  }
  if (!couponIdOrCode) return

  const trySub = (times = 0) => {
    couponSub = subscribeCouponEvents(String(couponIdOrCode), (evt) => {
      // evt: { type:'COUPON_EVENT', event:'DECREMENT'|'OUT_OF_STOCK', remaining, status, code, couponId, ... }
      console.log('Coupon WS event:', evt)
      if (evt.event === 'OUT_OF_STOCK' || evt.remaining === 0) {
        toastRef.value?.showToast('Mã giảm giá đã hết lượt', 'warning')
        removeCoupon()
      }
    })
    if (!couponSub && times < 5) {
      setTimeout(() => trySub(times + 1), 400)
    }
  }
  trySub()
}

// ====== LIFECYCLE ======
onMounted(async () => {
  try {
    // Kết nối WS sớm để nhận realtime coupon
    connectWebSocket()

    user.value = JSON.parse(localStorage.getItem('user') || '{}')
    if (!user.value?.id) {
      toastRef.value?.showToast('Vui lòng đăng nhập để tiếp tục!', 'error')
      router.replace('/login')
      return
    }

    fullnameInput.value = user.value.fullname || ''
    phoneInput.value = user.value.phoneNumber || ''

    // Địa chỉ mặc định
    try {
      const addressResponse = await getSelectedAddress(user.value.id)
      const selectedAddress = addressResponse?.data?.data
      if (selectedAddress) {
        const line = selectedAddress.line
          || [selectedAddress.addressDetail, selectedAddress.wardName, selectedAddress.districtName, selectedAddress.provinceName]
              .filter(Boolean).join(', ')
        addressInput.value = line
        user.value.address = line
        selectedAddressObj.value = selectedAddress
      } else {
        toastRef.value?.showToast('Vui lòng chọn địa chỉ để tính phí vận chuyển', 'warning')
      }
    } catch {
      toastRef.value?.showToast('Vui lòng chọn địa chỉ để tính phí vận chuyển', 'warning')
    }

    // Lấy giỏ hàng
    const idsString = route.query.ids || ''
    const ids = idsString.split(',').filter(Boolean).map(id => Number(id)).filter(id => !isNaN(id))
    if (!ids.length) {
      toastRef.value?.showToast('Không có sản phẩm nào được chọn!', 'error')
      router.replace('/cart')
      return
    }

    // Gọi song song
    const [paymentRes, cartRes] = await Promise.all([
      paymentMethodApi.getAllPaymentMethods(),
      getCheckoutItems(ids)
    ])

    // Payment methods
    const pmData = paymentRes?.data?.data
    paymentMethods.value = Array.isArray(pmData) ? pmData : []
    if (paymentMethods.value.length > 0) {
      selectedPaymentMethodId.value = paymentMethods.value[0].id
    }

    // Cart
    const cartData = cartRes?.data?.data
    cartItems.value = Array.isArray(cartData) ? cartData : []
    if (!cartItems.value.length) {
      toastRef.value?.showToast('Không tìm thấy sản phẩm nào!', 'error')
      router.replace('/carts')
      return
    }
    cartItems.value.forEach((item, index) => {
      console.log(`Item ${index + 1}:`, {
        name: item.productName,
        price: item.price,
        valuePromotion: item.valuePromotion,
        quantity: item.quantity,
        weight: item.weight
      })
    })

    const totalWeight = getCartWeight()
    console.log('Total cart weight:', totalWeight, 'grams')

    // tính ship nếu đủ địa chỉ
    if (selectedAddressObj.value?.provinceName && selectedAddressObj.value?.districtName && selectedAddressObj.value?.wardName) {
      await calculateShippingFeeFromAddress(selectedAddressObj.value)
    }
  } catch (error) {
    toastRef.value?.showToast('Có lỗi xảy ra khi tải trang!', 'error')
    router.replace('/carts')
  } finally {
    isLoading.value = false
  }
})

onBeforeUnmount(() => {
  if (couponSub) {
    try { couponSub.unsubscribe() } catch {}
    couponSub = null
  }
})

// Auto recalc phí khi giỏ hàng/địa chỉ đổi
watch([cartItems, selectedAddressObj], async ([items, addr]) => {
  if (!addr?.provinceName || !addr?.districtName || !addr?.wardName) return
  if (!Array.isArray(items) || items.length === 0) return
  if (isCalculatingShipping.value) return
  try {
    await calculateShippingFeeFromAddress(addr)
  } catch { /* no-op */ }
}, { deep: true })

// Khi tổng tiền thay đổi và danh sách coupon đang mở → refresh danh sách
watch(totalAfterProductPromotion, () => {
  if (showCouponList.value) {
    loadAvailableCoupons()
  }
})

// ====== COUPON ======
const loadAvailableCoupons = async () => {
  if (!user.value?.id) return

  couponListLoading.value = true
  try {
    const moneyOrder = Number(totalAfterProductPromotion.value)
    const res = await couponApi.getAllCoupons(user.value.id, moneyOrder)
    const coupons = res?.data?.data || []

    // Ưu tiên code từ BE; fallback = name nếu BE chưa trả
    availableCoupons.value = coupons
      .filter(c => c.status === 1)
      .map(c => {
        let actualDiscount = 0
        const total = moneyOrder
        if (c.type) { // %
          actualDiscount = total * (c.value / 100)
          if (c.valueLimit && actualDiscount > c.valueLimit) actualDiscount = c.valueLimit
        } else {
          actualDiscount = Math.min(c.value, total)
        }
        return {
          ...c,
          code: c.code || c.name,
          actualDiscount,
          discountPercent: c.type ? c.value : (total ? (actualDiscount / total) * 100 : 0)
        }
      })
      .sort((a, b) => b.actualDiscount - a.actualDiscount)
  } catch (err) {
    console.error('Lỗi khi tải mã giảm giá:', err)
    availableCoupons.value = []
  } finally {
    couponListLoading.value = false
  }
}

const handleApplyCoupon = async () => {
  couponError.value = ''
  couponSuccess.value = ''
  couponPreview.value = null

  if (!couponInput.value.trim()) {
    couponError.value = 'Vui lòng nhập mã giảm giá!'
    return
  }

  couponLoading.value = true
  try {
    const id_user = user.value?.id || null
    const res = await couponApi.getCouponByCode(couponInput.value.trim(), id_user)
    const coupon = res?.data?.data

    if (!coupon?.id) {
      couponError.value = 'Mã giảm giá không hợp lệ!'
      return
    }

    // Trạng thái
    if (coupon.status === 0) { couponError.value = 'Mã giảm giá này chưa bắt đầu!'; return }
    if (coupon.status === 2) { couponError.value = 'Mã giảm giá này đã hết hạn!'; return }
    if (coupon.status === 3) { couponError.value = 'Mã giảm giá này đã bị xóa!'; return }
    if (coupon.status !== 1) { couponError.value = 'Mã giảm giá không hợp lệ!'; return }

    // Kiểm tra điều kiện đơn tối thiểu ở FE
    const total = totalAfterProductPromotion.value
    if (coupon.condition != null && total < coupon.condition) {
      couponError.value = `Đơn tối thiểu ${formatPrice(coupon.condition)} mới dùng được mã này`
      return
    }

    // FE tính giảm
    let discountAmount = 0
    if (coupon.type) {
      discountAmount = total * (coupon.value / 100)
      if (coupon.valueLimit && discountAmount > coupon.valueLimit) discountAmount = coupon.valueLimit
    } else {
      discountAmount = Math.min(coupon.value, total)
    }

    // Bắt buộc gắn code vào preview (phòng BE chưa trả)
    const code = coupon.code || couponInput.value.trim()

    couponPreview.value = { ...coupon, code, discountAmount }
    couponSuccess.value = `Áp dụng mã thành công: Giảm ${coupon.type ? coupon.value + '%' : formatPrice(coupon.value)}`
    couponInput.value = code // đồng bộ input hiển thị CODE

    // Sub WS theo coupon id/code
    subscribeCurrentCoupon(couponPreview.value.id ?? couponPreview.value.code)
  } catch (e) {
    console.error('Lỗi khi kiểm tra coupon:', e)
    const msg =
      e?.response?.data?.message ??
      e?.data?.message ??
      e?.response?.data?.error ??
      e?.response?.statusText ??
      e?.message ??
      'Mã giảm giá không hợp lệ hoặc đã hết hạn!'
    couponError.value = msg
    couponPreview.value = null
    couponSuccess.value = ''
  } finally {
    couponLoading.value = false
  }
}

const removeCoupon = () => {
  couponPreview.value = null
  couponInput.value = ''
  couponError.value = ''
  couponSuccess.value = ''
  if (couponSub) {
    try { couponSub.unsubscribe() } catch {}
    couponSub = null
  }
}

// Áp dụng mã giảm giá từ danh sách
const applyCouponFromList = (coupon) => {
  const code = coupon.code || coupon.name
  const total = totalAfterProductPromotion.value

  // Kiểm tra điều kiện đơn tối thiểu
  if (coupon.condition != null && total < coupon.condition) {
    couponError.value = `Đơn tối thiểu ${formatPrice(coupon.condition)} mới dùng được mã này`
    couponSuccess.value = ''
    return
  }

  const discountAmount = coupon.actualDiscount ?? 0
  couponPreview.value = { ...coupon, code, discountAmount }
  couponInput.value = code
  couponSuccess.value = `Áp dụng mã thành công: ${coupon.name} - Giảm ${formatPrice(discountAmount)}`
  couponError.value = ''
  showCouponList.value = false

  // Sub WS
  subscribeCurrentCoupon(coupon.id ?? code)
}

// Toggle danh sách
const toggleCouponList = () => {
  if (!showCouponList.value && availableCoupons.value.length === 0) {
    loadAvailableCoupons()
  }
  showCouponList.value = !showCouponList.value
}

// ====== CHECKOUT ======
const handleCheckout = async () => {
  if (!user.value?.id) {
    toastRef.value?.showToast('Vui lòng đăng nhập lại!', 'error')
    router.replace('/login')
    return
  }
  if (!cartItems.value.length) {
    toastRef.value?.showToast('Không có sản phẩm nào để đặt hàng!', 'error')
    return
  }
  if (!selectedPaymentMethodId.value) {
    toastRef.value?.showToast('Vui lòng chọn phương thức thanh toán!', 'error')
    return
  }
  if (!fullnameInput.value.trim()) {
    toastRef.value?.showToast('Vui lòng nhập họ tên người nhận!', 'error')
    return
  }
  if (!phoneInput.value.trim()) {
    toastRef.value?.showToast('Vui lòng nhập số điện thoại người nhận!', 'error')
    return
  }
  if (!addressInput.value.trim()) {
    toastRef.value?.showToast('Vui lòng nhập địa chỉ người nhận!', 'error')
    return
  }

  isSubmitting.value = true
  try {
    const orderDetails = cartItems.value.map(item => ({
      productVariantId: item.idProductVariant,
      quantity: item.quantity,
      idPromotion: item.idPromotion || null,
    }))
    const cartItemIds = cartItems.value.map(item => item.idCartItem)

    const orderRequest = {
      userId: user.value.id,
      paymentMethodId: selectedPaymentMethodId.value,
      // ⚠️ gửi CODE, không gửi name
      couponCode: couponPreview.value?.code || (couponInput.value?.trim() || null),
      fullname: fullnameInput.value.trim(),
      phoneNumber: phoneInput.value.trim(),
      address: addressInput.value,
      note: noteInput.value.trim(),
      shippingDate: null,
      cartItemId: cartItemIds,
      orderDetails,
      type: true,
      shippingFee: shippingFee.value,
    }

    const selectedPaymentMethod = paymentMethods.value.find(pm => pm.id === selectedPaymentMethodId.value)
    if (selectedPaymentMethod?.type === 'ONLINE_PAYMENT') {
      const res = await createVnPayPayment(orderRequest)
      const apiRes = res?.data
      if (apiRes?.status === 200 && apiRes?.data) {
        window.location.href = apiRes.data
      } else {
        const errorMsg = apiRes?.message || 'Không thể tạo thanh toán online. Vui lòng thử lại!'
        toastRef.value?.showToast(errorMsg, 'error')
      }
    } else {
      // COD
      await createOrder(orderRequest)
      toastRef.value?.showToast('Đặt hàng thành công!', 'success')
      router.replace('/orders/success')
    }
  } catch (error) {
    console.error('Lỗi khi đặt hàng:', error)
    const status = error?.response?.status
    const msg = error?.response?.data?.message || error?.message
    if (status === 409) {
      // người thứ 3: coupon đã hết
      toastRef.value?.showToast(msg || 'Mã giảm giá đã hết lượt', 'error')
      removeCoupon()
    } else {
      toastRef.value?.showToast('Có lỗi xảy ra khi đặt hàng. Vui lòng thử lại!', 'error')
    }
  } finally {
    isSubmitting.value = false
  }
}

// ====== ADDRESS MODAL CALLBACK ======
const handleSaveAddress = (addressObj) => {
  const address = addressObj?.data || addressObj
  if (!address) {
    showAddressModal.value = false
    return
  }

  const line = address.line
    || [address.addressDetail, address.wardName, address.districtName, address.provinceName]
        .filter(Boolean).join(', ')

  if (!line) {
    toastRef.value?.showToast('Địa chỉ không đầy đủ thông tin', 'error')
    showAddressModal.value = false
    return
  }

  if (!address.provinceName || !address.districtName || !address.wardName) {
    toastRef.value?.showToast('Địa chỉ không đủ thông tin để tính phí vận chuyển', 'warning')
    addressInput.value = line
    if (user.value) user.value.address = line
    selectedAddressObj.value = null
    showAddressModal.value = false
    return
  }

  addressInput.value = line
  if (user.value) user.value.address = line
  selectedAddressObj.value = address

  toastRef.value?.showToast('Đã cập nhật địa chỉ giao hàng', 'success')
  calculateShippingFeeFromAddress(address)
  showAddressModal.value = false
}
</script>

<template>
  <div class="checkout-container">
    <!-- Loading -->
    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="mt-3 text-muted">Đang tải thông tin thanh toán...</p>
      </div>
    </div>

    <!-- Content -->
    <div v-else>
      <div class="checkout-content">
        <div class="checkout-left">
          <!-- Order Summary -->
          <div class="checkout-card">
            <div class="card-header">
              <h5 class="card-title">
                <i class="fas fa-receipt me-2"></i>
                Đơn hàng
              </h5>
            </div>
            <div class="card-body">
              <div class="order-items">
                <div class="order-item" v-for="(item, idx) in cartItems" :key="item.idCartItem">
                  <div class="item-index">{{ idx + 1 }}</div>
                  <div class="item-image">
                    <img :src="item.productImage" :alt="item.productName" />
                  </div>
                  <div class="item-details">
                    <h6 class="item-name">{{ item.productName }}</h6>
                    <div class="item-variants" v-if="item.size || item.color || item.variantName">
                      <span v-if="item.size" class="variant-tag">
                        <i class="fas fa-ruler me-1"></i>{{ item.size }}
                      </span>
                      <span v-if="item.color" class="variant-tag">
                        <i class="fas fa-palette me-1"></i>{{ item.color }}
                      </span>
                      <span v-if="item.variantName && !item.size && !item.color" class="variant-tag">
                        {{ item.variantName }}
                      </span>
                    </div>
                    <div class="item-price-section">
                      <div class="quantity-section">
                        <span class="quantity-label">Số lượng:</span>
                        <span class="quantity-value">{{ item.quantity }}</span>
                      </div>
                      <div class="price-section">
                        <div v-if="item.valuePromotion && item.valuePromotion > 0 && item.valuePromotion <= 100" class="promotion-price">
                          <span class="original-price">{{ formatPrice(item.price) }} × {{ item.quantity }}</span>
                          <span class="discount-percent">-{{ item.valuePromotion }}%</span>
                          <span class="promotion-price-value">{{ formatPrice(item.price * (1 - item.valuePromotion / 100)) }} × {{ item.quantity }}</span>
                        </div>
                        <span v-else class="unit-price">{{ formatPrice(item.price) }} × {{ item.quantity }}</span>
                        <span class="total-price text-end">{{ formatPrice(getProductTotal(item)) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Payment Methods -->
          <div class="checkout-card">
            <div class="card-header">
              <h5 class="card-title">
                <i class="fas fa-credit-card me-2"></i>
                Phương thức thanh toán
              </h5>
            </div>
            <div class="card-body">
              <template v-if="paymentMethods.length">
                <div class="payment-methods">
                  <div
                    class="payment-method"
                    v-for="pm in paymentMethods"
                    :key="pm.id"
                    :class="{ active: selectedPaymentMethodId === pm.id }"
                    @click="selectedPaymentMethodId = pm.id"
                  >
                    <div class="payment-radio">
                      <input
                        type="radio"
                        :id="'payment-' + pm.id"
                        v-model="selectedPaymentMethodId"
                        :value="pm.id"
                      />
                      <label :for="'payment-' + pm.id"></label>
                    </div>
                    <div class="payment-info">
                      <span class="payment-name">
                        {{ pm.id === 1 ? 'Thanh toán khi nhận hàng' : pm.name }}
                      </span>
                      <i class="fas fa-wallet payment-icon"></i>
                    </div>
                  </div>
                </div>
              </template>
              <div v-else class="empty-state">
                <i class="fas fa-exclamation-triangle"></i>
                <p>Không có phương thức thanh toán khả dụng.</p>
              </div>
            </div>
          </div>

          <!-- Customer Information -->
          <div class="checkout-card">
            <div class="card-header">
              <h5 class="card-title">
                <i class="fas fa-user me-2"></i>
                Thông tin người nhận
              </h5>
            </div>
            <div class="card-body">
              <div class="form-grid">
                <div class="form-group">
                  <label class="form-label">
                    <i class="fas fa-user-circle me-1"></i>
                    Họ và tên
                  </label>
                  <input class="form-control" v-model="fullnameInput" placeholder="Nhập họ tên người nhận" required />
                </div>
                <div class="form-group">
                  <label class="form-label">
                    <i class="fas fa-phone me-1"></i>
                    Số điện thoại
                  </label>
                  <input class="form-control" v-model="phoneInput" placeholder="Nhập số điện thoại" required />
                </div>
                <div class="form-group full-width">
                  <label class="form-label">
                    <i class="fas fa-map-marker-alt me-1"></i>
                    Địa chỉ giao hàng
                  </label>
                  <div class="address-display-container">
                    <div class="default-address-display">
                      <input class="form-control" :value="addressInput || 'Chưa có địa chỉ mặc định'" readonly />
                    </div>
                    <button type="button" class="btn-address" @click="showAddressModal = true">
                      <i class="fas fa-list me-1"></i>
                      Danh sách địa chỉ
                    </button>
                  </div>
                </div>
                <div class="form-group full-width">
                  <label class="form-label">
                    <i class="fas fa-envelope me-1"></i>
                    Email
                  </label>
                  <input class="form-control" :value="user?.email" readonly />
                </div>
              </div>
            </div>
          </div>

          <!-- Order Note -->
          <div class="checkout-card">
            <div class="card-header">
              <h5 class="card-title">
                <i class="fas fa-sticky-note me-2"></i>
                Ghi chú đơn hàng
              </h5>
            </div>
            <div class="card-body">
              <textarea class="form-control" v-model="noteInput" rows="4"
                        placeholder="Ghi chú thêm cho đơn hàng (không bắt buộc)"></textarea>
            </div>
          </div>
        </div>

        <!-- Right column -->
        <div class="checkout-right">
          <div class="sticky-coupon-group">
            <!-- Coupon -->
            <div class="checkout-card shadow-sm rounded mb-3">
              <div class="card-header bg-white border-bottom">
                <h5 class="card-title mb-0">
                  <i class="fas fa-ticket-alt me-2 text-primary"></i>
                  Mã giảm giá
                </h5>
                <button
                  class="btn btn-outline-primary btn-sm"
                  @click="toggleCouponList"
                  :disabled="couponListLoading"
                >
                  <i class="fas fa-list me-1"></i>
                  {{ showCouponList ? 'Ẩn danh sách' : 'Xem tất cả' }}
                  <span v-if="couponListLoading" class="spinner-border spinner-border-sm ms-1"></span>
                </button>
              </div>
              <div class="card-body">
                <!-- Danh sách mã giảm giá khả dụng -->
                <div v-if="showCouponList" class="available-coupons mb-3">
                  <div v-if="couponListLoading" class="text-center py-3">
                    <div class="spinner-border text-primary" role="status">
                      <span class="visually-hidden">Loading...</span>
                    </div>
                    <p class="mt-2 text-muted">Đang tải mã giảm giá...</p>
                  </div>

                  <div v-else-if="availableCoupons.length === 0" class="text-center py-3">
                    <i class="fas fa-ticket-alt text-muted" style="font-size: 2rem;"></i>
                    <p class="mt-2 text-muted">Không có mã giảm giá khả dụng</p>
                  </div>

                  <div v-else class="coupon-list">
                    <div
                      v-for="(coupon, index) in availableCoupons"
                      :key="coupon.id"
                      class="coupon-item"
                      :class="{ 'best-coupon': index === 0 }"
                      @click="applyCouponFromList(coupon)"
                    >
                      <div class="coupon-header">
                        <div class="coupon-name">
                          {{ coupon.name }}
                          <span v-if="index === 0" class="best-badge">
                            <i class="fas fa-star me-1"></i>Tốt nhất
                          </span>
                        </div>
                        <div class="coupon-badge">
                          <span v-if="coupon.type" class="badge bg-primary">
                            {{ coupon.value }}%
                          </span>
                          <span v-else class="badge bg-success">
                            {{ formatPrice(coupon.value) }}
                          </span>
                        </div>
                      </div>

                      <div class="coupon-details">
                        <div class="coupon-condition">
                          <i class="fas fa-info-circle me-1"></i>
                          Đơn hàng tối thiểu: {{ formatPrice(coupon.condition) }}
                        </div>

                        <div class="coupon-discount">
                          <strong>Tiết kiệm: {{ formatPrice(coupon.actualDiscount) }}</strong>
                          <span v-if="coupon.type && coupon.valueLimit" class="text-muted ms-2">
                            (Tối đa: {{ formatPrice(coupon.valueLimit) }})
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="!couponPreview" class="d-flex gap-2 align-items-center mb-3">
                  <div class="input-group mb-2">
                    <input class="form-control" v-model="couponInput" placeholder="Nhập mã giảm giá"
                           :disabled="couponLoading" @keyup.enter="handleApplyCoupon" />
                    <button class="btn btn-primary btn-sm" :disabled="couponLoading" @click="handleApplyCoupon">
                      <span v-if="couponLoading" class="spinner-border spinner-border-sm me-2"></span>
                      <span v-if="couponLoading">Đang kiểm tra...</span>
                      <span v-else><i class="fas fa-plus me-1"></i>Áp dụng</span>
                    </button>
                  </div>
                </div>

                <div v-if="couponPreview"
                     class="alert alert-info d-flex align-items-center justify-content-between py-2 px-3 mb-2">
                  <span>
                    Đã áp dụng mã: <b class="text-primary">{{ couponPreview.name || couponPreview.code }}</b>
                    <span v-if="couponPreview.type">({{ couponPreview.value }}%)</span>
                    <span v-else>({{ couponPreview.value.toLocaleString('vi-VN') }}₫)</span>
                  </span>
                  <button @click="removeCoupon" class="btn btn-link btn-sm text-danger">Hủy</button>
                </div>

                <div v-if="couponSuccess" class="alert alert-success mt-2 d-flex align-items-center">
                  <i class="fas fa-check-circle me-2"></i> {{ couponSuccess }}
                </div>

                <div v-if="couponError" class="alert alert-danger mt-2 d-flex align-items-center">
                  <i class="fas fa-exclamation-circle me-2"></i> {{ couponError }}
                </div>

                <div class="order-summary border-top pt-3 mt-3">
                  <div class="summary-row summary-row--line d-flex justify-content-between align-items-center">
                    <span class="summary-label">Tổng tiền hàng</span>
                    <strong class="summary-amount">{{ formatPrice(totalAfterProductPromotion) }}</strong>
                  </div>

                  <div class="summary-row summary-row--shipping d-flex justify-content-between align-items-start">
                    <span class="summary-label">
                      <i class="fas fa-truck me-1"></i>Phí vận chuyển
                      <span v-if="isCalculatingShipping" class="ms-1 text-muted">
                        <small><i class="fas fa-spinner fa-spin"></i> Đang tính...</small>
                      </span>
                    </span>
                    <strong class="summary-amount">{{ formatPrice(shippingFee) }}</strong>
                  </div>
                  <div v-if="shippingError" class="summary-row text-danger">
                    <small><i class="fas fa-exclamation-circle me-1"></i>{{ shippingError }}</small>
                  </div>

                  <div class="summary-row summary-row--discount d-flex justify-content-between align-items-center"
                       v-if="couponDiscount > 0">
                    <span class="summary-label"><i class="fas fa-ticket-alt me-1"></i>Số tiền trừ (mã giảm giá)</span>
                    <strong class="summary-amount text-danger">-{{ formatPrice(couponDiscount) }}</strong>
                  </div>

                  <div class="summary-row d-flex justify-content-between align-items-center border-top pt-2 mt-2">
                    <span class="summary-total-label">Tổng tiền đơn hàng</span>
                    <strong class="summary-total-amount">{{ formatPrice(finalTotal) }}</strong>
                  </div>
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div class="checkout-card shadow-sm rounded">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center gap-2">
                  <button class="btn btn-outline-secondary" @click="() => router.back()">
                    <i class="fas fa-arrow-left me-2"></i> Quay lại
                  </button>
                  <button class="btn btn-primary btn-checkout flex-grow-1"
                          :disabled="isSubmitting || !paymentMethods.length"
                          @click="handleCheckout"
                          style="font-size:1.15rem;font-weight:600;">
                    <span v-if="isSubmitting">
                      <span class="spinner-border spinner-border-sm me-2"></span> Đang xử lý...
                    </span>
                    <span v-else>
                      <i class="fas fa-check me-2"></i> Đặt hàng
                    </span>
                  </button>
                </div>
              </div>
            </div>
          </div> <!-- /sticky group -->
        </div> <!-- /right -->
      </div> <!-- /content -->
    </div> <!-- /else -->
  </div>

  <!-- Toast -->
  <ShowToastComponent ref="toastRef" />

  <!-- Address Modal -->
  <ListAddressModal
    v-if="showAddressModal"
    :userId="user?.id"
    @close="showAddressModal = false"
    @save="handleSaveAddress"
  />
</template>



<style scoped>
/* Loading */
.loading-container {
  display: flex; justify-content: center; align-items: center; min-height: 60vh;
}
.loading-spinner { text-align: center; }

/* Main */
.checkout-container {
  max-width: 1330px;
  margin: 2rem auto;
  padding: 0 1rem;
  min-height: 90vh;
}

/* Header */
.checkout-header {
  text-align: center;
  margin-bottom: 2rem;
  background: white;
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}
.checkout-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 1.5rem;
}
.checkout-steps { display: flex; justify-content: center; gap: 2rem; margin-bottom: 2rem; }
.step { display: flex; flex-direction: column; align-items: center; position: relative; }
.step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 15px;
  right: -1.5rem;
  width: 2rem;
  height: 2px;
  background: #e2e8f0;
}
.step.completed::after, .step.active::after { background: #1F2937; }
.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #6c757d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
  margin-bottom: 0.5rem;
}
.step.completed .step-number { background: #059669; color: #fff; }
.step.active .step-number { background: #1F2937; color: #fff; }
.step-text { font-size: 0.8rem; color: #6c757d; font-weight: 500; }
.step.completed .step-text, .step.active .step-text { color: #1f2937; }

/* Layout */
.checkout-content { display: grid; grid-template-columns: 1fr 400px; gap: 2rem; }
.checkout-left { display: flex; flex-direction: column; gap: 1.5rem; }
.checkout-right { position: relative; display: flex; flex-direction: column; gap: 1.5rem; }
.sticky-coupon-group { position: sticky; top: 20px; z-index: 10; }

/* Cards */
.checkout-card {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
}

.card-header {
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  padding: 1.5rem 1.5rem;
}
.card-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #1f2937;
}
.card-body { padding: 1.5rem; }

/* Payment Methods */
.payment-methods { display: flex; flex-direction: column; gap: .75rem; }
.payment-method {
  display: flex;
  align-items: center;
  padding: 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}

.payment-method.active {
  border-color: #1F2937;
  background: #f1f5f9;
  box-shadow: 0 4px 12px rgba(31, 41, 55, 0.15);
}
.payment-radio { margin-right: 1rem; }
.payment-radio input {
  width: 18px;
  height: 18px;
  accent-color: #1F2937;
}
.payment-info { display: flex; justify-content: space-between; align-items: center; flex: 1; }
.payment-name { font-weight: 600; color: #1f2937; font-size: 0.9rem; }
.payment-icon { color: #6c757d; font-size: 1.1rem; }

/* Form */
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-group.full-width { grid-column: 1 / -1; }
.form-label {
  display: block;
  margin-bottom: .5rem;
  font-weight: 600;
  color: #374151;
  font-size: 0.9rem;
}
.form-control {
  width: 100%;
  padding: .875rem;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 0.9rem;
  transition: all 0.3s ease;
  background: #fff;
}
.form-control:focus {
  outline: none;
  border-color: #1F2937;
  box-shadow: 0 0 0 3px rgba(31, 41, 55, 0.1);
}
.form-control:readonly {
  background: #f8f9fa;
  color: #6c757d;
}

/* Address */
.address-display-container { display: flex; align-items: center; gap: .5rem; margin-bottom: .5rem; }
.default-address-display { flex: 1; }
.btn-address {
  background: #1F2937;
  border: 2px solid #1F2937;
  border-radius: 10px;
  padding: .75rem 1rem;
  font-size: .875rem;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  font-weight: 600;
}
.btn-address:hover {
  background: #252e3e;
  border-color: #252e3e;
  box-shadow: 0 8px 25px rgba(31, 41, 55, 0.3);
}

/* Order items */
.order-items { margin-bottom: 1.5rem; }
.order-item {
  display: flex;
  gap: 1rem;
  padding: 1rem 0;
  border-bottom: 1px solid #e2e8f0;
}
.order-item:last-child { border-bottom: none; }
.item-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: #1F2937;
  color: #fff;
  border-radius: 50%;
  font-size: .8rem;
  font-weight: 700;
  flex-shrink: 0;
}
.item-image { position: relative; flex-shrink: 0; }
.item-image img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 10px;
  border: 2px solid #e2e8f0;
}
.item-details { flex: 1; }
.item-name {
  font-size: .9rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 .5rem;
  line-height: 1.3;
}
.item-variants { display: flex; gap: .5rem; margin-bottom: .5rem; }
.variant-tag {
  background: #f1f5f9;
  color: #374151;
  padding: .25rem .5rem;
  border-radius: 6px;
  font-size: .8rem;
  font-weight: 500;
}
.item-price-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: .5rem;
}
.quantity-section { display: flex; align-items: center; gap: .5rem; }
.quantity-label { font-size: .85rem; color: #6c757d; }
.quantity-value { font-weight: 600; color: #1f2937; }
.price-section { display: flex; flex-direction: column; align-items: flex-end; }
.unit-price { font-size: .85rem; color: #6c757d; }
.total-price { font-weight: 600; color: #1f2937; }
.promotion-price { display: flex; flex-direction: column; align-items: flex-end; }
.original-price {
  text-decoration: line-through;
  font-size: .8rem;
  color: #6c757d;
  margin-bottom: .25rem;
}
.discount-percent {
  font-size: .8rem;
  color: #dc3545;
  font-weight: 600;
  margin-bottom: .25rem;
}
.promotion-price-value {
  font-weight: 600;
  color: #1f2937;
  font-size: 1rem;
}

/* Summary */
.order-summary {
  border-top: 1px solid #e2e8f0;
  padding-top: 1rem;
  margin-bottom: 1.5rem;
}
.order-summary .summary-row {
  margin-bottom: .5rem;
  font-size: 0.9rem;
}
.order-summary .summary-row strong {
  min-width: 100px;
  display: inline-block;
  text-align: right;
}

/* Buttons */
.btn-checkout {
  padding: 1rem 2rem;
  font-size: 1.1rem;
  font-weight: 700;
  background: #1F2937;
  border: 2px solid #1F2937;
  border-radius: 35px;
  transition: all 0.3s ease;
  color: white;
}
.btn-checkout:hover {
  background: #252e3e;
  border-color: #252e3e;
  box-shadow: 0 8px 25px rgba(31, 41, 55, 0.3);
}
.btn-checkout:disabled {
  background: #9ca3af;
  border-color: #9ca3af;
  transform: none;
  box-shadow: none;
}

.btn-outline-secondary {
  padding: 1rem 2rem;
  font-size: 1rem;
  font-weight: 600;
  background: white;
  color: #6b7280;
  border: 2px solid #e2e8f0;
  border-radius: 35px;
  transition: all 0.3s ease;
}

.btn-outline-secondary:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* Responsive */
@media (max-width: 992px) {
  .checkout-content { grid-template-columns: 1fr; }
  .sticky-coupon-group { position: static; }
}

/* Available Coupons */
.available-coupons {
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  padding: 1rem;
  max-height: 400px;
  overflow-y: auto;
}

.available-coupons::-webkit-scrollbar {
  width: 6px;
}

.available-coupons::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.available-coupons::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}


.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.coupon-item {
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.75rem;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.coupon-item:hover {
  border-color: #1F2937;
  background-color: #f8fafc;
  box-shadow: 0 4px 12px rgba(31, 41, 55, 0.1);
}

/* Coupon tốt nhất */
.coupon-item.best-coupon {
  border: 2px solid #f59e0b;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.2);
}

.coupon-item.best-coupon:hover {
  border-color: #d97706;
  background: linear-gradient(135deg, #fde68a, #fcd34d);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.3);
}

.best-badge {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
  font-size: 0.7rem;
  font-weight: 600;
  margin-left: 0.5rem;
  display: inline-block;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

.coupon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.coupon-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 0.9rem;
  flex: 1;
  margin-right: 0.5rem;
  display: flex;
  align-items: center;
}

.coupon-badge .badge {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-weight: 500;
}

.coupon-details {
  margin-bottom: 0;
}

.coupon-condition {
  font-size: 0.8rem;
  color: #6c757d;
  margin-bottom: 0.25rem;
}

.coupon-discount {
  font-size: 0.85rem;
  color: #059669;
  font-weight: 500;
}

/* Card header với button */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .btn-outline-primary {
  border-radius: 10px;
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  font-weight: 600;
  border: 2px solid #1F2937;
  color: #1F2937;
  transition: all 0.3s ease;
  background: white;
}

/* Alert styles */
.alert {
  border-radius: 10px;
  border: 2px solid;
  padding: 0.75rem 1rem;
}

.alert-info {
  background-color: #eff6ff;
  border-color: #3b82f6;
  color: #1e40af;
}

.alert-success {
  background-color: #f0fdf4;
  border-color: #22c55e;
  color: #15803d;
}

.alert-danger {
  background-color: #fef2f2;
  border-color: #ef4444;
  color: #b91c1c;
}

/* Input group */
.input-group {
  border-radius: 10px;
  overflow: hidden;
}

.input-group .form-control {
  border-radius: 10px 0 0 10px;
  border-right: none;
}

.input-group .btn {
  border-radius: 0 10px 10px 0;
  border-left: none;
  background: #1F2937;
  border-color: #1F2937;
  color: white;
  font-weight: 600;
  transition: all 0.3s ease;
}

.input-group .btn:hover {
  background: #252e3e;
  border-color: #252e3e;
}

/* Empty state */
.empty-state {
  text-align: center;
  padding: 2rem;
  color: #6c757d;
}

.empty-state i {
  font-size: 2rem;
  margin-bottom: 1rem;
  color: #9ca3af;
}
</style>
