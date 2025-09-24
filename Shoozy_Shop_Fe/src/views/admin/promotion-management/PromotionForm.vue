<script setup>
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import promotionApi from '@/service/PromotionApi'
import { getListOfProducts, getActiveProductsForPromotion } from '@/service/ProductApi'
import { getAllCategories } from '@/service/CategoryApi'
import ShowToastComponent from '@/components/ShowToastComponent.vue'

const props = defineProps({
  mode: { type: String, default: 'create' },
  id: { type: [String, Number], default: null }
})

const router = useRouter()
const loading = ref(false)
const submitAttempted = ref(false)
const isSubmitting = ref(false)
const toastRef = ref(null)

// ===== CONSTANTS =====
const CHARACTER_LIMIT = 500
const MAX_NAME_LENGTH = 100
const MIN_NAME_LENGTH = 3
const MAX_DISCOUNT_PERCENT = 90
const MIN_DISCOUNT_PERCENT = 0
const PRODUCT_PAGE_SIZE = 9

// ===== VALIDATION MESSAGES =====
const VALIDATION_MESSAGES = {
  name: {
    required: 'Tên khuyến mãi không được để trống',
    minLength: `Tên khuyến mãi phải có ít nhất ${MIN_NAME_LENGTH} ký tự`,
    maxLength: `Tên khuyến mãi không được vượt quá ${MAX_NAME_LENGTH} ký tự`
  },
  value: {
    required: 'Giá trị giảm giá không được để trống',
    invalid: 'Giá trị giảm giá phải là số',
    negative: 'Giá trị giảm giá không được âm',
    max: `Giá trị giảm giá không được vượt quá ${MAX_DISCOUNT_PERCENT}%`
  },
  dates: {
    startRequired: 'Ngày bắt đầu không được để trống',
    endRequired: 'Ngày kết thúc không được để trống',
    invalidRange: 'Ngày kết thúc phải sau ngày bắt đầu',
    pastDate: 'Ngày bắt đầu không được trong quá khứ'
  },
  description: {
    maxLength: `Mô tả không được vượt quá ${CHARACTER_LIMIT} ký tự`
  },
  variant: {
    priceRequired: 'Giá sau khuyến mãi không được để trống',
    priceNegative: 'Giá sau khuyến mãi không được âm',
    priceExceed: 'Giá sau khuyến mãi không được lớn hơn giá gốc',
    discountRequired: 'Phần trăm giảm giá không được để trống',
    discountNegative: 'Phần trăm giảm giá không được âm',
      discountExceed: 'Phần trăm giảm giá không được vượt quá 90%',
    discountPriceMismatch: 'Phần trăm và giá sau KM không khớp'
  }
}

// ===== REACTIVE STATE =====
const form = ref({
  name: '',
  value: 0,
  start_date: '',
  expiration_date: '',
  description: '',
  status: true,
  product_id: [],
  code: '',
})

const initialForm = ref({})
const errors = ref({})
const touched = ref({})

const products = ref([])
const productsLoading = ref(false)
const activeProductsForPromotion = ref([])
const variantPromotionData = ref({})
const variantErrors = ref({})

// ===== MODAL STATE =====
const showProductModal = ref(false)
const tempSelectedProducts = ref([])
const productSearch = ref('')
const categories = ref([])
const selectedCategory = ref('')
const productPage = ref(1)
const productTotal = ref(0)

// ===== COMPUTED PROPERTIES =====
const isViewMode = computed(() => props.mode === 'detail')
const isEditMode = computed(() => props.mode === 'update')
const isCreateMode = computed(() => props.mode === 'create')
const isEditable = computed(() => !isViewMode.value)

const headerTitle = computed(() => {
  if (isCreateMode.value) return 'Tạo khuyến mãi mới'
  if (isEditMode.value) return 'Cập nhật khuyến mãi'
  return 'Chi tiết khuyến mãi'
})

const headerDesc = computed(() => {
  if (isCreateMode.value) return 'Tạo chương trình khuyến mãi hấp dẫn cho khách hàng'
  if (isEditMode.value) return 'Chỉnh sửa thông tin khuyến mãi'
  return 'Xem thông tin chi tiết khuyến mãi'
})

const characterCount = computed(() => form.value.description?.length || 0)
const productTotalPages = computed(() => Math.ceil(productTotal.value / PRODUCT_PAGE_SIZE))

const isFormValid = computed(() => {
  return Object.values(errors.value).every(error => !error) &&
      form.value.name &&
      form.value.value !== null &&
      form.value.value !== '' &&
      form.value.start_date &&
      form.value.expiration_date &&
      form.value.value >= MIN_DISCOUNT_PERCENT &&
      form.value.value <= MAX_DISCOUNT_PERCENT
})

const discountPreview = computed(() => {
  if (form.value.value === null || form.value.value === undefined || form.value.value === 0) return ''
  return `Giảm ${form.value.value}% giá trị sản phẩm`
})

const hasUnsavedChanges = computed(() => {
  return JSON.stringify(form.value) !== JSON.stringify(initialForm.value)
})

const canSubmit = computed(() => {
  return isFormValid.value && !isSubmitting.value && !loading.value
})

