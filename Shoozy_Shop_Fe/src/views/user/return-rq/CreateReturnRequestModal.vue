<script setup>
import { ref, watch, computed } from 'vue'
import { createReturnRequest, uploadReturnImages } from '@/service/ReturnApis'
import ShowToastComponent from '@/components/ShowToastComponent.vue'

const props = defineProps({ order: Object, items: Array })
const emit = defineEmits(['close'])

const MAX_IMAGES = 5

const reason = ref('')   // lý do chung (bắt buộc nếu >= 2 sp)
const note   = ref('')
const loading = ref(false)
const toastRef = ref(null)
const uploadingImagesCount = ref(0)

const selectedItems = ref([])

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
        itemNote      : '',           // lý do riêng (bắt buộc)
        imageUrls     : [],           // ảnh đã upload (URL thật)
        imagePreviews : []            // preview đang chờ upload
      }))
    }
  },
  { immediate: true }
)

const close = () => emit('close')

// còn được thêm bao nhiêu ảnh nữa?
const remainSlots = (item) =>
  MAX_IMAGES - ((item.imageUrls?.length || 0) + (item.imagePreviews?.length || 0))
const canAddMore = (item) => remainSlots(item) > 0

// Upload nhiều ảnh 1 lần (batch)
const handleImageUpload = async (event, item) => {
  const picked = Array.from(event.target.files || [])
  if (!picked.length) return

  const room = remainSlots(item)
  if (room <= 0) {
    toastRef.value?.showToast(`Mỗi sản phẩm chỉ tối đa ${MAX_IMAGES} ảnh.`, 'warning')
    event.target.value = ''
    return
  }

  // chỉ lấy vừa đủ slot
  const toSend = picked.slice(0, room)
  if (picked.length > toSend.length) {
    toastRef.value?.showToast(`Chỉ thêm được ${room} ảnh nữa (tối đa ${MAX_IMAGES}).`, 'info')
  }

  // thêm preview tạm
  const newPreviews = toSend.map(f => URL.createObjectURL(f))
  item.imagePreviews.push(...newPreviews)

  uploadingImagesCount.value++
  try {
    const res = await uploadReturnImages(toSend) // trả về mảng URL
    const urls = res?.data?.data || []

    // xoá đúng số preview vừa thêm (ở cuối) rồi thay bằng URL thật
    item.imagePreviews.splice(item.imagePreviews.length - newPreviews.length, newPreviews.length)
    item.imageUrls.push(...urls)
  } catch (err) {
    console.error(err)
    // rollback preview đã thêm nếu upload lỗi
    item.imagePreviews.splice(item.imagePreviews.length - newPreviews.length, newPreviews.length)
    toastRef.value?.showToast(
      err?.response?.data?.message ||
      'Upload ảnh thất bại. Kiểm tra định dạng (JPG/PNG/WebP/GIF) & dung lượng ≤ 5MB.',
      'error'
    )
  } finally {
    uploadingImagesCount.value--
    event.target.value = '' // reset input
  }
}

const removeUploaded = (item, index) => {  // xoá ảnh đã upload
  item.imageUrls.splice(index, 1)
}
const removePreview = (item, index) => {   // xoá preview đang chờ
  item.imagePreviews.splice(index, 1)
}

const selectedCount = computed(() =>
  selectedItems.value
    .filter(i => i.isSelected)
    .reduce((sum, i) => sum + (i.returnQuantity || 0), 0)
)

const submit = async () => {
  if (uploadingImagesCount.value > 0) {
    toastRef.value?.showToast('Vui lòng chờ ảnh tải lên xong rồi mới gửi yêu cầu.', 'warning')
    return
  }

  // Lý do chung bắt buộc khi trả >= 2 sản phẩm
  if (selectedCount.value >= 2 && !reason.value.trim()) {
    toastRef.value?.showToast('Vui lòng nhập lý do chung khi trả nhiều sản phẩm.', 'error')
    return
  }

  loading.value = true
  try {
    const items = []

    for (const item of selectedItems.value) {
      if (!item.isSelected) continue

      const qty = Number(item.returnQuantity || 0)
      if (qty < 1 || qty > item.quantity) {
        toastRef.value?.showToast(`Số lượng trả không hợp lệ cho "${item.productName}".`, 'warning')
        loading.value = false
        return
      }

      const perItemReason = (item.itemNote || '').trim()
      if (!perItemReason) {
        toastRef.value?.showToast(`Vui lòng nhập lý do cho sản phẩm "${item.productName}".`, 'error')
        loading.value = false
        return
      }

      items.push({
        orderDetailId: item.orderDetailId,
        quantity     : qty,
        note         : perItemReason,
        imageUrls    : (item.imageUrls || []).slice(0, MAX_IMAGES) // phòng hờ
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
      items
    }

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
}

const adjustQuantity = (item, delta) => {
  const next = Number(item.returnQuantity || 0) + delta
  if (next >= 1 && next <= item.quantity) item.returnQuantity = next
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
  type="number" 
  min="1" 
  :max="item.quantity" 
  v-model.number="item.returnQuantity"
  @input="
    item.returnQuantity = 
      item.returnQuantity > item.quantity 
        ? item.quantity 
        : item.returnQuantity < 1 
          ? 1 
          : item.returnQuantity
  "
/>

                    <button @click="adjustQuantity(item, 1)">+</button>
                  </div>
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
                    <!-- Ảnh đã upload -->
                    <div v-for="(url, i) in item.imageUrls" :key="'u'+i" class="preview-thumb-wrapper">
                      <img :src="url" class="preview-thumb" alt="Ảnh đã upload" />
                      <button class="remove-image-btn" @click="removeUploaded(item, i)">×</button>
                    </div>

                    <!-- Preview đang chờ -->
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
</style>
