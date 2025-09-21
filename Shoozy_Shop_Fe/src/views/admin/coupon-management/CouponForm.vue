<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import couponApi from '@/service/CouponApi'
import { useRouter } from 'vue-router'
import ShowToastComponent from '@/components/ShowToastComponent.vue'

const props = defineProps({
  id: [String, Number],
  mode: { type: String, default: 'create' }
})
const emit = defineEmits(['submit', 'back'])
const router = useRouter()
const toastRef = ref(null)

const CHARACTER_LIMIT = 300

// ===== RÀNG BUỘC CHÍNH SÁCH (có thể đưa ra config hệ thống) =====
const MAX_PERCENT = 30
const MIN_PAYABLE_VND = 1000
const MAX_ORDER_AMOUNT = 100000000
const MIN_ORDER_MIN = 10000

const form = ref({
  name: '',
  code: '',
  type: true, // Boolean: true - phần trăm, false - số tiền
  value: null,
  valueLimit: null,
  condition: 0,
  quantity: null,
  startDate: '',         // camelCase
  expirationDate: '',    // camelCase
  description: ''
})
const initialForm = ref({})
const errors = ref({})
const touched = ref({})
const submitAttempted = ref(false)
const loading = ref(false)
const characterCount = computed(() => form.value.description.length)

const isCreateMode = computed(() => props.mode === 'create')
const isUpdateMode = computed(() => props.mode === 'update')
const isDetailMode = computed(() => props.mode === 'detail')

const headerTitle = computed(() =>
  isCreateMode.value ? 'Tạo coupon mới'
    : isUpdateMode.value ? 'Chỉnh sửa coupon'
      : 'Chi tiết coupon'
)
const headerDesc = computed(() =>
  isCreateMode.value ? 'Tạo mới mã giảm giá/coupon cho khách hàng'
    : isUpdateMode.value ? 'Chỉnh sửa, cập nhật coupon'
      : 'Xem chi tiết mã coupon'
)