const filteredProducts = computed(() => {
  return products.value
})

// ===== UTILITY FUNCTIONS =====
const getDurationString = (start, end) => {
  if (!start || !end) return ''
  const startDate = new Date(start)
  const endDate = new Date(end)
  const ms = endDate - startDate
  if (ms <= 0) return '0 phút'
  const days = Math.floor(ms / (1000 * 60 * 60 * 24))
  const hours = Math.floor((ms / (1000 * 60 * 60)) % 24)
  const minutes = Math.floor((ms / (1000 * 60)) % 60)
  let result = ''
  if (days > 0) result += `${days} ngày `
  if (hours > 0) result += `${hours} giờ `
  if (minutes > 0) result += `${minutes} phút`
  return result.trim()
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

const showNotification = (message, type = 'success') => {
  if (toastRef.value) {
    toastRef.value.showToast(message, type)
  } else {
    console.log(`${type.toUpperCase()}: ${message}`)
  }
}

// ===== VALIDATION FUNCTIONS =====
const validateField = (field, value) => {
  switch (field) {
    case 'name':
      if (!value || value.trim().length === 0) {
        errors.value.name = VALIDATION_MESSAGES.name.required
      } else if (value.length < MIN_NAME_LENGTH) {
        errors.value.name = VALIDATION_MESSAGES.name.minLength
      } else if (value.length > MAX_NAME_LENGTH) {
        errors.value.name = VALIDATION_MESSAGES.name.maxLength
      } else {
        errors.value.name = ''
      }
      break
      
    case 'value': {
      const numValue = Number(value)
      if (value === '' || value === null || value === undefined) {
        errors.value.value = VALIDATION_MESSAGES.value.required
      } else if (isNaN(numValue)) {
        errors.value.value = VALIDATION_MESSAGES.value.invalid
      } else if (numValue < MIN_DISCOUNT_PERCENT) {
        errors.value.value = VALIDATION_MESSAGES.value.negative
      } else if (numValue > MAX_DISCOUNT_PERCENT) {
        errors.value.value = VALIDATION_MESSAGES.value.max
      } else {
        errors.value.value = ''
      }
    }
      break
      
    case 'start_date':
      if (!value) {
        errors.value.start_date = VALIDATION_MESSAGES.dates.startRequired
      } else {
        const startDate = new Date(value)
        const now = new Date()

        if (isCreateMode.value) {
          const startDateOnly = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate())
          const todayOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate())

          if (startDateOnly < todayOnly) {
            errors.value.start_date = VALIDATION_MESSAGES.dates.pastDate
          } else {
            errors.value.start_date = ''
          }
        } else {
          errors.value.start_date = ''
        }
      }
      break
      
    case 'expiration_date':
      if (!value) {
        errors.value.expiration_date = VALIDATION_MESSAGES.dates.endRequired
      } else if (form.value.start_date && new Date(value) <= new Date(form.value.start_date)) {
        errors.value.expiration_date = VALIDATION_MESSAGES.dates.invalidRange
      } else {
        errors.value.expiration_date = ''
      }
      break
      
    case 'description':
      if (value && value.length > CHARACTER_LIMIT) {
        errors.value.description = VALIDATION_MESSAGES.description.maxLength
      } else {
        errors.value.description = ''
      }
      break
  }
}
const validateForm = () => {
  Object.keys(form.value).forEach(key => {
    if (errors.value.hasOwnProperty(key)) {
      validateField(key, form.value[key])
    }
  })
  return isFormValid.value
}

// ===== EVENT HANDLERS =====
const handleBlur = (field) => {
  touched.value[field] = true
  validateField(field, form.value[field])
}

const handleInput = (field) => {
  if (touched.value[field] || submitAttempted.value) {
    validateField(field, form.value[field])
  }
}

const handleFieldChange = (field, value) => {
  form.value[field] = value
  handleInput(field)
}

const handleKeydown = (event) => {
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault()
    if (!isViewMode.value) {
      handleSubmit()
    }
  } else if (event.key === 'Escape') {
    event.preventDefault()
    goBack()
  }
}

const goBack = () => {
  if (hasUnsavedChanges.value && !isViewMode.value) {
    if (confirm('Bạn có thay đổi chưa được lưu. Bạn có chắc chắn muốn rời khỏi trang?')) {
      router.push('/admin/promotions')
    }
  } else {
    router.push('/admin/promotions')
  }
}

watch(() => form.value.start_date, (newVal) => {
  if (touched.value.start_date || submitAttempted.value) {
    validateField('start_date', newVal)
  }
  if (form.value.expiration_date && (touched.value.expiration_date || submitAttempted.value)) {
    validateField('expiration_date', form.value.expiration_date)
  }
})
watch(() => form.value.expiration_date, (newVal) => {
  if (touched.value.expiration_date || submitAttempted.value) {
    validateField('expiration_date', newVal)
  }
})

