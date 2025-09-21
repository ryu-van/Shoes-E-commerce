<script setup>
import { ref, onMounted, watch } from 'vue'
import { createAddress, getAllAddresses, deleteAddress, updateAddress, getAddressById, setAddressSelected } from '@/service/AddressApi'

const props = defineProps({
  userId: { type: [Number, String], required: true }
})

const emit = defineEmits(['save', 'close'])

const BASE_URL_API = 'https://provinces.open-api.vn/api'
const provinces = ref([])
const districts = ref([])
const wards = ref([])

const selectedProvince = ref('')
const selectedDistrict = ref('')
const selectedWard = ref('')
const detailAddress   = ref('')

// Validation errors
const errors = ref({
  province: '',
  district: '',
  ward: '',
  detailAddress: ''
})

const addresses  = ref([])
const loading    = ref(false)
const submitting = ref(false)
const editingId  = ref(null)

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const findByCode = (list, code) =>
    list.find(i => String(i.code) === String(code)) || null

const loadProvinces = async () => {
  const res = await fetch(`${BASE_URL_API}/p/`)
  provinces.value = await res.json()
}

const loadAddresses = async () => {
  if (!props.userId) return
  loading.value = true
  try {
    const res  = await getAllAddresses(props.userId)
    const list = res?.data?.data || res?.data || []
    // Tạo 1 dòng hiển thị theo yêu cầu
    addresses.value = list.map(a => ({
      ...a,
      line: [a.addressDetail, a.wardName, a.districtName, a.provinceName]
          .filter(Boolean)
          .join(', ')
    }))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadProvinces()
  await loadAddresses()
})

watch(selectedProvince, async (code) => {
  selectedDistrict.value = ''
  selectedWard.value = ''
  districts.value = []
  wards.value = []
  // Clear related errors
  errors.value.province = ''
  errors.value.district = ''
  errors.value.ward = ''

  if (code) {
    const res = await fetch(`${BASE_URL_API}/p/${code}?depth=2`)
    const data = await res.json()
    districts.value = data.districts || []
  }
})

watch(selectedDistrict, async (code) => {
  selectedWard.value = ''
  wards.value = []
  // Clear related errors
  errors.value.district = ''
  errors.value.ward = ''

  if (code) {
    const res = await fetch(`${BASE_URL_API}/d/${code}?depth=2`)
    const data = await res.json()
    wards.value = data.wards || []
  }
})

// Clear errors when user starts typing/selecting
watch(selectedProvince, () => { errors.value.province = '' })
watch(selectedDistrict, () => { errors.value.district = '' })
watch(selectedWard, () => { errors.value.ward = '' })
watch(detailAddress, () => { errors.value.detailAddress = '' })

const validateFields = () => {
  let isValid = true

  // Reset errors
  errors.value = {
    province: '',
    district: '',
    ward: '',
    detailAddress: ''
  }

  // Validate province
  if (!selectedProvince.value) {
    errors.value.province = 'Vui lòng chọn tỉnh/thành phố'
    isValid = false
  }

  // Validate district
  if (!selectedDistrict.value) {
    errors.value.district = 'Vui lòng chọn quận/huyện'
    isValid = false
  }

  // Validate ward
  if (!selectedWard.value) {
    errors.value.ward = 'Vui lòng chọn phường/xã'
    isValid = false
  }

  // Validate detail address
  if (!detailAddress.value.trim()) {
    errors.value.detailAddress = 'Vui lòng nhập địa chỉ chi tiết'
    isValid = false
  } else if (detailAddress.value.trim().length > 255) {
    errors.value.detailAddress = 'Địa chỉ chi tiết không được vượt quá 255 ký tự'
    isValid = false
  }

  return isValid
}

const resetForm = () => {
  selectedProvince.value = ''
  selectedDistrict.value = ''
  selectedWard.value = ''
  detailAddress.value = ''
  editingId.value = null
  // Reset errors
  errors.value = {
    province: '',
    district: '',
    ward: '',
    detailAddress: ''
  }
}