function validate (f = form.value) {
  const err = {}
  const isPercent = f.type === true || f.type === 'true'

  // Name
  if (!f.name?.trim()) err.name = 'Vui lòng nhập tên coupon'
  else if (f.name.length < 3) err.name = 'Tên coupon phải có ít nhất 3 ký tự'
  else if (f.name.length > 100) err.name = 'Tên coupon không được vượt quá 100 ký tự'

  // Code
  if (!f.code?.trim()) err.code = 'Vui lòng nhập mã coupon'
  else if (f.code.length < 4) err.code = 'Mã coupon phải có ít nhất 4 ký tự'
  else if (f.code.length > 20) err.code = 'Mã coupon không được vượt quá 20 ký tự'
  else if (!/^[A-Za-z0-9_-]+$/.test(f.code)) err.code = 'Mã coupon chỉ được chứa chữ, số, "-", "_"'

  // Dates
  if (!f.startDate) err.startDate = 'Vui lòng chọn ngày bắt đầu'
  if (!f.expirationDate) err.expirationDate = 'Vui lòng chọn ngày kết thúc'
  if (f.startDate && f.expirationDate && f.startDate > f.expirationDate) {
    err.expirationDate = 'Ngày kết thúc phải sau ngày bắt đầu'
  }
  if (isCreateMode.value && f.startDate) {
    const startOnly = new Date(new Date(f.startDate).toDateString())
    const todayOnly = new Date(new Date().toDateString())
    if (startOnly < todayOnly) err.startDate = 'Ngày bắt đầu không được trong quá khứ'
  }

  // Description
  if (f.description && f.description.length > CHARACTER_LIMIT) {
    err.description = `Mô tả không được vượt quá ${CHARACTER_LIMIT} ký tự`
  }

  // Quantity
  if (f.quantity !== null && f.quantity !== '') {
    if (isNaN(f.quantity)) err.quantity = 'Số lượng phải là số'
    else if (!Number.isInteger(Number(f.quantity))) err.quantity = 'Số lượng phải là số nguyên'
    else if (f.quantity < 0) err.quantity = 'Số lượng không được âm'
    else if (f.quantity > 1000000) err.quantity = 'Số lượng không được vượt quá 1.000.000'
  }

  // Condition (orderMin)
  if (f.condition === null || f.condition === '' || isNaN(f.condition)) {
    err.condition = 'Điều kiện đơn tối thiểu phải là số'
  } else if (f.condition < MIN_ORDER_MIN) {
    err.condition = `Điều kiện đơn tối thiểu phải ≥ ${MIN_ORDER_MIN.toLocaleString()}đ`
  } else if (f.condition > MAX_ORDER_AMOUNT) {
    err.condition = `Điều kiện đơn tối thiểu không được vượt quá ${MAX_ORDER_AMOUNT.toLocaleString()}đ`
  }

  // Value + cross-field guards
  if (f.value === null || f.value === '' || isNaN(f.value)) {
    err.value = 'Vui lòng nhập giá trị'
  } else if (isPercent) {
    // % coupon
    if (f.value < 1 || f.value > MAX_PERCENT) {
      err.value = `Giá trị % phải từ 1% đến ${MAX_PERCENT}%`
    }
    // cap (bắt buộc)
    if (f.valueLimit === null || f.valueLimit === '' || f.valueLimit === undefined) {
      err.valueLimit = 'Bắt buộc nhập “Giới hạn giá trị giảm tối đa (VNĐ)”'
    } else if (isNaN(f.valueLimit)) {
      err.valueLimit = 'Giới hạn phải là số'
    } else if (f.valueLimit < 1000) {
      err.valueLimit = 'Giới hạn phải từ 1.000đ trở lên'
    } else if (f.valueLimit > MAX_ORDER_AMOUNT) {
      err.valueLimit = `Giới hạn không được vượt quá ${MAX_ORDER_AMOUNT.toLocaleString()}đ`
    }

    // Cross-field: orderMin ≥ cap + minPayable
    if (!err.condition && !err.valueLimit) {
      if (Number(f.condition) < Number(f.valueLimit) + MIN_PAYABLE_VND) {
        err.condition = `Đơn tối thiểu phải ≥ (giới hạn giảm) + ${MIN_PAYABLE_VND.toLocaleString()}đ`
      }

      // Guard tổng quát: min(orderMin*%/100, cap) ≤ orderMin - minPayable
      const orderMin = Number(f.condition)
      const percent = Number(f.value)
      const cap = Number(f.valueLimit)
      const byPercentAtMin = Math.floor((orderMin * percent) / 100) // floor để an toàn
      const effectiveAtMin = Math.min(byPercentAtMin, cap)
      const maxAllow = orderMin - MIN_PAYABLE_VND
      if (effectiveAtMin > maxAllow) {
        err.value = `Cấu hình này khiến đơn tối thiểu còn dưới ${MIN_PAYABLE_VND.toLocaleString()}đ sau giảm`
      }

      // (Khuyến nghị) cap không vượt % tại đơn tối thiểu
      if (cap > byPercentAtMin) {
        err.valueLimit = `Giới hạn tối đa không được vượt ${byPercentAtMin.toLocaleString()}đ (tại đơn tối thiểu)`
      }
    }
  } else {
    // Fixed amount
    if (f.value < 1000) err.value = 'Số tiền phải từ 1.000đ trở lên'
    else if (f.value > MAX_ORDER_AMOUNT) err.value = `Số tiền không được vượt quá ${MAX_ORDER_AMOUNT.toLocaleString()}đ`

    // Cross-field: orderMin ≥ amount + minPayable
    if (!err.condition && !err.value) {
      if (Number(f.condition) < Number(f.value) + MIN_PAYABLE_VND) {
        err.condition = `Đơn tối thiểu phải ≥ (số tiền giảm) + ${MIN_PAYABLE_VND.toLocaleString()}đ`
      }
    }
  }

  return err
}
const isFormValid = computed(() => Object.keys(validate()).length === 0)

watch(form, () => {
  errors.value = validate()
}, { deep: true })

function handleBlur (field) { touched.value[field] = true }
function handleInput (field) { errors.value = validate() }

