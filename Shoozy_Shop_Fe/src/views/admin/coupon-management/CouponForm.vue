<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import couponApi from '@/service/CouponApi'
import { useRouter } from 'vue-router'
import ShowToastComponent from '@/components/ShowToastComponent.vue'
import { connectWebSocket, getStompClient, isConnected } from '@/service/Websocket'

const props = defineProps({
  id: [String, Number],
  mode: { type: String, default: 'create' }
})
const emit = defineEmits(['submit', 'back'])
const router = useRouter()
const toastRef = ref(null)

// ===== CONSTANTS =====
const CHARACTER_LIMIT = 300
const MAX_PERCENT = 30
const MIN_PAYABLE_VND = 1000
const MAX_ORDER_AMOUNT = 100000000
const MIN_ORDER_MIN = 10000
const CODE_LENGTH = 8
const MIN_CODE_LENGTH = 4
const MAX_CODE_LENGTH = 20
const MIN_NAME_LENGTH = 3
const MAX_NAME_LENGTH = 100
const MAX_QUANTITY = 1000000

// ===== VALIDATION MESSAGES =====
const VALIDATION_MESSAGES = {
  name: {
    required: 'Vui lòng nhập tên coupon',
    minLength: `Tên coupon phải có ít nhất ${MIN_NAME_LENGTH} ký tự`,
    maxLength: `Tên coupon không được vượt quá ${MAX_NAME_LENGTH} ký tự`
  },
  code: {
    required: 'Vui lòng nhập mã coupon',
    minLength: `Mã coupon phải có ít nhất ${MIN_CODE_LENGTH} ký tự`,
    maxLength: `Mã coupon không được vượt quá ${MAX_CODE_LENGTH} ký tự`,
    pattern: 'Mã coupon chỉ được chứa chữ, số, "-", "_"',
    duplicate: 'Mã coupon đã tồn tại'
  },
  dates: {
    startRequired: 'Vui lòng chọn ngày bắt đầu',
    endRequired: 'Vui lòng chọn ngày kết thúc',
    invalidRange: 'Ngày kết thúc phải sau ngày bắt đầu',
    pastDate: 'Ngày bắt đầu không được trong quá khứ'
  },
  value: {
    required: 'Vui lòng nhập giá trị',
    percentRange: `Giá trị % phải từ 1% đến ${MAX_PERCENT}%`,
    amountMin: 'Số tiền phải từ 1.000đ trở lên',
    amountMax: `Số tiền không được vượt quá ${MAX_ORDER_AMOUNT.toLocaleString()}đ`
  },
  valueLimit: {
    required: 'Bắt buộc nhập "Giới hạn giá trị giảm tối đa (VNĐ)"',
    invalid: 'Giới hạn phải là số',
    min: 'Giới hạn phải từ 1.000đ trở lên',
    max: `Giới hạn không được vượt quá ${MAX_ORDER_AMOUNT.toLocaleString()}đ`,
    exceed: 'Giới hạn tối đa không được vượt giá trị tính toán'
  },
  condition: {
    required: 'Điều kiện đơn tối thiểu phải là số',
    min: `Điều kiện đơn tối thiểu phải ≥ ${MIN_ORDER_MIN.toLocaleString()}đ`,
    max: `Điều kiện đơn tối thiểu không được vượt quá ${MAX_ORDER_AMOUNT.toLocaleString()}đ`,
    insufficient: 'Đơn tối thiểu không đủ để áp dụng coupon'
  },
  quantity: {
    required: 'Vui lòng nhập số lượng mã còn lại',
    invalid: 'Số lượng phải là số',
    integer: 'Số lượng phải là số nguyên',
    negative: 'Số lượng không được âm',
    max: `Số lượng không được vượt quá ${MAX_QUANTITY.toLocaleString()}`
  },
  description: {
    maxLength: `Mô tả không được vượt quá ${CHARACTER_LIMIT} ký tự`
  }
}

// ===== UTILITY FUNCTIONS =====
const formatCurrency = (value) => {
  if (!value && value !== 0) return ''
  const num = Number(value)
  if (isNaN(num)) return value
  return num.toLocaleString('vi-VN')
}

const parseCurrency = (value) => {
  if (!value) return null
  const cleaned = value.toString().replace(/[^\d]/g, '')
  if (!cleaned) return null
  const num = Number(cleaned)
  return isNaN(num) ? null : num
}

