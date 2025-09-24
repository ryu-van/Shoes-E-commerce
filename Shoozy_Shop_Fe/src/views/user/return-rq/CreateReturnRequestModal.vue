<script setup>
import { ref, watch, computed } from 'vue'
import { createReturnRequest, uploadReturnImages } from '@/service/ReturnApis'
import ShowToastComponent from '@/components/ShowToastComponent.vue'

const props = defineProps({ order: Object, items: Array })
const emit = defineEmits(['close','submitted'])

const MAX_IMAGES = 5

const reason = ref('')
const note   = ref('')
const loading = ref(false)
const toastRef = ref(null)
const uploadingImagesCount = ref(0)

const selectedItems = ref([])

// ==== Refund info + errors ====
const refund = ref({
  method: 'BANK_TRANSFER',
  bankName: '',
  accountNumber: '',
  accountHolder: '',
  walletProvider: '',
  walletAccount: ''
})

// 👉 Nâng cấp error: có cả message
const errors = ref({
  bankName: false,
  accountNumber: false,
  accountHolder: false,
  walletProvider: false,
  walletAccount: false,
})
const errorMsgs = ref({
  bankName: '',
  accountNumber: '',
  accountHolder: '',
  walletProvider: '',
  walletAccount: '',
})
function resetErrors () {
  errors.value = {
    bankName: false,
    accountNumber: false,
    accountHolder: false,
    walletProvider: false,
    walletAccount: false
  }
  errorMsgs.value = {
    bankName: '',
    accountNumber: '',
    accountHolder: '',
    walletProvider: '',
    walletAccount: '',
  }
}

/** ===========================
 * Regex & helpers cho refund
 * =========================== */