function generateCode () {
  const pool = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let code = ''
  for (let i = 0; i < 8; i++) code += pool.charAt(Math.floor(Math.random() * pool.length))
  form.value.code = code
  handleInput('code')
}

const discountPreview = computed(() => {
  if (!form.value.value) return ''
  return form.value.type
    ? `Giảm ${form.value.value}%`
    : `Giảm ${Number(form.value.value).toLocaleString()}đ`
})

function handleTypeChange (e) {
  if (e && e.target) {
    const val = e.target.value
    form.value.type = val === 'true' || val === true
  } else {
    form.value.type = form.value.type === true || form.value.type === 'true'
  }
  form.value.value = null
  if (form.value.type === false) form.value.valueLimit = null
  handleInput('type')
}

async function focusFirstError () {
  await nextTick()
  const keys = Object.keys(errors.value)
  if (keys.length) {
    const el = document.querySelector(`[name="${keys[0]}"]`)
    el && el.focus()
  }
}

async function handleSubmit () {
  submitAttempted.value = true
  errors.value = validate()
  if (!isFormValid.value) {
    toastRef.value?.showToast('Vui lòng kiểm tra lại thông tin form', 'error')
    focusFirstError()
    return
  }

  // Additional validation to ensure dates are not empty
  if (!form.value.startDate || !form.value.expirationDate) {
    toastRef.value?.showToast('Vui lòng chọn ngày bắt đầu và ngày kết thúc', 'error')
    return
  }

  form.value.type = form.value.type === true || form.value.type === 'true'
  loading.value = true

  try {
    // Convert to snake_case and ensure dates are properly formatted, giữ nguyên múi giờ địa phương
    let startDate = form.value.startDate || null
    let expirationDate = form.value.expirationDate || null

    // Loại 'Z' nếu có
    if (startDate && startDate.includes('Z')) startDate = startDate.replace('Z', '')
    if (expirationDate && expirationDate.includes('Z')) expirationDate = expirationDate.replace('Z', '')

    // Định dạng YYYY-MM-DDTHH:mm nếu thiếu 'T'
    if (startDate && !startDate.includes('T')) {
      const date = new Date(startDate)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      startDate = `${year}-${month}-${day}T${hours}:${minutes}`
    }
    if (expirationDate && !expirationDate.includes('T')) {
      const date = new Date(expirationDate)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      expirationDate = `${year}-${month}-${day}T${hours}:${minutes}`
    }

    const submitData = {
      ...form.value,
      start_date: startDate,
      expiration_date: expirationDate,
      value_limit: form.value.valueLimit || null
    }

    // Không gửi code khi edit để bảo toàn mã cũ
    if (isUpdateMode.value) delete submitData.code

    // Remove camelCase fields
    delete submitData.startDate
    delete submitData.expirationDate
    delete submitData.valueLimit

    if (!submitData.start_date) throw new Error('Ngày bắt đầu không được để trống')
    if (!submitData.expiration_date) throw new Error('Ngày kết thúc không được để trống')

    let res
    if (isCreateMode.value) {
      res = await couponApi.createCoupon(submitData)
      console.log('Create response:', res)
    } else if (isUpdateMode.value) {
      res = await couponApi.updateCoupon(props.id, submitData)
    }

    if (res && res.data && res.data.status === 200) {
      submitAttempted.value = false
      if (isCreateMode.value) toastRef.value?.showToast('Tạo coupon thành công!', 'success')
      else toastRef.value?.showToast('Cập nhật coupon thành công!', 'success')
      setTimeout(() => { router.push('/admin/coupons') }, 1500)
    } else {
      toastRef.value?.showToast('Có lỗi khi lưu coupon: ' + (res?.data?.message || 'Unknown error'), 'error')
      loading.value = false
      return
    }
  } catch (e) {
    if (e?.response?.data?.message?.includes('đã tồn tại')) {
      errors.value.code = 'Mã coupon đã tồn tại'
      touched.value.code = true
      toastRef.value?.showToast('Mã coupon đã tồn tại, vui lòng chọn mã khác', 'error')
      await focusFirstError()
    } else {
      toastRef.value?.showToast('Có lỗi khi lưu coupon: ' + (e?.response?.data?.message || e.message), 'error')
    }
    loading.value = false
  } finally {
    // no-op
  }
}