const handleSave = async () => {
  if (!validateFields()) {
    return
  }

  const p = findByCode(provinces.value, selectedProvince.value)
  const d = findByCode(districts.value,  selectedDistrict.value)
  const w = findByCode(wards.value,      selectedWard.value)

  // Payload theo BE (snake_case)
  const payload = {
    province_code: Number(selectedProvince.value),
    province_name: p?.name || '',
    district_code: Number(selectedDistrict.value),
    district_name: d?.name || '',
    ward_code: Number(selectedWard.value),
    ward_name: w?.name || '',
    address_detail: detailAddress.value.trim(),
    is_selected: false,
    user_id: Number(props.userId || user.value.id)
  }

  submitting.value = true
  try {
    let res
    if (editingId.value) {
      res = await updateAddress(editingId.value, payload)
    } else {
      res = await createAddress(payload)
    }

    // Tạo đối tượng địa chỉ có định dạng đúng để trả về cho component cha
    const formattedAddress = {
      data: {
        ...payload,
        // Chuyển snake_case sang camelCase
        provinceCode: payload.province_code,
        provinceName: payload.province_name,
        districtCode: payload.district_code,
        districtName: payload.district_name,
        wardCode: payload.ward_code,
        wardName: payload.ward_name,
        addressDetail: payload.address_detail,
        isSelected: payload.is_selected,
        userId: payload.user_id,
        // Tạo trường line để hiển thị
        line: `${payload.address_detail}, ${payload.ward_name}, ${payload.district_name}, ${payload.province_name}`
      }
    }

    emit('save', formattedAddress)
    await loadAddresses()
    resetForm()
  } catch (e) {
    console.error(e)
    alert(editingId.value ? 'Cập nhật địa chỉ thất bại.' : 'Lưu địa chỉ thất bại.')
  } finally {
    submitting.value = false
  }
}

const handleEdit = async (item) => {
  // Backend trả camelCase -> map đúng field
  selectedProvince.value = item.provinceCode || ''
  if (selectedProvince.value) {
    const res = await fetch(`${BASE_URL_API}/p/${selectedProvince.value}?depth=2`)
    const data = await res.json()
    districts.value = data.districts || []
  }
  selectedDistrict.value = item.districtCode || ''
  if (selectedDistrict.value) {
    const res2 = await fetch(`${BASE_URL_API}/d/${selectedDistrict.value}?depth=2`)
    const data2 = await res2.json()
    wards.value = data2.wards || []
  }
  selectedWard.value  = item.wardCode || ''
  detailAddress.value = item.addressDetail || ''
  editingId.value     = item.id
}

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc muốn xóa địa chỉ này?')) return
  try {
    await deleteAddress(id)
    await loadAddresses()
    if (editingId.value === id) resetForm()
  } catch (e) {
    console.error(e)
    alert('Xóa địa chỉ thất bại.')
  }
}

const handleSetDefault = async (id) => {
  try {
    await setAddressSelected(id, props.userId)
    await loadAddresses()
    // Thông báo cho component cha biết địa chỉ mặc định đã thay đổi
    const selectedAddress = addresses.value.find(addr => addr.id === id)
    if (selectedAddress) {
      // Đảm bảo địa chỉ có trường line để hiển thị
      if (!selectedAddress.line && selectedAddress.addressDetail) {
        selectedAddress.line = `${selectedAddress.addressDetail}, ${selectedAddress.wardName}, ${selectedAddress.districtName}, ${selectedAddress.provinceName}`;
      }
      console.log('Địa chỉ mặc định được chọn:', selectedAddress);
      emit('save', { data: selectedAddress })
    }
  } catch (e) {
    console.error(e)
    alert('Đặt địa chỉ mặc định thất bại.')
  }
}
</script>