const formatDateTime = (date) => {
  if (!date) return ''
  try {
    const dateObj = new Date(date)
    if (isNaN(dateObj.getTime())) return ''
    const year = dateObj.getFullYear()
    const month = String(dateObj.getMonth() + 1).padStart(2, '0')
    const day = String(dateObj.getDate()).padStart(2, '0')
    const hours = String(dateObj.getHours()).padStart(2, '0')
    const minutes = String(dateObj.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day}T${hours}:${minutes}`
  } catch (e) {
    console.error('Lỗi khi format datetime:', e)
    return ''
  }
}

const generateRandomCode = () => {
  const pool = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let code = ''
  for (let i = 0; i < CODE_LENGTH; i++) {
    code += pool.charAt(Math.floor(Math.random() * pool.length))
  }
  return code
}

// ===== REACTIVE STATE =====
const form = ref({
  name: '',
  code: '',
  type: true, // true: %, false: VND
  value: null,
  valueLimit: null,
  condition: 0,
  quantity: '',
  startDate: '',
  expirationDate: '',
  description: ''
})

const initialForm = ref({})
const errors = ref({})
const touched = ref({})
const submitAttempted = ref(false)
const loading = ref(false)
const isSubmitting = ref(false)

// ===== INPUT STATE =====
const valueRaw = ref('') // Raw for currency
const isValueFocused = ref(false)
const fieldFocusOrder = ['name', 'code', 'type', 'value', 'valueLimit', 'condition', 'quantity', 'startDate', 'expirationDate', 'description']

// ===== COMPUTED =====
const characterCount = computed(() => form.value.description.length)

const formattedValue = computed({
  get: () => (form.value.type ? (form.value.value ?? '') : valueRaw.value),
  set: (value) => {
    if (form.value.type) {
      form.value.value = value
    } else {
      valueRaw.value = value ?? ''
      form.value.value = parseCurrency(value)
    }
  }
})

const formattedValueLimit = computed({
  get: () => formatCurrency(form.value.valueLimit),
  set: (value) => { form.value.valueLimit = parseCurrency(value) }
})

const formattedCondition = computed({
  get: () => formatCurrency(form.value.condition),
  set: (value) => { form.value.condition = parseCurrency(value) }
})

const discountPreview = computed(() => {
  if (!form.value.value) return ''
  return form.value.type ? `Giảm ${form.value.value}%` : `Giảm ${Number(form.value.value).toLocaleString()}đ`
})

const effectiveDiscountAtMin = computed(() => {
  if (!form.value.value || !form.value.condition) return 0
  const orderMin = Number(form.value.condition)
  const percent = Number(form.value.value)
  const cap = Number(form.value.valueLimit)
  if (form.value.type) {
    const byPercentAtMin = Math.floor((orderMin * percent) / 100)
    return Math.min(byPercentAtMin, cap)
  }
  return Math.min(Number(form.value.value), orderMin - MIN_PAYABLE_VND)
})

const remainingAfterDiscount = computed(() => {
  if (!form.value.condition || !form.value.value) return 0
  return Number(form.value.condition) - effectiveDiscountAtMin.value
})

const formSummary = computed(() => {
  if (!form.value.name || !form.value.value) return null
  return {
    name: form.value.name,
    code: form.value.code,
    type: form.value.type ? 'Phần trăm' : 'Số tiền',
    value: form.value.type ? `${form.value.value}%` : `${formatCurrency(form.value.value)}đ`,
    condition: form.value.condition ? `${formatCurrency(form.value.condition)}đ` : 'Không giới hạn',
    duration: form.value.startDate && form.value.expirationDate
      ? Math.ceil((new Date(form.value.expirationDate) - new Date(form.value.startDate)) / (1000 * 60 * 60 * 24))
      : 0
  }
})

const canSubmit = computed(() => !isSubmitting.value && !loading.value)

// ===== MODE =====
const isCreateMode = computed(() => props.mode === 'create')
const isUpdateMode = computed(() => props.mode === 'update')
const isDetailMode = computed(() => props.mode === 'detail')
const isEditable = computed(() => !isDetailMode.value)

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

// ===== VALIDATION =====
const validateField = (field, value, f = form.value) => {
  const isPercent = f.type === true || f.type === 'true'
  switch (field) {
    case 'name':
      if (!value?.trim()) return VALIDATION_MESSAGES.name.required
      if (value.length < MIN_NAME_LENGTH) return VALIDATION_MESSAGES.name.minLength
      if (value.length > MAX_NAME_LENGTH) return VALIDATION_MESSAGES.name.maxLength
      return null
    case 'code':
      if (!value?.trim() && isCreateMode.value) return VALIDATION_MESSAGES.code.required
      if (isCreateMode.value && value.length < MIN_CODE_LENGTH) return VALIDATION_MESSAGES.code.minLength
      if (isCreateMode.value && value.length > MAX_CODE_LENGTH) return VALIDATION_MESSAGES.code.maxLength
      if (isCreateMode.value && !/^[A-Za-z0-9_-]+$/.test(value)) return VALIDATION_MESSAGES.code.pattern
      return null
    case 'startDate':
      if (!value) return VALIDATION_MESSAGES.dates.startRequired
      if (isCreateMode.value) {
        const startOnly = new Date(new Date(value).toDateString())
        const todayOnly = new Date(new Date().toDateString())
        if (startOnly < todayOnly) return VALIDATION_MESSAGES.dates.pastDate
      }
      return null
    case 'expirationDate':
      if (!value) return VALIDATION_MESSAGES.dates.endRequired
      if (f.startDate && value && f.startDate > value) return VALIDATION_MESSAGES.dates.invalidRange
      return null
    case 'description':
      if (value && value.length > CHARACTER_LIMIT) return VALIDATION_MESSAGES.description.maxLength
      return null
    case 'quantity':
      if (value === null || value === '' || value === undefined) return VALIDATION_MESSAGES.quantity.required
      if (isNaN(value)) return VALIDATION_MESSAGES.quantity.invalid
      if (!Number.isInteger(Number(value))) return VALIDATION_MESSAGES.quantity.integer
      if (value < 0) return VALIDATION_MESSAGES.quantity.negative
      if (value > MAX_QUANTITY) return VALIDATION_MESSAGES.quantity.max
      return null
    case 'condition':
      if (value === null || value === '' || isNaN(value)) return VALIDATION_MESSAGES.condition.required
      if (value < MIN_ORDER_MIN) return VALIDATION_MESSAGES.condition.min
      if (value > MAX_ORDER_AMOUNT) return VALIDATION_MESSAGES.condition.max
      return null
    case 'value':
      if (value === null || value === '' || isNaN(value)) return VALIDATION_MESSAGES.value.required
      if (isPercent) {
        if (value < 1 || value > MAX_PERCENT) return VALIDATION_MESSAGES.value.percentRange
      } else {
        if (value < 1000) return VALIDATION_MESSAGES.value.amountMin
        if (value > MAX_ORDER_AMOUNT) return VALIDATION_MESSAGES.value.amountMax
      }
      return null
    case 'valueLimit':
      if (isPercent) {
        if (value === null || value === '' || value === undefined) return VALIDATION_MESSAGES.valueLimit.required
        if (isNaN(value)) return VALIDATION_MESSAGES.valueLimit.invalid
        if (value < 1000) return VALIDATION_MESSAGES.valueLimit.min
        if (value > MAX_ORDER_AMOUNT) return VALIDATION_MESSAGES.valueLimit.max
      }
      return null
    default:
      return null
  }
}

const validateBusinessRules = (f = form.value) => {
  const err = {}
  const isPercent = f.type === true || f.type === 'true'
  if (isPercent && f.value && f.valueLimit && f.condition) {
    const orderMin = Number(f.condition)
    const percent = Number(f.value)
    const cap = Number(f.valueLimit)
    const byPercentAtMin = Math.floor((orderMin * percent) / 100)
    const effectiveAtMin = Math.min(byPercentAtMin, cap)
    const maxAllow = orderMin - MIN_PAYABLE_VND
    if (orderMin < cap + MIN_PAYABLE_VND) {
      err.condition = `Đơn tối thiểu phải ≥ (giới hạn giảm) + ${MIN_PAYABLE_VND.toLocaleString()}đ`
    }
    if (effectiveAtMin > maxAllow) {
      err.value = `Cấu hình này khiến đơn tối thiểu còn dưới ${MIN_PAYABLE_VND.toLocaleString()}đ sau giảm`
    }
    if (cap > byPercentAtMin) {
      err.valueLimit = `Giới hạn tối đa không được vượt ${byPercentAtMin.toLocaleString()}đ (tại đơn tối thiểu)`
    }
  } else if (!isPercent && f.value && f.condition) {
    if (Number(f.condition) < Number(f.value) + MIN_PAYABLE_VND) {
      err.condition = `Đơn tối thiểu phải ≥ (số tiền giảm) + ${MIN_PAYABLE_VND.toLocaleString()}đ`
    }
  }
  return err
}

const validate = (f = form.value) => {
  const err = {}
  Object.keys(f).forEach((field) => {
    const error = validateField(field, f[field], f)
    if (error) err[field] = error
  })
  const businessErrors = validateBusinessRules(f)
  Object.assign(err, businessErrors)
  return err
}

const isFormValid = computed(() => Object.keys(validate()).length === 0)
const hasUnsavedChanges = computed(() => JSON.stringify(form.value) !== JSON.stringify(initialForm.value))

// ===== EVENT HANDLERS =====
const handleBlur = (field) => { touched.value[field] = true }
const handleInput = (field) => { touched.value[field] = true }
const handleFieldChange = (field, value) => { form.value[field] = value }

const generateCode = () => {
  form.value.code = generateRandomCode()
  toastRef.value?.showToast('Đã tạo mã coupon ngẫu nhiên', 'info')
}

const handleTypeChange = (e) => {
  const val = e?.target?.value || e
  form.value.type = val === 'true' || val === true
  // Reset values when type changes
  form.value.value = null
  if (!form.value.type) {
    form.value.valueLimit = null
    valueRaw.value = ''
  } else {
    valueRaw.value = ''
  }
}

const handleValueFocus = () => {
  if (!form.value.type) {
    isValueFocused.value = true
    valueRaw.value = form.value.value == null ? '' : String(form.value.value)
  }
}
const handleValueBlur = () => {
  if (!form.value.type) {
    isValueFocused.value = false
    valueRaw.value = form.value.value == null ? '' : formatCurrency(form.value.value)
  }
}

const handleKeydown = (e) => {
  if (isDetailMode.value) return
  if (e.ctrlKey && e.key.toLowerCase() === 's') {
    e.preventDefault()
    handleSubmit()
  }
  if (e.key === 'Escape') {
    goBack()
  }
}

// ===== SUBMIT =====
const focusFirstError = async () => {
  await nextTick()
  const keys = Object.keys(errors.value)
  if (keys.length) {
    const el = document.querySelector(`[name="${keys[0]}"]`)
    el && el.focus()
  }
}

const prepareSubmitData = () => {
  const submitData = {
    ...form.value,
    type: form.value.type === true || form.value.type === 'true',
    start_date: formatDateTime(form.value.startDate),
    expiration_date: formatDateTime(form.value.expirationDate),
    value_limit: form.value.valueLimit || null
  }
  delete submitData.startDate
  delete submitData.expirationDate
  delete submitData.valueLimit
  if (isUpdateMode.value) delete submitData.code
  return submitData
}

const broadcastCouponUpdate = (couponData, action = 'COUPON_UPDATED') => {
  if (!isConnected() || !getStompClient()) {
    console.warn('[WS] Not connected, cannot broadcast coupon update')
    return
  }

  try {
    const client = getStompClient()
    const updateMessage = {
      type: 'COUPON_UPDATE',
      couponId: couponData.id,
      code: couponData.code,
      name: couponData.name,
      value: couponData.value,
      valueLimit: couponData.valueLimit,
      couponType: couponData.type,
      condition: couponData.condition,
      quantity: couponData.quantity,
      status: couponData.status,
      action: action,
      timestamp: new Date().toISOString(),
      event: 'COUPON_UPDATED'
    }

    // Broadcast đến multiple channels để đảm bảo các client khác nhận được
    const destinations = [
      '/topic/refresh',
      '/topic/coupon/status',
      '/topic/admin/coupon',
      `/topic/coupons/${couponData.code}`
    ]

    destinations.forEach(dest => {
      client.publish({
        destination: dest,
        body: JSON.stringify(updateMessage)
      })
    })

    console.log('[WS] Broadcasted coupon update:', updateMessage)
  } catch (error) {
    console.error('[WS] Error broadcasting coupon update:', error)
  }
}

// Sửa lại handleSubmit function để broadcast sau khi update thành công
const handleSubmit = async () => {
  submitAttempted.value = true
  errors.value = validate()

  if (!isFormValid.value) {
    toastRef.value?.showToast('Vui lòng kiểm tra lại thông tin form', 'error')
    await focusFirstError()
    return
  }

  if (!form.value.startDate || !form.value.expirationDate) {
    toastRef.value?.showToast('Vui lòng chọn ngày bắt đầu và ngày kết thúc', 'error')
    return
  }

  isSubmitting.value = true
  loading.value = true

  try {
    const submitData = prepareSubmitData()
    if (!submitData.start_date) throw new Error('Ngày bắt đầu không được để trống')
    if (!submitData.expiration_date) throw new Error('Ngày kết thúc không được để trống')

    let res
    if (isCreateMode.value) {
      res = await couponApi.createCoupon(submitData)
    } else if (isUpdateMode.value) {
      res = await couponApi.updateCoupon(props.id, submitData)
    }

    if (res?.data?.status === 200) {
      submitAttempted.value = false
      const successMessage = isCreateMode.value ? 'Tạo coupon thành công!' : 'Cập nhật coupon thành công!'
      toastRef.value?.showToast(successMessage, 'success')

      // Cập nhật initialForm
      initialForm.value = JSON.parse(JSON.stringify(form.value))

      // **THÊM: Broadcast WebSocket update cho các client khác**
      if (isUpdateMode.value && res?.data?.data) {
        const updatedCoupon = res.data.data
        // Đợi một chút để đảm bảo DB đã được cập nhật
        setTimeout(() => {
          broadcastCouponUpdate(updatedCoupon, 'COUPON_UPDATED')
        }, 100)
      }

      setTimeout(() => {
        router.push('/admin/coupons')
      }, 1500)
    } else {
      throw new Error(res?.data?.message || 'Unknown error')
    }
  } catch (e) {
    const errorMessage = e?.response?.data?.message || e.message
    if (errorMessage?.includes('đã tồn tại')) {
      errors.value.code = VALIDATION_MESSAGES.code.duplicate
      touched.value.code = true
      toastRef.value?.showToast('Mã coupon đã tồn tại, vui lòng chọn mã khác', 'error')
      await focusFirstError()
    } else {
      toastRef.value?.showToast(`Có lỗi khi lưu coupon: ${errorMessage}`, 'error')
    }
  } finally {
    loading.value = false
    isSubmitting.value = false
  }
}

// Thêm function để broadcast khi có thay đổi real-time (optional)
const broadcastFieldUpdate = (field, value) => {
  if (!isConnected() || !getStompClient() || isCreateMode.value) return

  // Chỉ broadcast những field quan trọng
  const importantFields = ['value', 'valueLimit', 'condition', 'quantity', 'status']
  if (!importantFields.includes(field)) return

  try {
    const client = getStompClient()
    const fieldUpdateMessage = {
      type: 'COUPON_FIELD_UPDATE',
      couponId: props.id,
      code: form.value.code,
      field: field,
      value: value,
      action: 'FIELD_CHANGED',
      timestamp: new Date().toISOString()
    }

    client.publish({
      destination: `/topic/coupons/${form.value.code}`,
      body: JSON.stringify(fieldUpdateMessage)
    })

    console.log('[WS] Broadcasted field update:', fieldUpdateMessage)
  } catch (error) {
    console.error('[WS] Error broadcasting field update:', error)
  }
}

// Thêm watcher cho các field quan trọng để broadcast real-time (optional)
watch([
  () => form.value.value,
  () => form.value.valueLimit,
  () => form.value.condition,
  () => form.value.quantity
], ([newValue, newValueLimit, newCondition, newQuantity], [oldValue, oldValueLimit, oldCondition, oldQuantity]) => {
  if (!isUpdateMode.value) return

  // Broadcast từng field thay đổi
  if (newValue !== oldValue) broadcastFieldUpdate('value', newValue)
  if (newValueLimit !== oldValueLimit) broadcastFieldUpdate('valueLimit', newValueLimit)
  if (newCondition !== oldCondition) broadcastFieldUpdate('condition', newCondition)
  if (newQuantity !== oldQuantity) broadcastFieldUpdate('quantity', newQuantity)
})

// Cleanup khi component unmount
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('beforeunload', beforeUnloadHandler)
})

// ===== DATA MAPPING =====
const mapCouponData = (data) => {
  const start = data.startDate || data.start_date
  const end = data.expirationDate || data.expiration_date
  return {
    ...data,
    valueLimit: data.valueLimit ?? data.value_limit ?? null,
    startDate: formatDateTime(start),
    expirationDate: formatDateTime(end),
    type: data.type !== undefined ? (data.type === true || data.type === 'true') : true
  }
}

const loadCoupon = async () => {
  if (props.id && (isUpdateMode.value || isDetailMode.value)) {
    loading.value = true
    try {
      const res = await couponApi.getCouponById(props.id)
      if (res?.data?.status === 200) {
        const mappedData = mapCouponData(res.data.data)
        Object.assign(form.value, mappedData)
        initialForm.value = JSON.parse(JSON.stringify(form.value))
        if (!form.value.type && form.value.value != null) {
          valueRaw.value = formatCurrency(form.value.value)
        } else {
          valueRaw.value = ''
        }
        if (isUpdateMode.value) {
          toastRef.value?.showToast('Đã tải thông tin coupon thành công!', 'success')
        }
      } else {
        throw new Error(res?.data?.message || 'Unknown error')
      }
    } catch (e) {
      toastRef.value?.showToast('Không tìm thấy coupon', 'error')
      console.error('Error loading coupon:', e)
    } finally {
      loading.value = false
    }
  } else {
    resetForm()
  }
}

const resetForm = () => {
  Object.assign(form.value, {
    name: '',
    code: '',
    type: true,
    value: null,
    valueLimit: null,
    condition: 0,
    quantity: '',
    startDate: '',
    expirationDate: '',
    description: ''
  })
  valueRaw.value = ''
  initialForm.value = JSON.parse(JSON.stringify(form.value))
  errors.value = {}
  touched.value = {}
  submitAttempted.value = false
}

const goBack = () => {
  if (hasUnsavedChanges.value && !isDetailMode.value) {
    if (confirm('Bạn có thay đổi chưa được lưu. Bạn có chắc chắn muốn rời khỏi trang?')) {
      router.push('/admin/coupons')
    }
  } else {
    router.push('/admin/coupons')
  }
}

// ===== LIFECYCLE =====
onMounted(() => {
  loadCoupon()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

// ===== WATCHERS =====
// ❌ Không auto-validate khi người dùng nhập
// (đã gỡ watcher form)

// Cảnh báo rời trang khi có thay đổi
let beforeUnloadHandler = (e) => {
  e.preventDefault()
  e.returnValue = 'Bạn có thay đổi chưa được lưu. Bạn có chắc chắn muốn rời khỏi trang?'
}
watch(hasUnsavedChanges, (hasChanges) => {
  if (hasChanges && !isDetailMode.value) {
    window.addEventListener('beforeunload', beforeUnloadHandler)
  } else {
    window.removeEventListener('beforeunload', beforeUnloadHandler)
  }
})

onMounted(() => {
  loadCoupon()
  window.addEventListener('keydown', handleKeydown)

  // Kết nối WebSocket
  connectWebSocket()
})
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
                <div class="d-flex align-items-center gap-2 mt-2">
                  <div v-if="discountPreview && form.value" class="badge bg-success-subtle text-success">
                    {{ discountPreview }}
                  </div>
                  <div v-if="hasUnsavedChanges && isEditable" class="badge bg-warning-subtle text-warning">
                    <i class="fas fa-exclamation-triangle me-1"></i>Có thay đổi chưa lưu
                  </div>
                  <div v-if="loading" class="badge bg-info-subtle text-info">
                    <span class="spinner-border spinner-border-sm me-1"></span>Đang tải...
                  </div>
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
              <!-- Discount Preview Card -->
              <div
                v-if="form.value && form.condition && !errors.value && !errors.condition"
                class="alert alert-info mb-4"
              >
                <div class="d-flex align-items-center">
                  <i class="fas fa-info-circle me-2"></i>
                  <div>
                    <strong>Xem trước:</strong> Với đơn hàng {{ formatCurrency(form.condition) }}đ,
                    khách hàng sẽ được giảm {{ formatCurrency(effectiveDiscountAtMin) }}đ
                    và còn phải trả {{ formatCurrency(remainingAfterDiscount) }}đ
                  </div>
                </div>
              </div>

              <div class="row g-4">
                <!-- Name -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required" for="coupon-name">
                    <i class="fas fa-tag me-2"></i>Tên coupon
                  </label>
                  <input
                    id="coupon-name"
                    type="text"
                    class="form-control"
                    v-model="form.name"
                    :readonly="isDetailMode"
                    :disabled="loading"
                    :class="{
                      'is-invalid': errors.name && submitAttempted,
                      'is-valid': !errors.name && submitAttempted && form.name
                    }"
                    placeholder="Nhập tên coupon"
                    @blur="handleBlur('name')"
                    @input="handleInput('name')"
                    maxlength="100"
                    name="name"
                    aria-describedby="name-error"
                    autocomplete="off"
                  />
                  <div v-if="errors.name && submitAttempted" id="name-error" class="invalid-feedback">
                    {{ errors.name }}
                  </div>
                  <div class="form-text">
                    {{ form.name.length }}/{{ MAX_NAME_LENGTH }} ký tự
                    <span v-if="form.name.length > MAX_NAME_LENGTH * 0.8" class="text-warning">
                      (Gần đạt giới hạn)
                    </span>
                  </div>
                </div>

                <!-- Code -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required" for="coupon-code">
                    <i class="fas fa-barcode me-2"></i>Mã coupon
                  </label>
                  <div class="input-group">
                    <input
                      id="coupon-code"
                      type="text"
                      class="form-control text-uppercase"
                      v-model="form.code"
                      :readonly="isDetailMode || isUpdateMode"
                      :disabled="loading"
                      :class="{
                        'is-invalid': errors.code && submitAttempted,
                        'is-valid': !errors.code && submitAttempted && form.code,
                        'bg-light': isUpdateMode
                      }"
                      placeholder="Nhập mã coupon"
                      @blur="handleBlur('code')"
                      @input="handleInput('code')"
                      maxlength="20"
                      style="text-transform: uppercase;"
                      name="code"
                      aria-describedby="code-error"
                      autocomplete="off"
                    />
                    <button
                      v-if="!isDetailMode && !isUpdateMode"
                      type="button"
                      class="btn btn-outline-primary"
                      @click="generateCode"
                      :disabled="loading"
                      tabindex="-1"
                      title="Tạo mã ngẫu nhiên"
                      aria-label="Tạo mã coupon ngẫu nhiên"
                    >
                      <i class="fas fa-magic"></i>
                    </button>
                  </div>
                  <div v-if="errors.code && submitAttempted" id="code-error" class="invalid-feedback d-block">
                    {{ errors.code }}
                  </div>
                  <div class="form-text">
                    <span v-if="isCreateMode">{{ form.code.length }}/{{ MAX_CODE_LENGTH }} ký tự</span>
                    <span v-else class="text-muted">Mã coupon không thể thay đổi sau khi tạo</span>
                  </div>
                </div>

                <!-- Type -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required" for="coupon-type">
                    <i class="fas fa-percent me-2"></i>Loại giảm giá
                  </label>
                  <div class="input-group">
                    <select
                      id="coupon-type"
                      class="form-select"
                      :disabled="isDetailMode || loading"
                      :value="form.type === true ? 'true' : 'false'"
                      @change="handleTypeChange"
                      name="type"
                    >
                      <option value="true">Phần trăm (%)</option>
                      <option value="false">Số tiền (VNĐ)</option>
                    </select>
                  </div>
                </div>

                <!-- Value -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required" for="coupon-value">
                    <i class="fas fa-money-bill-wave me-2"></i>Giá trị coupon
                  </label>
                  <div class="input-group">
                    <input
                      id="coupon-value"
                      :type="form.type ? 'number' : 'text'"
                      class="form-control"
                      v-model="formattedValue"
                      :min="form.type ? 1 : undefined"
                      :max="form.type ? MAX_PERCENT : undefined"
                      :step="form.type ? 1 : undefined"
                      :readonly="isDetailMode"
                      :disabled="loading"
                      :inputmode="form.type ? undefined : 'numeric'"
                      :pattern="form.type ? undefined : '\\d*'"
                      :class="{
                        'is-invalid': errors.value && submitAttempted,
                        'is-valid': !errors.value && submitAttempted && form.value !== null && form.value !== ''
                      }"
                      :placeholder="form.type ? 'Nhập phần trăm' : 'Nhập số tiền'"
                      name="value"
                      aria-describedby="value-error"
                      @focus="handleValueFocus"
                      @blur="handleValueBlur"
                      @input="handleInput('value')"
                    />
                    <span class="input-group-text">{{ form.type ? '%' : 'VNĐ' }}</span>
                  </div>
                  <div v-if="errors.value && submitAttempted" id="value-error" class="invalid-feedback">
                    {{ errors.value }}
                  </div>
                  <div class="form-text">
                    <span v-if="form.type">Tối đa {{ MAX_PERCENT }}%</span>
                    <span v-else>Tối thiểu 1.000đ</span>
                    <span v-if="form.value && form.type && form.valueLimit" class="ms-2 text-info">
                      (Tối đa {{ formatCurrency(form.valueLimit) }}đ)
                    </span>
                  </div>
                </div>

                <!-- Value Limit -->
                <div class="col-md-6" v-if="form.type">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-balance-scale-left me-2"></i>Giới hạn giá trị giảm tối đa (VNĐ)
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    v-model="formattedValueLimit"
                    :readonly="isDetailMode"
                    placeholder="VD: 50.000"
                    :class="{
                      'is-invalid': errors.valueLimit && submitAttempted,
                      'is-valid': !errors.valueLimit && submitAttempted && form.valueLimit !== null && form.valueLimit !== ''
                    }"
                    @blur="handleBlur('valueLimit')"
                    @input="handleInput('valueLimit')"
                    name="valueLimit"
                  />
                  <div v-if="errors.valueLimit && submitAttempted" class="invalid-feedback">
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
                    type="text"
                    class="form-control"
                    v-model="formattedCondition"
                    :readonly="isDetailMode"
                    placeholder="VD: 100.000"
                    :class="{
                      'is-invalid': errors.condition && submitAttempted,
                      'is-valid': !errors.condition && submitAttempted && form.condition !== null && form.condition !== ''
                    }"
                    @blur="handleBlur('condition')"
                    @input="handleInput('condition')"
                    name="condition"
                  />
                  <div v-if="errors.condition && submitAttempted" class="invalid-feedback">
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
                  <label class="form-label fw-semibold required" for="coupon-quantity">
                    <i class="fas fa-dice-six me-2"></i>Số lượng mã còn lại
                  </label>
                  <input
                    id="coupon-quantity"
                    type="number"
                    class="form-control"
                    v-model.number="form.quantity"
                    min="1"
                    step="1"
                    :readonly="isDetailMode"
                    :disabled="loading"
                    placeholder="VD: 100"
                    @blur="handleBlur('quantity')"
                    @input="handleInput('quantity')"
                    name="quantity"
                    aria-describedby="quantity-error"
                  />
                  <div v-if="errors.quantity && submitAttempted" id="quantity-error" class="invalid-feedback">
                    {{ errors.quantity }}
                  </div>
                  <div class="form-text">
                    Số lượng mã coupon có thể phát hành (bắt buộc nhập)
                  </div>
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
                      'is-invalid': errors.startDate && submitAttempted,
                      'is-valid': !errors.startDate && submitAttempted && form.startDate
                    }"
                    @blur="handleBlur('startDate')"
                    @input="(e) => { form.startDate = e.target.value; handleInput('startDate') }"
                    name="startDate"
                  />
                  <div v-if="errors.startDate && submitAttempted" class="invalid-feedback">
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
                      'is-invalid': errors.expirationDate && submitAttempted,
                      'is-valid': !errors.expirationDate && submitAttempted && form.expirationDate
                    }"
                    @blur="handleBlur('expirationDate')"
                    @input="(e) => { form.expirationDate = e.target.value; handleInput('expirationDate') }"
                    name="expirationDate"
                  />
                  <div v-if="errors.expirationDate && submitAttempted" class="invalid-feedback">
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
                    :class="{ 'is-invalid': errors.description && submitAttempted }"
                    @blur="handleBlur('description')"
                    @input="handleInput('description')"
                    name="description"
                  />
                  <div v-if="errors.description && submitAttempted" class="invalid-feedback d-block">
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
                  :disabled="isSubmitting"
                >
                  <i class="fas fa-arrow-left me-2"></i>Quay lại
                </button>
                <div class="d-flex gap-2">
                  <button
                    v-if="!isDetailMode"
                    type="submit"
                    :class="['btn d-flex align-items-center', isCreateMode ? 'btn-success' : 'btn-primary']"
                    :disabled="!canSubmit"
                  >
                    <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
                    <i v-else-if="isCreateMode" class="fas fa-plus me-2"></i>
                    <i v-else class="fas fa-save me-2"></i>
                    {{ isSubmitting ? 'Đang xử lý...' : (isCreateMode ? 'Tạo coupon' : 'Cập nhật') }}
                  </button>
                </div>
              </div>

              <!-- (ĐÃ XOÁ phần text hướng dẫn) -->
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Toast Component -->
  <ShowToastComponent ref="toastRef" />
</template>