/
watch(() => form.value.value, (newVal, oldVal) => {
  if (!variantPromotionData.value || Object.keys(variantPromotionData.value).length === 0) return

  Object.entries(variantPromotionData.value).forEach(([variantId, vd]) => {
    const followsGlobal =
        vd._custom !== true &&
        (vd.discountPercent == null || vd.discountPercent === oldVal)

    if (followsGlobal) {
      const p = Number(newVal) || 0
      const orig = Number(vd.originalPrice) || 0
      variantPromotionData.value[variantId] = {
        ...vd,
        discountPercent: p,
        price: Math.round(orig * (1 - p / 100)),
        _custom: false
      }
    }
  })
})

const fetchPromotion = async () => {
  if (!props.id) return
  loading.value = true
  try {
    const res = await promotionApi.getPromotionById(props.id)
    if (res && res.data) {
      const promotionData = res.data.data || res.data

      Object.keys(form.value).forEach(key => {
        if (promotionData.hasOwnProperty(key)) {
          form.value[key] = promotionData[key]
        }
      })

      // Format dates for datetime-local input
      if (form.value.start_date) {
        form.value.start_date = formatDateTime(form.value.start_date)
      }
      if (form.value.expiration_date) {
        form.value.expiration_date = formatDateTime(form.value.expiration_date)
      }

      // Store initial form state
      initialForm.value = JSON.parse(JSON.stringify(form.value))

      // Map dữ liệu biến thể khuyến mãi
      if (Array.isArray(promotionData.promotionVariantDetailResponses)) {
        const productIds = [
          ...new Set(promotionData.promotionVariantDetailResponses.map(v => v.idProduct).filter(Boolean))
        ]
        form.value.product_id = productIds

        const productMap = {}
        const variantData = {}

        promotionData.promotionVariantDetailResponses.forEach(variant => {
          // FIX 2: Nếu có custom_value thì là custom; nếu không, theo % chung
          const hasCustom = variant.custom_value != null
          const discountPercent = hasCustom ? variant.custom_value : (promotionData.value || 0)

          variantData[variant.idProductVariant] = {
            discountPercent,
            originalPrice: variant.originalPrice || 0,
            price: Math.round((variant.originalPrice || 0) * (1 - (discountPercent || 0) / 100)),
            _custom: !!hasCustom, // FIX
            size: variant.size,
            color: variant.color,
            quantity: variant.quantity || 0,
            idPromotionProduct: variant.idPromotionProduct, // giữ để update
            customValueRaw: variant.custom_value
          }

          // Gom theo sản phẩm
          const pid = variant.idProduct
          if (!productMap[pid]) {
            const productInfo = products.value.find(p => p.id === pid)
            productMap[pid] = {
              id: pid,
              name: productInfo ? productInfo.name : `Sản phẩm #${pid}`,
              variants: []
            }
          }
          productMap[pid].variants.push({
            id: variant.idProductVariant,
            size: variant.size,
            color: variant.color,
            originalPrice: variant.originalPrice || 0,
            discountPercent,
            price: Math.round((variant.originalPrice || 0) * (1 - (discountPercent || 0) / 100)),
            quantity: variant.quantity || 0
          })
        })
        variantPromotionData.value = variantData
        activeProductsForPromotion.value = Object.values(productMap)
      }
    }
  } catch (error) {
    console.error('Lỗi khi lấy thông tin khuyến mãi:', error)
    showNotification('Không thể tải thông tin khuyến mãi', 'error')
    router.push('/admin/promotions')
  } finally {
    loading.value = false
  }
}

// ===== SUBMIT FUNCTIONS =====
const prepareSubmitData = () => {
  let startDate = form.value.start_date
  let expirationDate = form.value.expiration_date

  // Clean up date format
  if (startDate.includes('Z')) startDate = startDate.replace('Z', '')
  if (expirationDate.includes('Z')) expirationDate = expirationDate.replace('Z', '')

  // Ensure proper format for API
  if (!startDate.includes('T')) {
    startDate = formatDateTime(startDate)
  }
  if (!expirationDate.includes('T')) {
    expirationDate = formatDateTime(expirationDate)
  }

  return {
    name: form.value.name,
    value: form.value.value,
    start_date: startDate,
    expiration_date: expirationDate,
    description: form.value.description,
    ...(isEditMode.value && form.value.code ? { code: form.value.code } : {}),
    productPromotionRequests: buildProductPromotionRequests()
  }
}

const handleSubmit = async () => {
  submitAttempted.value = true
  if (!validateForm()) {
    showNotification('Vui lòng kiểm tra lại thông tin form', 'error')
    return
  }

  isSubmitting.value = true
  loading.value = true

  try {
    const submitData = prepareSubmitData()

    if (isEditMode.value) {
      await promotionApi.updatePromotion(props.id, submitData)
      showNotification('Cập nhật khuyến mãi thành công!', 'success')
    } else {
      await promotionApi.createPromotion(submitData)
      showNotification('Tạo khuyến mãi thành công!', 'success')
    }

    // Update initial form to reflect saved state
    initialForm.value = JSON.parse(JSON.stringify(form.value))

    // Wait for toast to show before redirecting
    await new Promise(resolve => setTimeout(resolve, 1500))
    router.push('/admin/promotions')
  } catch (error) {
    console.error('Error details:', error)
    const errorMessage = error.response?.data?.message || error.message || 'Có lỗi xảy ra'
    showNotification(errorMessage, 'error')
  } finally {
    loading.value = false
    isSubmitting.value = false
  }
}