<template>
  <div class="address-modal-overlay" @click.self="emit('close')">
    <div class="address-modal-center card shadow-lg">
      <div class="modal-header-custom">
        <span class="modal-title-custom">
          {{ editingId ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới' }}
        </span>
        <button type="button" class="btn-close" aria-label="Đóng" @click="emit('close')"></button>
      </div>

      <div class="modal-body-custom">
        <!-- FORM -->
        <div class="addr-grid">
          <div>
            <label class="form-label">Tỉnh/Thành</label>
            <select
                class="form-select"
                :class="{ 'is-invalid': errors.province }"
                v-model="selectedProvince"
            >
              <option value="" disabled>Chọn tỉnh/thành phố</option>
              <option v-for="p in provinces" :key="p.code" :value="p.code">{{ p.name }}</option>
            </select>
            <div v-if="errors.province" class="invalid-feedback">{{ errors.province }}</div>
          </div>

          <div>
            <label class="form-label">Quận/Huyện</label>
            <select
                class="form-select"
                :class="{ 'is-invalid': errors.district }"
                v-model="selectedDistrict"
                :disabled="!districts.length"
            >
              <option value="" disabled>Chọn quận/huyện</option>
              <option v-for="d in districts" :key="d.code" :value="d.code">{{ d.name }}</option>
            </select>
            <div v-if="errors.district" class="invalid-feedback">{{ errors.district }}</div>
          </div>

          <div>
            <label class="form-label">Phường/Xã</label>
            <select
                class="form-select"
                :class="{ 'is-invalid': errors.ward }"
                v-model="selectedWard"
                :disabled="!wards.length"
            >
              <option value="" disabled>Chọn phường/xã</option>
              <option v-for="w in wards" :key="w.code" :value="w.code">{{ w.name }}</option>
            </select>
            <div v-if="errors.ward" class="invalid-feedback">{{ errors.ward }}</div>
          </div>

          <div class="addr-detail">
            <div class="d-flex justify-content-between align-items-center">
              <label class="form-label">Địa chỉ chi tiết</label>
              <small class="form-text text-muted">{{ detailAddress.length }}/255 ký tự</small>
            </div>

            <input
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.detailAddress }"
                v-model="detailAddress"
                placeholder="Số nhà, tên đường..."
                maxlength="255"
            />
            <div v-if="errors.detailAddress" class="invalid-feedback">{{ errors.detailAddress }}</div>
          </div>

          <div class="addr-actions">
            <button class="btn btn-dark w-100" :disabled="submitting" @click="handleSave">
              {{ submitting ? 'Đang lưu...' : (editingId ? 'Cập nhật' : 'Lưu') }}
            </button>
            <button v-if="editingId" class="btn btn-outline-secondary w-100 mt-2" @click="resetForm">
              Hủy sửa
            </button>
          </div>
        </div>

        <!-- DANH SÁCH ĐỊA CHỈ -->
        <div class="address-list mt-4">
          <div class="d-flex align-items-center justify-content-between mb-2">
            <h5 class="m-0">Danh sách địa chỉ</h5>
            <small v-if="loading" class="text-muted">Đang tải...</small>
          </div>

          <div v-if="!addresses.length && !loading" class="text-muted">
            Chưa có địa chỉ nào.
          </div>

          <ul class="addr-list">
            <li v-for="item in addresses" :key="item.id" class="addr-row">
              <div class="addr-info">
                <div class="addr-line">
                  <strong>{{ item.line }}</strong>
                  <span v-if="item.isSelected" class="badge bg-dark ms-2">Mặc định</span>
                </div>
              </div>
              <div class="addr-row-actions">
                <button
                    class="btn btn-sm btn-outline-secondary me-2"
                    :disabled="item.isSelected"
                    title="Đặt làm mặc định"
                    @click="handleSetDefault(item.id)"
                >
                  {{ item.isSelected ? 'Mặc định' : 'Đặt mặc định' }}
                </button>

                <button class="btn btn-sm btn-outline-dark me-2" @click="handleEdit(item)">
                  Chỉnh sửa
                </button>
                <button class="btn btn-sm btn-outline-danger" @click="handleDelete(item.id)">
                  Xóa
                </button>
              </div>
            </li>
          </ul>
        </div>
      </div> <!-- /modal-body-custom -->
    </div>
  </div>
</template>

<style scoped>
.address-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.35);
  z-index: 2000;
  display: flex;
  align-items: center; /* Thay đổi từ flex-end thành center để hiển thị ở giữa màn hình */
  justify-content: center;
  padding: 24px;
}
.address-modal-center {
  width: 100%;
  max-width: 1160px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 16px 48px rgba(0,0,0,0.2);
  animation: fadeInModal 0.25s ease; /* Thay đổi animation */
  overflow: hidden;
  max-height: 90vh; /* Giới hạn chiều cao tối đa */
  overflow-y: auto; /* Cho phép cuộn nếu nội dung quá dài */
}
@keyframes fadeInModal {
  from { opacity: 0; transform: scale(0.95); }
  to   { opacity: 1; transform: scale(1); }
}
.modal-header-custom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.1rem 1.25rem;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}
.modal-title-custom {
  font-size: 1.2rem;
  font-weight: 700;
  color: #111;
}
.btn-close {
  background: none;
  border: none;
  font-size: 1.3rem;
  color: #888;
  cursor: pointer;
  transition: color 0.2s;
}
.btn-close:hover { color: #111; }
.modal-body-custom {
  padding: 1rem 1.25rem 1.25rem 1.25rem;
}
/* FORM grid */
.addr-grid {
  display: grid;
  grid-template-columns: 1.1fr 1.1fr 1.1fr 2fr 0.9fr;
  gap: 12px;
  align-items: start;
}

.addr-grid > div {
  min-height: 75px; /* Đặt chiều cao tối thiểu để đảm bảo đồng nhất */
  display: flex;
  flex-direction: column;
}

.addr-actions {
  margin-top: 29px; /* Đẩy button xuống dưới */
}
.addr-detail input { width: 100%; }

.form-label {
  font-weight: 600;
  margin-bottom: 0.35rem;
  color: #222;
}
.form-select, .form-control {
  border-radius: 10px;
  font-size: 0.98rem;
  border: 1px solid #d0d7de;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.form-select:focus, .form-control:focus {
  border-color: #111;
  box-shadow: 0 0 0 2px rgba(0,0,0,0.05);
}

/* Validation styles */
.form-select.is-invalid, .form-control.is-invalid {
  border-color: #dc3545;
  padding-right: calc(1.5em + 0.75rem);
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 12 12' width='12' height='12' fill='none' stroke='%23dc3545'%3e%3ccircle cx='6' cy='6' r='4.5'/%3e%3cpath d='m5.8 4.6 0.4 0.8 0.4-0.8'/%3e%3cpath d='M6 9v.01'/%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right calc(0.375em + 0.1875rem) center;
  background-size: calc(0.75em + 0.375rem) calc(0.75em + 0.375rem);
}

.form-select.is-invalid:focus, .form-control.is-invalid:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 0.25rem rgba(220, 53, 69, 0.25);
}

.invalid-feedback {
  display: block;
  width: 100%;
  margin-top: 0.25rem;
  font-size: 0.875em;
  color: #dc3545;
}

.form-text {
  margin-top: 0.25rem;
  font-size: 0.875em;
}

/* LIST */
.addr-list { list-style: none; margin: 0; padding: 0; }
.addr-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 10px;
  border: 1px solid #eee;
  border-radius: 12px;
  margin-bottom: 10px;
  transition: background 0.15s, border-color 0.15s;
}
.addr-row:hover {
  background: #fafafa;
  border-color: #e5e5e5;
}
.addr-info { min-width: 0; }
.addr-line { font-size: 0.98rem; color: #111; }
.addr-row-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
@media (max-width: 900px) {
  .addr-grid { grid-template-columns: 1fr 1fr; }
  .addr-actions { grid-column: 1 / -1; }
}
@media (max-width: 600px) {
  .address-modal-center {
    max-width: 100vw;
    max-height: 95vh;
    border-radius: 16px;
  }
  .modal-header-custom,
  .modal-body-custom {
    padding-left: 0.8rem;
    padding-right: 0.8rem;
  }
  .addr-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .addr-row-actions .btn { width: 100%; }
}
</style>