const NAME_RE        = /^[\p{L}\p{M}\s'.-]{2,60}$/u
const BANK_RE        = /^[\p{L}\p{M}\s'.-]{2,60}$/u
const ACCOUNT_NO_RE  = /^\d{6,20}$/
const PHONE_RE       = /^0\d{9,10}$/
const EMAIL_RE       = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

function validateRefundLive() {
  resetErrors()
  if (refund.value.method === 'BANK_TRANSFER') {
    const bn = (refund.value.bankName || '').trim()
    const an = (refund.value.accountNumber || '').trim()
    const ah = (refund.value.accountHolder || '').trim()
    if (!bn || !BANK_RE.test(bn)) {
      errors.value.bankName = true
      errorMsgs.value.bankName = bn ? 'Tên ngân hàng không hợp lệ.' : 'Không được để trống.'
    }
    if (!an || !ACCOUNT_NO_RE.test(an)) {
      errors.value.accountNumber = true
      errorMsgs.value.accountNumber = an ? 'Số tài khoản chỉ gồm 6–20 chữ số.' : 'Không được để trống.'
    }
    if (!ah || !NAME_RE.test(ah)) {
      errors.value.accountHolder = true
      errorMsgs.value.accountHolder = ah ? 'Tên chủ tài khoản không hợp lệ.' : 'Không được để trống.'
    }
  } else if (refund.value.method === 'EWALLET') {
    const wp = (refund.value.walletProvider || '').trim()
    const wa = (refund.value.walletAccount || '').trim()
    if (!wp || !NAME_RE.test(wp)) {
      errors.value.walletProvider = true
      errorMsgs.value.walletProvider = wp ? 'Tên ví không hợp lệ.' : 'Không được để trống.'
    }
    if (!wa || !(PHONE_RE.test(wa) || EMAIL_RE.test(wa))) {
      errors.value.walletAccount = true
      errorMsgs.value.walletAccount = wa ? 'Tài khoản ví phải là SĐT (bắt đầu 0, 10–11 số) hoặc Email hợp lệ.' : 'Không được để trống.'
    }
  }
}

// Validate theo thời gian thực khi đổi phương thức/nhập dữ liệu
watch(() => refund.value.method, validateRefundLive)
watch(refund, validateRefundLive, { deep: true })

/** ================================
 * Khởi tạo item + error từng hàng
 * ================================ */
watch(
  () => props.items,
  (newItems) => {
    if (Array.isArray(newItems)) {
      selectedItems.value = newItems.map(item => ({
        orderDetailId : item.orderDetailId,
        productName   : item.productName,
        color         : item.color,
        size          : item.size,
        quantity      : item.quantity,
        thumbnail     : item.thumbnail,
        isSelected    : true,
        returnQuantity: 1,
        itemNote      : '',
        imageUrls     : [],
        imagePreviews : [],
        // 👉 lỗi số lượng
        qtyError      : '',
         imgError      : '',
      }))
    }
  },
  { immediate: true }
)

const close = () => emit('close')
function validateItemImages(item) {
  // yêu cầu: phải có ít nhất 1 ảnh đã upload thành công
  const countUploaded = (item.imageUrls?.length || 0)
  if (countUploaded < 1) {
    item.imgError = 'Vui lòng thêm ít nhất 1 ảnh minh chứng cho sản phẩm này.'
    return false
  }
  item.imgError = ''
  return true
}

/** =========================================
 * Validate số lượng: khóa cứng 1..max khi gõ
 * ========================================= */
function clampInt(n, min, max) {
  if (Number.isNaN(n)) return min
  n = Math.trunc(n)
  if (n < min) n = min
  if (n > max) n = max
  return n
}

// Dự đoán chuỗi nếu thao tác chèn xảy ra
function buildNextValue(el, insertText) {
  const start = el.selectionStart ?? el.value.length
  const end   = el.selectionEnd ?? el.value.length
  const left  = el.value.slice(0, start)
  const right = el.value.slice(end)
  return left + (insertText ?? '') + right
}

// Chặn ngay ở trước khi input nếu vượt min/max hoặc không phải số
function onQtyBeforeInput(item, e) {
  const t = e.target
  const type = e.inputType

  // Cho phép delete/backspace
  if (type?.startsWith('delete')) return

  const insert = e.data ?? (e.clipboardData?.getData('text') ?? '')
  if (!/^\d+$/.test(insert)) { e.preventDefault(); return }

  let next = buildNextValue(t, insert).replace(/\D+/g, '')
  if (next === '') return // cho phép để trống tạm thời

  next = String(Number(next)) // bỏ 0 đầu
  const max = Number(item.quantity || 1)
  const n = Number(next)

  if (!Number.isFinite(n) || n < 1 || n > max) {
    e.preventDefault()
    item.qtyError = n > max ? `Tối đa ${max}.` : 'Tối thiểu là 1.'
  }
}

// Sau khi input: normalize và đồng bộ model
function onQtyAfterInput(item, e) {
  let v = String(e.target.value || '').replace(/\D+/g, '')
  if (v === '') { item.returnQuantity = ''; return }
  v = String(Number(v))
  const max = Number(item.quantity || 1)
  let n = Number(v)
  if (n < 1) n = 1
  if (n > max) n = max
  e.target.value = String(n)
  item.returnQuantity = n
  item.qtyError = ''
}

// Ràng buộc lần cuối khi blur (phòng hờ)
function onQuantityBlur(item) {
  const max = Number(item.quantity || 1)
  const n = clampInt(Number(item.returnQuantity), 1, max)
  item.returnQuantity = n
  item.qtyError = ''
}

/** ============== Ảnh (giữ nguyên) ============== */
const remainSlots = (item) =>
  MAX_IMAGES - ((item.imageUrls?.length || 0) + (item.imagePreviews?.length || 0))
const canAddMore = (item) => remainSlots(item) > 0

const handleImageUpload = async (event, item) => {
  const picked = Array.from(event.target.files || [])
  if (!picked.length) return

  const room = remainSlots(item)
  if (room <= 0) {
    toastRef.value?.showToast(`Mỗi sản phẩm chỉ tối đa ${MAX_IMAGES} ảnh.`, 'warning')
    event.target.value = ''
    return
  }

  const toSend = picked.slice(0, room)
  if (picked.length > toSend.length) {
    toastRef.value?.showToast(`Chỉ thêm được ${room} ảnh nữa (tối đa ${MAX_IMAGES}).`, 'info')
  }

  const newPreviews = toSend.map(f => URL.createObjectURL(f))
  item.imagePreviews.push(...newPreviews)

  uploadingImagesCount.value++
  try {
    const res = await uploadReturnImages(toSend)
    const urls = res?.data?.data || []
    item.imagePreviews.splice(item.imagePreviews.length - newPreviews.length, newPreviews.length)
    item.imageUrls.push(...urls)
    validateItemImages(item)
  } catch (err) {
    console.error(err)
    item.imagePreviews.splice(item.imagePreviews.length - newPreviews.length, newPreviews.length)
    toastRef.value?.showToast(
      err?.response?.data?.message ||
      'Upload ảnh thất bại. Kiểm tra định dạng (JPG/PNG/WebP/GIF) & dung lượng ≤ 5MB.',
      'error'
    )
  } finally {
    uploadingImagesCount.value--
    event.target.value = ''
  }
}

const removeUploaded = (item, index) => {
  item.imageUrls.splice(index, 1)
  validateItemImages(item)
}
const removePreview  = (item, index) => {
  item.imagePreviews.splice(index, 1)
  // Preview chỉ là tạm, nhưng xóa vẫn có thể nhắc lại cho rõ
  validateItemImages(item)
}


const selectedCount = computed(() =>
  selectedItems.value
    .filter(i => i.isSelected)
    .reduce((sum, i) => sum + (Number(i.returnQuantity) || 0), 0)
)

// Nút +/- đảm bảo clamp
const adjustQuantity = (item, delta) => {
  const max = Number(item.quantity || 1)
  const next = clampInt(Number(item.returnQuantity || 0) + delta, 1, max)
  item.returnQuantity = next
  item.qtyError = ''
}

/** ============== Submit (bổ sung check lỗi) ============== */
const submit = async () => {
  if (uploadingImagesCount.value > 0) {
    toastRef.value?.showToast('Vui lòng chờ ảnh tải lên xong rồi mới gửi yêu cầu.', 'warning')
    return
  }

  if (selectedCount.value >= 2 && !reason.value.trim()) {
    toastRef.value?.showToast('Vui lòng nhập lý do chung khi trả nhiều sản phẩm.', 'error')
    return
  }

  // Live-validate refund
  validateRefundLive()
  if (Object.values(errors.value).some(Boolean)) {
    toastRef.value?.showToast('Thông tin nhận tiền hoàn chưa hợp lệ. Vui lòng kiểm tra lại.', 'error')
    return
  }

  const items = []
  for (const item of selectedItems.value) {
    if (!item.isSelected) continue

    const qty = Number(item.returnQuantity || 0)
    if (!Number.isInteger(qty) || qty < 1 || qty > item.quantity) {
      item.qtyError = `Số lượng phải từ 1 đến ${item.quantity}.`
      toastRef.value?.showToast(`Số lượng trả không hợp lệ cho "${item.productName}".`, 'warning')
      return
    }

    const perItemReason = (item.itemNote || '').trim()
    if (!perItemReason) {
      toastRef.value?.showToast(`Vui lòng nhập lý do cho sản phẩm "${item.productName}".`, 'error')
      return
    }  if (!validateItemImages(item)) {
    toastRef.value?.showToast(`"${item.productName}" cần ít nhất 1 ảnh minh chứng.`, 'error')
    return
  }

  items.push({
    orderDetailId: item.orderDetailId,
    quantity     : qty,
    note         : perItemReason,
    imageUrls    : (item.imageUrls || []).slice(0, MAX_IMAGES)
  })

  }

  if (!items.length) {
    toastRef.value?.showToast('Bạn chưa chọn sản phẩm nào để trả hàng.', 'warning')
    return
  }

  const payload = {
    orderId: props.order.id,
    reason : selectedCount.value >= 2 ? reason.value.trim() : '',
    note   : selectedCount.value >= 2 ? (note.value || '').trim() : '',
    items,
    refundInfo: {
      method: refund.value.method,
      bankName: refund.value.bankName || null,
      accountNumber: refund.value.accountNumber || null,
      accountHolder: refund.value.accountHolder || null,
      walletProvider: refund.value.walletProvider || null,
      walletAccount: refund.value.walletAccount || null
    }
  }

  loading.value = true
  try {
    await createReturnRequest(payload)
    toastRef.value?.showToast('Tạo yêu cầu trả hàng thành công!', 'success')
    setTimeout(() => {
      reason.value = ''
      note.value = ''
      selectedItems.value = []
      emit('close')
    }, 1000)
  } catch (err) {
    console.error('Lỗi khi gửi yêu cầu trả hàng:', err)
    toastRef.value?.showToast('Gửi yêu cầu thất bại.', 'error')
  } finally {
    loading.value = false
  }
  emit('submitted', { 
  orderId: props.order.id, 
  // nếu BE có trả về số còn trả được thì gửi kèm, không có thì bỏ
  remainingCount: resp?.data?.data?.remainingCount 
})
}
</script>


<template>
  <div class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h3>Tạo Yêu Cầu Trả Hàng</h3>
        <button class="close-btn" @click="close" aria-label="Đóng">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      <div class="modal-body">
        <!-- Lý do chung -->
        <div v-if="selectedCount >= 2" class="general-reason-box">
          <h4>Lý do chung</h4>
          <div class="form-group">
            <label for="reason">Lý do chung (bắt buộc)</label>
            <textarea id="reason" v-model="reason" rows="3" placeholder="Ví dụ: Hàng không đúng mô tả, sai kích thước..."></textarea>
          </div>
          <div class="form-group">
            <label for="note">Ghi chú thêm (không bắt buộc)</label>
            <textarea id="note" v-model="note" rows="2" placeholder="Thêm ghi chú cho yêu cầu của bạn..."></textarea>
          </div>
        </div>

        <div class="return-items-list">
          <p class="items-list-header">Chọn sản phẩm và lý do trả hàng:</p>

          <div v-for="item in selectedItems" :key="item.orderDetailId" class="return-item-card" :class="{ 'is-selected': item.isSelected }">
            <div class="item-info">
              <input type="checkbox" v-model="item.isSelected" class="item-checkbox" />
              <img :src="item.thumbnail" class="thumbnail" alt="Ảnh sản phẩm" />
              <div class="product-details">
                <div class="product-name">{{ item.productName }}</div>
                <div class="variant">Phân loại: {{ item.color }} - Size {{ item.size }}</div>
                <div class="original-quantity">Số lượng đã mua: {{ item.quantity }}</div>
              </div>
            </div>

            <div v-if="item.isSelected" class="item-return-form">
              <div class="form-grid">
               <div class="form-group">
  <label :for="'qty-' + item.orderDetailId">Số lượng trả</label>
  <div class="quantity-control">
    <button @click="adjustQuantity(item, -1)">-</button>

   <input
  :id="'qty-' + item.orderDetailId"
  type="text"
  inputmode="numeric"
  pattern="\d*"
  :class="{ 'is-invalid': !!item.qtyError }"
  :value="item.returnQuantity"
  :maxlength="String(item.quantity).length"
  @beforeinput="(e) => onQtyBeforeInput(item, e)"
  @input="(e) => onQtyAfterInput(item, e)"
  @blur="() => onQuantityBlur(item)"
  aria-describedby="'qty-err-' + item.orderDetailId"
/>


    <button @click="adjustQuantity(item, 1)">+</button>
  </div>
  <small
    v-if="item.qtyError"
    class="invalid-msg"
    :id="'qty-err-' + item.orderDetailId"
  >{{ item.qtyError }}</small>
</div>


                <div class="form-group reason-group">
                  <label :for="'reason-' + item.orderDetailId">Lý do trả hàng (bắt buộc)</label>
                  <input :id="'reason-' + item.orderDetailId" type="text" v-model="item.itemNote" placeholder="Nhập lý do cho sản phẩm này" />
                </div>
              </div>

              <div class="form-group">
                <label>Ảnh/Video minh chứng</label>
                <div class="image-upload-area">
                  <div class="text-muted" style="font-size:12px;margin-bottom:6px;">
                    {{ (item.imageUrls?.length || 0) + (item.imagePreviews?.length || 0) }}/{{ MAX_IMAGES }} ảnh
                  </div>

                  <div class="preview-images">
                    <div v-for="(url, i) in item.imageUrls" :key="'u'+i" class="preview-thumb-wrapper">
                      <img :src="url" class="preview-thumb" alt="Ảnh đã upload" />
                      <button class="remove-image-btn" @click="removeUploaded(item, i)">×</button>
                    </div>

                    <div v-for="(img, i) in item.imagePreviews" :key="'p'+i" class="preview-thumb-wrapper">
                      <img :src="img" class="preview-thumb" alt="Đang tải..." />
                      <button class="remove-image-btn" @click="removePreview(item, i)">×</button>
                    </div>
                  </div>

                  <label class="upload-btn-label" :class="{ 'disabled': !canAddMore(item) }" :title="canAddMore(item) ? 'Thêm ảnh' : `Tối đa ${MAX_IMAGES} ảnh`">
                    <input type="file" multiple accept="image/*" :disabled="!canAddMore(item)" @change="(e) => handleImageUpload(e, item)" hidden />
                    <span>+ Thêm ảnh</span>
                  </label>
                </div>
                <small v-if="item.imgError" class="invalid-msg">{{ item.imgError }}</small>

                <div v-if="uploadingImagesCount > 0" class="uploading-note">
                  <svg width="16" height="16" viewBox="0 0 24 24">
                    <path fill="currentColor" d="M12,4a8,8,0,0,1,7.89,6.7A1.53,1.53,0,0,0,21.38,12h0a1.5,1.5,0,0,0,1.48-1.75,11,11,0,0,0-21.72,0A1.5,1.5,0,0,0,2.62,12h0a1.53,1.53,0,0,0,1.49-1.3A8,8,0,0,1,12,4Z">
                      <animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="1s" repeatCount="indefinite"/>
                    </path>
                  </svg>
                  Đang tải ảnh lên...
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Refund info -->
        <div class="card p-3 mt-3">
          <h5>Thông tin nhận tiền hoàn</h5>

          <div class="mb-2">
            <label class="form-label">Phương thức</label>
            <select v-model="refund.method" class="form-select">
              <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
              <option value="EWALLET">Ví điện tử</option>
              <option value="CASH">Tiền mặt</option>
            </select>
          </div>

          <!-- BANK_TRANSFER -->
<div v-if="refund.method === 'BANK_TRANSFER'" class="row g-2">
  <div class="col-md-4">
    <label class="form-label">Ngân hàng</label>
    <input
      v-model.trim="refund.bankName"
      class="form-control"
      :class="{ 'is-invalid': errors.bankName }"
      placeholder="Vietcombank..."
    />
    <small v-if="errors.bankName" class="invalid-msg">{{ errorMsgs.bankName }}</small>
  </div>
  <div class="col-md-4">
    <label class="form-label">Số tài khoản</label>
    <input
      v-model.trim="refund.accountNumber"
      class="form-control"
      :class="{ 'is-invalid': errors.accountNumber }"
      placeholder="0123456789"
      inputmode="numeric"
      pattern="\d*"
      @keypress="onlyDigitsKeypress"
      @paste="digitsOnPaste"
    />
    <small v-if="errors.accountNumber" class="invalid-msg">{{ errorMsgs.accountNumber }}</small>
  </div>
  <div class="col-md-4">
    <label class="form-label">Chủ tài khoản</label>
    <input
      v-model.trim="refund.accountHolder"
      class="form-control"
      :class="{ 'is-invalid': errors.accountHolder }"
      placeholder="Nguyễn Văn A"
    />
    <small v-if="errors.accountHolder" class="invalid-msg">{{ errorMsgs.accountHolder }}</small>
  </div>
</div>

<!-- EWALLET -->
<div v-else-if="refund.method === 'EWALLET'" class="row g-2">
  <div class="col-md-4">
    <label class="form-label">Ví</label>
    <input
      v-model.trim="refund.walletProvider"
      class="form-control"
      :class="{ 'is-invalid': errors.walletProvider }"
      placeholder="MoMo / ZaloPay..."
    />
    <small v-if="errors.walletProvider" class="invalid-msg">{{ errorMsgs.walletProvider }}</small>
  </div>
  <div class="col-md-4">
    <label class="form-label">Tài khoản ví (SĐT/Email)</label>
    <input
      v-model.trim="refund.walletAccount"
      class="form-control"
      :class="{ 'is-invalid': errors.walletAccount }"
      placeholder="09xxx / email@..."
      @blur="validateRefundLive"
    />
    <small v-if="errors.walletAccount" class="invalid-msg">{{ errorMsgs.walletAccount }}</small>
  </div>
</div>


          <div v-else class="text-muted">
            Tiền mặt: bạn sẽ nhận tại quầy / khi shipper thu hồi hàng (tuỳ chính sách).
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-secondary" @click="close">Hủy</button>
        <button class="btn btn-primary" @click="submit" :disabled="loading || uploadingImagesCount > 0">
          <span v-if="loading">Đang xử lý...</span>
          <span v-else>Gửi Yêu Cầu</span>
        </button>
      </div>
    </div>
    <ShowToastComponent ref="toastRef" />
  </div>
</template>

<style scoped>
.image-upload-area.has-error {
  border: 1px solid #dc3545; /* đỏ */
  border-radius: 6px;
  padding: 8px;
}
.invalid-msg {
  color: #dc3545;
  font-size: 12px;
}

.upload-btn-label.disabled { opacity: .6; pointer-events: none; }

/* Modal Layout */
.modal-overlay {
  --primary-color:#007bff; --primary-color-light:#e6f2ff; --secondary-color:#6c757d;
  --text-color:#333; --border-color:#dee2e6; --background-color:#f8f9fa;
  --white-color:#fff; --danger-color:#dc3545; --border-radius:8px;
  position: fixed; inset: 0; background: rgba(0,0,0,.6); display:flex; justify-content:center; align-items:center; z-index:1000; backdrop-filter: blur(4px);
}
.modal-content { background: var(--background-color); width: 90%; max-width: 720px; border-radius: var(--border-radius); max-height: 90vh; display:flex; flex-direction:column; overflow:hidden; box-shadow:0 10px 30px rgba(0,0,0,.1); }
.modal-header { display:flex; justify-content:space-between; align-items:center; padding:16px 24px; border-bottom:1px solid var(--border-color); }
.modal-body   { padding:16px 24px; overflow-y:auto; flex-grow:1; }
.modal-footer { display:flex; justify-content:flex-end; gap:12px; padding:16px 24px; border-top:1px solid var(--border-color); background:var(--white-color); }

/* General Reason Box */
.general-reason-box { background:var(--white-color); border:1px solid var(--border-color); border-radius:var(--border-radius); padding:16px; margin-bottom:24px; }

/* Items List */
.items-list-header { font-size:14px; color:var(--secondary-color); margin-bottom:12px; }

/* Return Item Card */
.return-item-card { background:#fff; border:1px solid var(--border-color); border-radius:var(--border-radius); padding:16px; margin-bottom:16px; transition:all .2s ease; }
.return-item-card.is-selected { border-color:var(--primary-color); box-shadow:0 0 0 2px var(--primary-color-light); }
.item-info { display:flex; gap:16px; align-items:center; }
.item-checkbox { width:18px; height:18px; }
.thumbnail { width:60px; height:60px; object-fit:cover; border-radius:4px; border:1px solid var(--border-color); }
.product-details { flex-grow:1; }
.product-name { font-weight:600; color:var(--text-color); }
.variant, .original-quantity { font-size:14px; color:var(--secondary-color); margin-top:4px; }

/* Item Return Form */
.item-return-form { margin-top:16px; padding-top:16px; border-top:1px dashed var(--border-color); }
.form-grid { display:grid; grid-template-columns:1fr 2fr; gap:16px; margin-bottom:16px; align-items:end; }
@media (max-width:600px){ .form-grid{ grid-template-columns:1fr; } }

/* Form Elements */
.form-group{ display:flex; flex-direction:column; }
.form-group.reason-group{ grid-column:2 / 3; }
.form-group label{ font-size:14px; font-weight:500; margin-bottom:8px; color:#344054; }
.form-group input[type="text"], .form-group input[type="number"], .form-group textarea {
  width:100%; padding:10px 12px; border-radius:6px; border:1px solid var(--border-color); font-size:14px;
}
.form-group input:focus, .form-group textarea:focus { outline:none; border-color:var(--primary-color); box-shadow:0 0 0 2px var(--primary-color-light); }

/* Quantity Control */
.quantity-control { display:flex; }
.quantity-control input { text-align:center; border-left:none; border-right:none; width:50px; }
.quantity-control button { width:38px; height:38px; border:1px solid var(--border-color); background:#fff; font-size:18px; cursor:pointer; color:var(--secondary-color); }
.quantity-control button:first-child { border-top-left-radius:6px; border-bottom-left-radius:6px; }
.quantity-control button:last-child  { border-top-right-radius:6px; border-bottom-right-radius:6px; }

/* Image Upload */
.image-upload-area { display:flex; gap:10px; align-items:flex-start; flex-wrap:wrap; }
.preview-images { display:flex; gap:10px; flex-wrap:wrap; }
.preview-thumb-wrapper { position:relative; }
.preview-thumb { width:70px; height:70px; object-fit:cover; border-radius:6px; border:1px solid var(--border-color); }
.remove-image-btn { position:absolute; top:-5px; right:-5px; width:20px; height:20px; border-radius:50%; background:var(--danger-color); color:#fff; border:none; font-size:12px; cursor:pointer; display:flex; align-items:center; justify-content:center; }
.upload-btn-label { width:70px; height:70px; border:2px dashed var(--border-color); border-radius:6px; display:flex; flex-direction:column; align-items:center; justify-content:center; cursor:pointer; color:var(--secondary-color); }
.upload-btn-label:hover { border-color:var(--primary-color); color:var(--primary-color); }

.uploading-note { display:flex; align-items:center; gap:8px; color:#ffc107; font-size:14px; margin-top:8px; }

/* Buttons */
.close-btn { background:transparent; border:none; cursor:pointer; color:var(--secondary-color); padding:4px; border-radius:50%; }
.btn { padding:10px 18px; border-radius:6px; font-weight:600; font-size:14px; border:1px solid transparent; cursor:pointer; transition:all .2s; }
.btn:disabled { opacity:.6; cursor:not-allowed; }
.btn-primary { background:#007bff; color:#fff; }
.btn-primary:hover:not(:disabled){ background:#0056b3; }
.btn-secondary { background:#fff; border:1px solid var(--border-color); color:var(--text-color); }
.btn-secondary:hover:not(:disabled){ background:#f8f9fa; }

/* Invalid state */
.is-invalid { border-color:#dc3545 !important; }
.invalid-msg { color:#dc3545; font-size:12px; margin-top:4px; display:block; }
</style>