// ===== API FUNCTIONS =====
const fetchProducts = async () => {
  try {
    productsLoading.value = true
    const res = await getListOfProducts()
    let productData = []
    if (res && res.data) {
      if (Array.isArray(res.data)) {
        productData = res.data
      } else if (res.data.data && Array.isArray(res.data.data)) {
        productData = res.data.data
      } else if (res.data.products && Array.isArray(res.data.products)) {
        productData = res.data.products
      }
    }
    products.value = productData.filter(product =>
        product && typeof product === 'object' && product.id && product.name
    )
  } catch (err) {
    console.error('Lỗi khi lấy danh sách sản phẩm:', err)
    products.value = []
    showNotification('Không thể tải danh sách sản phẩm', 'error')
  } finally {
    productsLoading.value = false
  }
}
// ===== MODAL FUNCTIONS =====
const openProductModal = async () => {
  tempSelectedProducts.value = [...form.value.product_id]
  showProductModal.value = true
  await fetchCategories()
  await fetchProductsModal()
}

const closeProductModal = () => {
  showProductModal.value = false
}

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    if (res && res.data) {
      categories.value = Array.isArray(res.data) ? res.data : (res.data.data || [])
    }
  } catch (e) {
    categories.value = []
  }
}

const fetchProductsModal = async () => {
  try {
    productsLoading.value = true
    const params = {
      pageNo: productPage.value,
      pageSize: PRODUCT_PAGE_SIZE,
      productName: productSearch.value,
      categoryId: selectedCategory.value || undefined
    }
    const res = await getListOfProducts(params)
    let productData = []
    let total = 0
    if (res && res.data) {
      if (Array.isArray(res.data.productPromotions)) {
        productData = res.data.productPromotions
        total = res.data.totalElements || 0
      } else if (Array.isArray(res.data)) {
        productData = res.data
        total = res.data.length
      }
    }
    products.value = productData.filter(product =>
        product && typeof product === 'object' && product.id && product.name
    )
    productTotal.value = total
  } catch (err) {
    console.error('Lỗi khi lấy danh sách sản phẩm:', err)
    products.value = []
    productTotal.value = 0
  } finally {
    productsLoading.value = false
  }
}

const handleSearchInput = () => {
  productPage.value = 1
  fetchProductsModal()
}

const handleCategoryChange = () => {
  productPage.value = 1
  fetchProductsModal()
}

const goToPage = (page) => {
  if (page >= 1 && page <= productTotalPages.value) {
    productPage.value = page
  }
}

const addSelectedProducts = () => {
  form.value.product_id = [...new Set(tempSelectedProducts.value)]
  showProductModal.value = false
}

const handleRemoveProduct = (productId) => {
  form.value.product_id = form.value.product_id.filter(id => id !== productId)
}