// Map data từ BE trả về sang camelCase cho form
function mapCouponData (d) {
  const start = d.startDate || d.start_date
  const end = d.expirationDate || d.expiration_date

  // start
  let formattedStartDate = ''
  if (start) {
    try {
      const startDate = new Date(start)
      if (!isNaN(startDate.getTime())) {
        const year = startDate.getFullYear()
        const month = String(startDate.getMonth() + 1).padStart(2, '0')
        const day = String(startDate.getDate()).padStart(2, '0')
        const hours = String(startDate.getHours()).padStart(2, '0')
        const minutes = String(startDate.getMinutes()).padStart(2, '0')
        formattedStartDate = `${year}-${month}-${day}T${hours}:${minutes}`
      }
    } catch (e) { console.error('Lỗi khi xử lý ngày bắt đầu:', e) }
  }

  // end
  let formattedEndDate = ''
  if (end) {
    try {
      const endDate = new Date(end)
      if (!isNaN(endDate.getTime())) {
        const year = endDate.getFullYear()
        const month = String(endDate.getMonth() + 1).padStart(2, '0')
        const day = String(endDate.getDate()).padStart(2, '0')
        const hours = String(endDate.getHours()).padStart(2, '0')
        const minutes = String(endDate.getMinutes()).padStart(2, '0')
        formattedEndDate = `${year}-${month}-${day}T${hours}:${minutes}`
      }
    } catch (e) { console.error('Lỗi khi xử lý ngày kết thúc:', e) }
  }

  return {
    ...d,
    valueLimit: d.valueLimit ?? d.value_limit ?? null,
    startDate: formattedStartDate,
    expirationDate: formattedEndDate,
    type: d.type !== undefined ? (d.type === true || d.type === 'true') : true
  }
}

async function loadCoupon () {
  if (props.id && (isUpdateMode.value || isDetailMode.value)) {
    loading.value = true
    try {
      const res = await couponApi.getCouponById(props.id)
      if (res.data && res.data.status === 200) {
        Object.assign(form.value, mapCouponData(res.data.data))
        initialForm.value = JSON.parse(JSON.stringify(form.value))
        if (isUpdateMode.value) {
          toastRef.value?.showToast('Đã tải thông tin coupon thành công!', 'success')
        }
      } else {
        toastRef.value?.showToast('Không tìm thấy coupon: ' + (res.data?.message || 'Unknown error'), 'error')
      }
    } catch (e) {
      toastRef.value?.showToast('Không tìm thấy coupon', 'error')
    } finally {
      loading.value = false
    }
  } else {
    Object.assign(form.value, {
      name: '',
      code: '',
      type: true,
      value: null,
      valueLimit: null,
      condition: 0,
      quantity: null,
      startDate: '',
      expirationDate: '',
      description: ''
    })
    initialForm.value = JSON.stringify(form.value)
  }
}
onMounted(loadCoupon)