// ===== LIFECYCLE HOOKS =====
onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  await fetchProducts()
  if (isEditMode.value || isViewMode.value) {
    await fetchPromotion()
  } else {
    // Initialize form for create mode
    initialForm.value = JSON.parse(JSON.stringify(form.value))
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

// ===== WATCHERS =====
watch([productSearch, selectedCategory, productPage], fetchProductsModal)

/**
 * Khi danh sách sản phẩm áp dụng thay đổi, gọi API lấy variants
 */
watch(() => form.value.product_id, async (newIds) => {
  if (newIds && newIds.length > 0) {
    try {
      const res = await getActiveProductsForPromotion(newIds)
      console.log('API sản phẩm khuyến mãi:', res.data)
      activeProductsForPromotion.value = res.data?.data || res.data || []
    } catch (e) {
      activeProductsForPromotion.value = []
    }
  } else {
    activeProductsForPromotion.value = []
  }
}, {immediate: true})

/**
 * Đồng bộ variantPromotionData theo activeProductsForPromotion
 * - Giữ lại biến thể cũ nếu đã chỉnh tay (_custom = true)
 * - Biến thể mới sẽ theo % chung (global) và _custom = false
 */
watch(activeProductsForPromotion, (productsList) => {
  const prev = {...variantPromotionData.value}
  const data = {}

  productsList.forEach(product => {
    if (product.variants && Array.isArray(product.variants)) {
      product.variants.forEach(variant => {
        const vid = variant.id
        if (prev[vid]) {
          // Giữ nguyên nếu có sẵn (bao gồm _custom)
          data[vid] = {...prev[vid]}
        } else {
          // Mới: theo % chung
          const discountPercent = Number(form.value.value) || 0
          const originalPrice = Number(variant.price || variant.originalPrice || 0)
          const discountedPrice = Math.round(originalPrice * (1 - discountPercent / 100))
          data[vid] = {
            price: discountedPrice,
            originalPrice,
            discountPercent,
            quantity: variant.quantity || 0,
            status: true,
            _custom: false, // FIX: mới → theo % chung
            size: variant.size,
            color: variant.color
          }
        }
      })
    }
  })

  variantPromotionData.value = data
}, {immediate: true})

watch(activeProductsForPromotion, (val) => {
  console.log('activeProductsForPromotion:', val)
}, {immediate: true})

/**
 * Khi người dùng chỉnh GIÁ sau KM → đánh dấu custom
 */
const onVariantPriceChange = (variantId) => {
  const data = variantPromotionData.value[variantId]
  if (!data) return
  const {price, originalPrice} = data

  let error = ''
  if (price === '' || price === null || price === undefined) {
    error = 'Giá sau khuyến mãi không được để trống'
  } else if (price < 0) {
    error = 'Giá sau khuyến mãi không được âm'
  } else if (originalPrice !== undefined && price > originalPrice) {
    error = 'Giá sau khuyến mãi không được lớn hơn giá gốc'
  }

  if (!error) {
    let percent = 0
    if (originalPrice > 0) {
      percent = ((originalPrice - price) / originalPrice) * 100
    }
    if (percent < 0) {
      error = 'Phần trăm giảm giá không được âm'
  } else if (percent > MAX_DISCOUNT_PERCENT) {
      error = 'Phần trăm giảm giá không được vượt quá 90%'
    }
    if (price === 0 && Math.round(percent) !== 50) {
      error = 'Nếu giá sau KM = 0 thì phần trăm giảm phải là 50%'
    }
    if (price === originalPrice && Math.round(percent) !== 0) {
      error = 'Nếu giá sau KM = giá gốc thì phần trăm giảm phải là 0%'
    }
  variantPromotionData.value[variantId].discountPercent = Math.max(0, Math.min(MAX_DISCOUNT_PERCENT, Math.round(percent * 100) / 100))
  }

  // FIX: đánh dấu custom
  variantPromotionData.value[variantId]._custom = true
  variantErrors.value[variantId] = {...variantErrors.value[variantId], price: error}
}

/**
 * Khi người dùng chỉnh % giảm → cập nhật giá + đánh dấu custom
 */
const onVariantDiscountChange = (variantId) => {
  const data = variantPromotionData.value[variantId]
  if (!data) return
  const {discountPercent, originalPrice} = data

  let error = ''
  if (discountPercent === '' || discountPercent === null || discountPercent === undefined) {
    error = 'Phần trăm giảm giá không được để trống'
  } else if (discountPercent < 0) {
    error = 'Phần trăm giảm giá không được âm'
  } else if (discountPercent > MAX_DISCOUNT_PERCENT) {
    error = 'Phần trăm giảm giá không được vượt quá 90%'
  }
  let price = Math.round(originalPrice * (1 - (Number(discountPercent) || 0) / 100))
  if (discountPercent === 50 && price !== 0) {
    error = 'Nếu phần trăm giảm là 50% thì giá sau KM phải là 0'
  }
  if (discountPercent === 0 && price !== originalPrice) {
    error = 'Nếu phần trăm giảm là 0% thì giá sau KM phải bằng giá gốc'
  }
  variantPromotionData.value[variantId].price = price

  // FIX: đánh dấu custom
  variantPromotionData.value[variantId]._custom = true
  variantErrors.value[variantId] = {...variantErrors.value[variantId], discountPercent: error}
}

/**
 * FIX 3: Không gửi priority nữa; gom theo customValue + idPromotionProduct (nếu có)
 */
const buildProductPromotionRequests = () => {
  const groupMap = {}
  Object.entries(variantPromotionData.value).forEach(([variantId, data]) => {
    const idPromotionProduct = data.idPromotionProduct || undefined
    const key = `${data.discountPercent}_${idPromotionProduct || 'new'}`

    if (!groupMap[key]) {
      groupMap[key] = {
        customValue: data.discountPercent,
        productVariantIds: [],
        ...(idPromotionProduct ? {idPromotionProduct} : {})
      }
    }
    groupMap[key].productVariantIds.push(Number(variantId))
  })
  return Object.values(groupMap)
}
</script>

<template>
  <div class="min-vh-100 bg-light bg-gradient pt-2 pb-5">
    <div class="container-fluid">
      <div class="row justify-content-center mb-4">
        <div class="col-12 px-0">
          <div class="card glass-card shadow-lg border-0 position-relative p-4">
            <div class="glass-gradient position-absolute top-0 start-0 w-100 h-100 rounded"></div>
            <div class="d-flex align-items-center position-relative" style="z-index:2">
              <div class="me-4">
                <div class="icon-glass d-flex align-items-center justify-content-center">
                  <i :class="{
                    'fas fa-percentage text-white fs-3': isCreateMode,
                    'fas fa-edit text-white fs-3': isEditMode,
                    'fas fa-eye text-white fs-3': isViewMode
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

      <!-- Main Form FULL WIDTH -->
      <div class="row justify-content-center">
        <div class="col-12 px-0">
          <div class="card glass-card border-0 shadow p-4 mb-5" style="border-radius:0;">
            <form @submit.prevent="handleSubmit" novalidate>
              <div class="row g-4">
                <!-- Name -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required" for="promotion-name">
                    <i class="fas fa-tag me-2"></i>Tên khuyến mãi
                  </label>
                  <input 
                    id="promotion-name"
                    type="text" 
                    class="form-control" 
                    v-model="form.name" 
                    :readonly="isViewMode"
                    :disabled="loading"
                    :class="{
                      'is-invalid': errors.name && (touched.name || submitAttempted),
                      'is-valid': !errors.name && (touched.name || submitAttempted) && form.name
                    }" 
                    placeholder="Nhập tên khuyến mãi" 
                    @blur="handleBlur('name')" 
                    @input="handleInput('name')"
                    maxlength="100"
                    aria-describedby="name-help name-error"
                    autocomplete="off"
                  />
                  <div v-if="errors.name && (touched.name || submitAttempted)" id="name-error" class="invalid-feedback">
                    {{ errors.name }}
                  </div>
                  <div id="name-help" class="form-text">
                    {{ form.name.length }}/{{ MAX_NAME_LENGTH }} ký tự
                    <span v-if="form.name.length > MAX_NAME_LENGTH * 0.8" class="text-warning">
                      (Gần đạt giới hạn)
                    </span>
                  </div>
                </div>

                <!-- Code - Only show in edit/view mode -->
                <div v-if="!isCreateMode" class="col-md-6">
                  <label class="form-label fw-semibold">
                    <i class="fas fa-code me-2"></i>Mã khuyến mãi
                  </label>
                  <input type="text" class="form-control bg-light" v-model="form.code"
                         readonly
                         :class="{
                      'is-invalid': errors.code && (touched.code || submitAttempted),
                      'is-valid': !errors.code && (touched.code || submitAttempted) && form.code
                    }"
                         maxlength="50"/>
                  <div class="form-text">
                    <i class="fas fa-info-circle me-1"></i>
                    Mã khuyến mãi được tạo tự động
                  </div>
                </div>

                <!-- Value - Adjust column size based on mode -->
                <div :class="isCreateMode ? 'col-md-6' : 'col-md-6'">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-percent me-2"></i>Giá trị giảm giá (%)
                  </label>
                  <div class="input-group">
                    <input type="number" class="form-control" v-model.number="form.value" min="0" :max="MAX_DISCOUNT_PERCENT" step="0.01"
                           :readonly="isViewMode" :class="{
                        'is-invalid': errors.value && (touched.value || submitAttempted),
                        'is-valid': !errors.value && (touched.value || submitAttempted) && form.value !== null && form.value !== ''
                      }" placeholder="0" @blur="handleBlur('value')" @input="handleInput('value')"/>
                    <span class="input-group-text">%</span>
                  </div>
                  <div v-if="errors.value && (touched.value || submitAttempted)" class="invalid-feedback">
                    {{ errors.value }}
                  </div>
                  <div class="form-text">Giá trị từ 0% đến 90%</div>
                </div>

                <!-- Start datetime -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-play text-success me-2"></i>Ngày bắt đầu
                  </label>
                  <input type="datetime-local" class="form-control" v-model="form.start_date" :readonly="isViewMode"
                         :min="isCreateMode ? new Date().toISOString().slice(0, 10) + 'T00:00' : ''" :class="{
                      'is-invalid': errors.start_date && (touched.start_date || submitAttempted),
                      'is-valid': !errors.start_date && (touched.start_date || submitAttempted) && form.start_date
                    }" @blur="handleBlur('start_date')"
                         @input="(e) => {
                      // Đảm bảo định dạng ngày tháng đúng
                      form.start_date = e.target.value;
                      handleInput('start_date');
                    }"/>
                  <div v-if="errors.start_date && (touched.start_date || submitAttempted)" class="invalid-feedback">
                    {{ errors.start_date }}
                  </div>
                </div>
                <!-- End datetime -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold required">
                    <i class="fas fa-stop text-danger me-2"></i>Ngày kết thúc
                  </label>
                  <input type="datetime-local" class="form-control" v-model="form.expiration_date"
                         :readonly="isViewMode" :min="form.start_date || new Date().toISOString().slice(0, 16)" :class="{
                      'is-invalid': errors.expiration_date && (touched.expiration_date || submitAttempted),
                      'is-valid': !errors.expiration_date && (touched.expiration_date || submitAttempted) && form.expiration_date
                    }" @blur="handleBlur('expiration_date')"
                         @input="(e) => {
                      // Đảm bảo định dạng ngày tháng đúng
                      form.expiration_date = e.target.value;
                      handleInput('expiration_date');
                    }"/>
                  <div v-if="errors.expiration_date && (touched.expiration_date || submitAttempted)"
                       class="invalid-feedback">
                    {{ errors.expiration_date }}
                  </div>
                  <div v-if="form.start_date && form.expiration_date" class="form-text">
                    Thời gian: {{ getDurationString(form.start_date, form.expiration_date) }}
                  </div>
                </div>
                <!-- Description -->
                <div class="col-12">
                  <label class="form-label fw-semibold">
                    <i class="fas fa-align-left me-2"></i>Mô tả chi tiết
                  </label>
                  <textarea class="form-control" v-model="form.description" rows="4" :readonly="isViewMode"
                            :class="{ 'is-invalid': errors.description && (touched.description || submitAttempted) }"
                            placeholder="Mô tả chi tiết về chương trình khuyến mãi, điều kiện áp dụng..."
                            @blur="handleBlur('description')" @input="handleInput('description')"
                            :maxlength="characterLimit"></textarea>
                  <div class="d-flex justify-content-between">
                    <div v-if="errors.description && (touched.description || submitAttempted)"
                         class="invalid-feedback d-block">
                      {{ errors.description }}
                    </div>
                    <small class="ms-auto"
                           :class="characterCount > characterLimit * 0.9 ? 'text-warning' : 'text-muted'">
                      {{ characterCount }}/{{ characterLimit }}
                    </small>
                  </div>
                </div>
                <!-- Chọn sản phẩm áp dụng khuyến mãi -->
                <div class="col-12">
                  <label class="form-label fw-semibold">
                    <i class="fas fa-box me-2"></i>Sản phẩm áp dụng khuyến mãi
                  </label>
                  <div class="mt-2">
                    <button type="button" class="btn btn-outline-success" @click="openProductModal" v-if="!isViewMode">
                      <i class="fas fa-plus me-1"></i>Thêm sản phẩm
                    </button>
                    <div v-if="form.product_id.length > 0" class="mt-2">
                      <small class="text-muted">
                        <i class="fas fa-info-circle me-1"></i>
                        Đã chọn {{ form.product_id.length }} sản phẩm
                      </small>
                    </div>
                  </div>
                  <div v-if="Array.isArray(activeProductsForPromotion) && activeProductsForPromotion.length > 0"
                       class="mt-4">
                    <h6>Sản phẩm và biến thể áp dụng khuyến mãi:</h6>
                    <div v-for="product in activeProductsForPromotion" :key="product.id" class="card mb-3">
                      <div class="card-body">
                        <div class="d-flex align-items-center mb-3">
                          <div class="bg-light d-flex align-items-center justify-content-center me-3"
                               style="width:48px;height:48px;border-radius:6px;">
                            <i class="fas fa-shoe-prints text-muted"></i>
                          </div>
                          <div class="flex-grow-1">
                            <div class="fw-bold">{{ product.name }}</div>
                            <small class="text-muted">Tổng số lượng: {{ product.quantity || 0 }}</small>
                          </div>
                          <!-- Nút xoá sản phẩm -->
                          <button v-if="!isViewMode" type="button" class="btn btn-sm btn-outline-danger ms-3"
                                  @click="handleRemoveProduct(product.id)">
                            <i class="fas fa-trash"></i>
                          </button>
                        </div>
                        <div class="table-responsive">
                          <table class="table table-sm align-middle">
                            <thead class="table-light">
                            <tr>
                              <th>Size</th>
                              <th>Màu</th>
                              <th>Giá gốc</th>
                              <th>Giá sau KM</th>
                              <th>% Giảm</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="variant in product.variants" :key="variant.id">
                              <td><span class="badge bg-secondary">{{ variant.size }}</span></td>
                              <td><span class="badge bg-info">{{ variant.color }}</span></td>
                              <td>
                                <div class="input-group input-group-sm">
                                    <span class="form-control bg-light" style="border:none;">
                                      {{ variantPromotionData[variant.id]?.originalPrice?.toLocaleString('vi-VN') }} ₫
                                    </span>
                                </div>
                              </td>
                              <td>
                                <div class="input-group input-group-sm">
                                  <input type="number" v-model.number="variantPromotionData[variant.id].price"
                                         class="form-control" placeholder="Giá KM" min="0" step="1000"
                                         :readonly="isViewMode" @input="onVariantPriceChange(variant.id)"/>
                                  <span class="input-group-text">₫</span>
                                </div>
                                <div v-if="variantErrors[variant.id]?.price" class="text-danger small mt-1">
                                  {{ variantErrors[variant.id].price }}
                                </div>
                              </td>
                              <td>
                                <div v-if="isViewMode">
                                  {{ variantPromotionData[variant.id]?.discountPercent }} %
                                </div>
                                <div v-else class="input-group input-group-sm">
                                  <input type="number"
                                         v-model.number="variantPromotionData[variant.id].discountPercent"
                                         class="form-control" min="0" :max="MAX_DISCOUNT_PERCENT" step="0.01" placeholder="%"
                                         :readonly="isViewMode" @input="onVariantDiscountChange(variant.id)"/>
                                  <span class="input-group-text">%</span>
                                  <div v-if="variantErrors[variant.id]?.discountPercent"
                                       class="text-danger small mt-1">
                                    {{ variantErrors[variant.id].discountPercent }}
                                  </div>
                                </div>
                              </td>
                            </tr>
                            </tbody>
                          </table>
                        </div>
                      </div>
                    </div>
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
                    v-if="!isViewMode" 
                    type="submit"
                    :class="['btn d-flex align-items-center', isCreateMode ? 'btn-success' : 'btn-primary']"
                    :disabled="!canSubmit"
                  >
                    <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
                    <i v-else-if="isCreateMode" class="fas fa-plus me-2"></i>
                    <i v-else class="fas fa-save me-2"></i>
                    {{ isSubmitting ? 'Đang xử lý...' : (isCreateMode ? 'Tạo khuyến mãi' : 'Cập nhật') }}
                  </button>
                </div>
              </div>

              <!-- Help Text -->
              <div v-if="!isViewMode" class="mt-3 pt-2 border-top">
                <div class="row">
                  <div class="col-md-6">
                    <small class="text-muted">
                      <i class="fas fa-keyboard me-1"></i>
                      Phím tắt: <kbd>Ctrl+S</kbd> để lưu, <kbd>Esc</kbd> để quay lại
                    </small>
                  </div>
                  <div class="col-md-6 text-end">
                    <small class="text-muted">
                      <i class="fas fa-info-circle me-1"></i>
                      Form sẽ tự động validate khi bạn nhập
                    </small>
                  </div>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- Modal chọn sản phẩm -->
      <div v-if="showProductModal" class="modal fade show d-block" tabindex="-1" style="background:rgba(0,0,0,0.3);">
        <div class="modal-dialog modal-lg">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">Chọn sản phẩm</h5>
              <button type="button" class="btn-close" @click="closeProductModal"></button>
            </div>
            <div class="modal-body">
              <div v-if="productsLoading" class="text-center py-3">
                <div class="spinner-border spinner-border-sm me-2"></div>
                <span class="text-muted">Đang tải danh sách sản phẩm...</span>
              </div>
              <div v-else>
                <div class="row mb-3 g-2 align-items-end">
                  <div class="col-md-6">
                    <label class="form-label mb-1">Danh mục</label>
                    <select class="form-select" v-model="selectedCategory" @change="handleCategoryChange">
                      <option value="">Tất cả danh mục</option>
                      <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
                    </select>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label mb-1">Tìm kiếm</label>
                    <div class="input-group">
                      <input type="text" v-model="productSearch" class="form-control"
                             placeholder="Tìm kiếm sản phẩm theo tên..." @input="handleSearchInput">
                      <button class="btn btn-outline-secondary" @click="productSearch = ''; handleSearchInput()"
                              v-if="productSearch">
                        <i class="fas fa-times"></i>
                      </button>
                    </div>
                  </div>
                </div>
                <div v-if="filteredProducts.length" class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3">
                  <div v-for="product in filteredProducts" :key="product.id" class="col">
                    <div class="card product-card h-100">
                      <div class="card-body p-3">
                        <div class="d-flex align-items-start mb-2">
                          <input class="form-check-input me-2 mt-1" type="checkbox" :id="'modal-product-' + product.id"
                                 :value="product.id" v-model="tempSelectedProducts">
                          <img :src="product.thumbnail" alt="thumb"
                               style="width:60px;height:60px;object-fit:cover;border-radius:8px;"
                               v-if="product.thumbnail">
                          <div v-else class="bg-light d-flex align-items-center justify-content-center"
                               style="width:60px;height:60px;border-radius:8px;">
                            <i class="fas fa-image text-muted"></i>
                          </div>
                        </div>
                        <div class="mb-2">
                          <label class="form-check-label fw-semibold text-truncate d-block"
                                 :for="'modal-product-' + product.id">
                            {{ product.name }}
                          </label>
                        </div>
                        <div class="d-flex justify-content-between align-items-center">
                          <div class="text-success fw-bold">
                            {{ product.price ? product.price.toLocaleString('vi-VN') + ' ₫' : 'N/A' }}
                          </div>
                          <div class="text-muted small">
                            SL: {{ product.quantity ?? 0 }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="text-center py-4 text-muted">
                  <i class="fas fa-inbox fa-2x mb-2"></i>
                  <div>Không có sản phẩm nào phù hợp</div>
                </div>
                <div v-if="productTotalPages > 1" class="mt-3 d-flex justify-content-center align-items-center gap-2">
                  <button class="btn btn-sm btn-outline-secondary" :disabled="productPage === 1"
                          @click="goToPage(productPage - 1)"><i class="fas fa-chevron-left"></i></button>
                  <span>Trang {{ productPage }} / {{ productTotalPages }}</span>
                  <button class="btn btn-sm btn-outline-secondary" :disabled="productPage === productTotalPages"
                          @click="goToPage(productPage + 1)"><i class="fas fa-chevron-right"></i></button>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" @click="closeProductModal">Hủy</button>
              <button type="button" class="btn btn-success" @click="addSelectedProducts">Thêm sản phẩm</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ShowToastComponent -->
      <ShowToastComponent ref="toastRef"/>

    </div>
  </div>
</template>

<style scoped>
.product-card {
  border-radius: 0.85rem;
  border: 1px solid #f1f1f3;
  background: #fff;
  transition: box-shadow .2s;
}

.product-card:hover {
  box-shadow: 0 2px 16px rgba(59, 130, 246, .12);
  border-color: #b8daff;
}

.product-badge {
  background: linear-gradient(90deg, #dbeafe 50%, #f0fdfa 100%);
  color: #2563eb !important;
  font-weight: 500;
  border-radius: 0.7rem;
  margin-bottom: 2px;
}

.card.glass-card {
  border-radius: 0 !important;
}

.table-responsive {
  border-radius: 0.5rem;
  overflow: hidden;
}

.table thead th {
  background-color: #f8f9fa;
  border-bottom: 2px solid #dee2e6;
  font-weight: 600;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.badge {
  font-size: 0.75rem;
  padding: 0.375rem 0.5rem;
}

.input-group-sm .form-control {
  font-size: 0.875rem;
}

.input-group-sm .input-group-text {
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
}
</style>