function handleKeydown (e) {
  if (isDetailMode.value) return
  if (e.ctrlKey && e.key.toLowerCase() === 's') {
    e.preventDefault()
    handleSubmit()
  }
  if (e.key === 'Escape') {
    goBack()
  }
}
function goBack () { router.push('/admin/coupons') }
onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <div class="min-vh-100 bg-light bg-gradient pt-2 pb-5">
    <div class="container-fluid px-4">
      <!-- Header -->
      <div class="row justify-content-center mb-4">
        <div class="col-12 px-0">
          <div class="card glass-card shadow-lg border-0 position-relative p-3">
            <div class="glass-gradient position-absolute top-0 start-0 w-100 h-100 rounded"></div>
            <div class="d-flex align-items-center position-relative" style="z-index:2">
              <div class="me-4">
                <div class="icon-glass d-flex align-items-center justify-content-center">
                  <i :class="{
                    'fas fa-ticket-alt text-white fs-3': isCreateMode,
                    'fas fa-edit text-white fs-3': isUpdateMode,
                    'fas fa-eye text-white fs-3': isDetailMode
                  }"></i>
                </div>
              </div>
              <div class="flex-grow-1">
                <h1 class="fw-bold fs-3 mb-1 text-gradient">{{ headerTitle }}</h1>
                <div class="text-secondary">{{ headerDesc }}</div>
                <div v-if="discountPreview && form.value" class="badge bg-success-subtle text-success mt-2">
                  {{ discountPreview }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Main Form -->
      <div class="row justify-content-center">
        <div class="col-12 px-0">
          <div class="card glass-card border-0 shadow p-3 mb-5">
            <form @submit.prevent="handleSubmit" novalidate>
              <div class="row g-4">
                <!-- Name -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-tag me-2"></i>Tên coupon
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    v-model="form.name"
                    :readonly="isDetailMode"
                    :class="{
                      'is-invalid': errors.name && (touched.name || submitAttempted),
                      'is-valid': !errors.name && (touched.name || submitAttempted) && form.name
                    }"
                    placeholder="Nhập tên coupon"
                    @blur="handleBlur('name')"
                    @input="handleInput('name')"
                    maxlength="100"
                  />
                  <div v-if="errors.name && (touched.name || submitAttempted)" class="invalid-feedback">
                    {{ errors.name }}
                  </div>
                  <div class="form-text">{{ form.name.length }}/100 ký tự</div>
                </div>

                <!-- Code -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-barcode me-2"></i>Mã coupon
                  </label>
                  <div class="input-group">
                    <input
                      type="text"
                      class="form-control text-uppercase"
                      v-model="form.code"
                      :readonly="isDetailMode || isUpdateMode"
                      :class="{
                        'is-invalid': errors.code && (touched.code || submitAttempted),
                        'is-valid': !errors.code && (touched.code || submitAttempted) && form.code,
                        'bg-light': isUpdateMode
                      }"
                      placeholder="Nhập mã coupon"
                      @blur="handleBlur('code')"
                      @input="handleInput('code')"
                      maxlength="20"
                      style="text-transform: uppercase;"
                    />
                    <button
                      v-if="!isDetailMode && !isUpdateMode"
                      type="button"
                      class="btn btn-outline-primary"
                      @click="generateCode"
                      tabindex="-1"
                      title="Tạo mã ngẫu nhiên"
                    >
                      <i class="fas fa-magic"></i>
                    </button>
                  </div>
                  <div v-if="errors.code && (touched.code || submitAttempted)" class="invalid-feedback d-block">
                    {{ errors.code }}
                  </div>
                  <div class="form-text">
                    <span v-if="isCreateMode">{{ form.code.length }}/20 ký tự</span>
                    <span v-else class="text-muted">Mã coupon không thể thay đổi sau khi tạo</span>
                  </div>
                </div>

                <!-- Type -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-percent me-2"></i>Loại giảm giá
                  </label>
                  <div class="input-group">
                    <select class="form-select" :disabled="isDetailMode"
                            :value="form.type === true ? 'true' : 'false'"
                            @change="handleTypeChange">
                      <option value="true">Phần trăm (%)</option>
                      <option value="false">Số tiền (VNĐ)</option>
                    </select>
                  </div>
                </div>

                <!-- Value -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-money-bill-wave me-2"></i>Giá trị coupon
                  </label>
                  <div class="input-group">
                    <input
                      type="number"
                      class="form-control"
                      v-model.number="form.value"
                      :min="form.type ? 1 : 1000"
                      :max="form.type ? MAX_PERCENT : MAX_ORDER_AMOUNT"
                      step="1"
                      :readonly="isDetailMode"
                      :class="{
                        'is-invalid': errors.value && (touched.value || submitAttempted),
                        'is-valid': !errors.value && (touched.value || submitAttempted) && form.value !== null && form.value !== ''
                      }"
                      placeholder="0"
                      @blur="handleBlur('value')"
                      @input="handleInput('value')"
                    />
                    <span class="input-group-text">{{ form.type ? '%' : 'VNĐ' }}</span>
                  </div>
                  <div v-if="errors.value && (touched.value || submitAttempted)" class="invalid-feedback">
                    {{ errors.value }}
                  </div>
                  <div class="form-text" v-if="form.type">Tối đa {{ MAX_PERCENT }}%</div>
                  <div class="form-text" v-else>Tối thiểu 1.000đ</div>
                </div>

                <!-- Giới hạn giảm tối đa -->
                <div class="col-md-6" v-if="form.type">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-balance-scale-left me-2"></i>Giới hạn giá trị giảm tối đa (VNĐ)
                  </label>
                  <input
                    type="number"
                    class="form-control"
                    v-model.number="form.valueLimit"
                    :readonly="isDetailMode"
                    min="1000"
                    step="1000"
                    placeholder="VD: 50000"
                    :class="{
                      'is-invalid': errors.valueLimit && (touched.valueLimit || submitAttempted),
                      'is-valid': !errors.valueLimit && (touched.valueLimit || submitAttempted) && form.valueLimit !== null && form.valueLimit !== ''
                    }"
                    @blur="handleBlur('valueLimit')"
                    @input="handleInput('valueLimit')"
                  />
                  <div v-if="errors.valueLimit && (touched.valueLimit || submitAttempted)" class="invalid-feedback">
                    {{ errors.valueLimit }}
                  </div>
                  <div class="form-text">
                    Bắt buộc khi giảm theo %. Tối thiểu 1.000đ.
                  </div>
                </div>

                <!-- Condition -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold">
                    <i class="fas fa-gift me-2"></i>Điều kiện đơn tối thiểu (VNĐ)
                  </label>
                  <input
                    type="number"
                    class="form-control"
                    v-model.number="form.condition"
                    :min="MIN_ORDER_MIN"
                    step="1000"
                    :readonly="isDetailMode"
                    placeholder="VD: 100000"
                    :class="{
                      'is-invalid': errors.condition && (touched.condition || submitAttempted),
                      'is-valid': !errors.condition && (touched.condition || submitAttempted) && form.condition !== null && form.condition !== ''
                    }"
                    @blur="handleBlur('condition')"
                    @input="handleInput('condition')"
                  />
                  <div v-if="errors.condition && (touched.condition || submitAttempted)" class="invalid-feedback">
                    {{ errors.condition }}
                  </div>
                  <div class="form-text">
                    <span v-if="form.type && form.valueLimit">
                      Yêu cầu: đơn tối thiểu ≥ {{ (Number(form.valueLimit) + MIN_PAYABLE_VND).toLocaleString() }}đ
                    </span>
                    <span v-else-if="!form.type && form.value">
                      Yêu cầu: đơn tối thiểu ≥ {{ (Number(form.value) + MIN_PAYABLE_VND).toLocaleString() }}đ
                    </span>
                    <span v-else>Chỉ áp dụng cho đơn từ giá trị này trở lên</span>
                  </div>
                </div>

                <!-- Quantity -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold">
                    <i class="fas fa-dice-six me-2"></i>Số lượng mã còn lại
                  </label>
                  <input
                    type="number"
                    class="form-control"
                    v-model.number="form.quantity"
                    min="0"
                    step="1"
                    :readonly="isDetailMode"
                    placeholder="VD: 100"
                    @blur="handleBlur('quantity')"
                    @input="handleInput('quantity')"
                  />
                  <div v-if="errors.quantity && (touched.quantity || submitAttempted)" class="invalid-feedback">
                    {{ errors.quantity }}
                  </div>
                  <div class="form-text">Số lượng mã còn có thể phát hành (để trống là không giới hạn)</div>
                </div>

                <!-- Start date -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-play text-success me-2"></i>Ngày bắt đầu
                  </label>
                  <input
                    type="datetime-local"
                    class="form-control"
                    v-model="form.startDate"
                    :readonly="isDetailMode"
                    :min="isCreateMode ? new Date().toISOString().slice(0, 10) + 'T00:00' : ''"
                    :class="{
                      'is-invalid': errors.startDate && (touched.startDate || submitAttempted),
                      'is-valid': !errors.startDate && (touched.startDate || submitAttempted) && form.startDate
                    }"
                    @blur="handleBlur('startDate')"
                    @input="(e) => { form.startDate = e.target.value; handleInput('startDate') }"
                  />
                  <div v-if="errors.startDate && (touched.startDate || submitAttempted)" class="invalid-feedback">
                    {{ errors.startDate }}
                  </div>
                  <div v-if="!errors.startDate && form.startDate" class="form-text">
                    Ngày bắt đầu: {{ new Date(form.startDate).toLocaleDateString('vi-VN') }}
                  </div>
                </div>

                <!-- End date -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-stop text-danger me-2"></i>Ngày kết thúc
                  </label>
                  <input
                    type="datetime-local"
                    class="form-control"
                    v-model="form.expirationDate"
                    :readonly="isDetailMode"
                    :min="form.startDate || new Date().toISOString().slice(0, 16)"
                    :class="{
                      'is-invalid': errors.expirationDate && (touched.expirationDate || submitAttempted),
                      'is-valid': !errors.expirationDate && (touched.expirationDate || submitAttempted) && form.expirationDate
                    }"
                    @blur="handleBlur('expirationDate')"
                    @input="(e) => { form.expirationDate = e.target.value; handleInput('expirationDate') }"
                  />
                  <div v-if="errors.expirationDate && (touched.expirationDate || submitAttempted)" class="invalid-feedback">
                    {{ errors.expirationDate }}
                  </div>
                  <div v-if="!errors.expirationDate && form.expirationDate" class="form-text">
                    Ngày kết thúc: {{ new Date(form.expirationDate).toLocaleDateString('vi-VN') }}
                  </div>
                  <div v-if="form.startDate && form.expirationDate && !errors.startDate && !errors.expirationDate" class="form-text">
                    Thời gian: {{ Math.ceil((new Date(form.expirationDate) - new Date(form.startDate)) / (1000 * 60 * 60 * 24)) }} ngày
                  </div>
                </div>

                <!-- Description -->
                <div class="col-12">
                  <label class="form-label fw-semibold">
                    <i class="fas fa-align-left me-2"></i>Mô tả chi tiết
                  </label>
                  <textarea
                    class="form-control"
                    v-model="form.description"
                    rows="4"
                    :readonly="isDetailMode"
                    :maxlength="CHARACTER_LIMIT"
                    :class="{ 'is-invalid': errors.description && (touched.description || submitAttempted) }"
                    @blur="handleBlur('description')"
                    @input="handleInput('description')"
                  />
                  <div v-if="errors.description && (touched.description || submitAttempted)" class="invalid-feedback d-block">
                    {{ errors.description }}
                  </div>
                  <div class="d-flex justify-content-between">
                    <small class="ms-auto" :class="characterCount > CHARACTER_LIMIT * 0.9 ? 'text-warning' : 'text-muted'">
                      {{ characterCount }}/{{ CHARACTER_LIMIT }}
                    </small>
                  </div>
                </div>
              </div>

              <!-- Actions -->
              <div class="d-flex flex-wrap gap-2 justify-content-between align-items-center mt-4 pt-3 border-top">
                <button
                  type="button"
                  class="btn btn-outline-secondary d-flex align-items-center"
                  @click="goBack"
                  :disabled="loading"
                >
                  <i class="fas fa-arrow-left me-2"></i>Quay lại
                </button>
                <div class="d-flex gap-2">
                  <button
                    v-if="!isDetailMode"
                    type="submit"
                    :class="['btn d-flex align-items-center', isCreateMode ? 'btn-success' : 'btn-primary']"
                    :disabled="loading || (!isFormValid && submitAttempted)"
                  >
                    <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                    <i v-else-if="isCreateMode" class="fas fa-plus me-2"></i>
                    <i v-else class="fas fa-save me-2"></i>
                    {{ isCreateMode ? 'Tạo coupon' : 'Cập nhật' }}
                  </button>
                </div>
              </div>

              <div v-if="!isDetailMode" class="mt-3 pt-2 border-top">
                <small class="text-muted">
                  <i class="fas fa-keyboard me-1"></i>
                  Phím tắt: <kbd>Ctrl+S</kbd> để lưu, <kbd>Esc</kbd> để quay lại
                </small>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Toast Component -->
  <ShowToastComponent ref="toastRef" />
</template>
