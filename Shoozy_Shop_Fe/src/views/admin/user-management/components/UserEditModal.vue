<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ShowToastComponent from "@/components/ShowToastComponent.vue"
import { getUserById, updateUserMultipart } from '@/service/UserApis'

const route = useRoute()
const router = useRouter()

// ===== State =====
const localUser   = ref({})
const original    = ref({ email: '', phoneNumber: '' })   // GIÁ TRỊ GỐC để so sánh
const fieldErrors = reactive({})
const generalError = ref('')
const loading = ref(true)
const saving  = ref(false)
const toastRef = ref(null)

const avatarFile = ref(null)
const avatarPreview = ref(null)

const showToast = (m, t) => toastRef.value?.showToast(m, t)

// ===== Helpers =====
function getRoleNameVi(role) {
  switch (role) {
    case 'Admin': return 'Quản trị viên'
    case 'Staff': return 'Nhân viên'
    case 'Customer': return 'Khách hàng'
    default: return role
  }
}
function getStatusVi(status) { return status ? 'Hoạt động' : 'Đã khoá' }

function toYyyyMmDd(value) {
  if (!value) return ''
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) return value
  const d = new Date(value)
  return isNaN(d.getTime()) ? '' : d.toISOString().split('T')[0]
}
function getAgeFromYyyyMmDd(yyyyMmDd) {
  if (!yyyyMmDd) return NaN
  const [y, m, d] = yyyyMmDd.split('-').map(Number)
  if (!y || !m || !d) return NaN
  const today = new Date()
  let age = today.getFullYear() - y
  const hadBirthday = (today.getMonth() + 1 > m) || ((today.getMonth() + 1 === m) && (today.getDate() >= d))
  if (!hadBirthday) age--
  return age
}
const PHONE_REGEX = /^0\d{9}$/ // đồng bộ với BE
const digits10 = (v='') => String(v).replace(/\D/g,'').slice(0,10)
const normEmail = (v='') => String(v).trim().toLowerCase()

// ===== Load user =====
async function fetchUser() {
  loading.value = true
  try {
    const id = route.query.id
    if (!id) throw new Error('Không có id')
    const res = await getUserById(id)
    const u = res?.data?.data || res

    localUser.value = {
      id: u.id,
      fullname: u.fullname || '',
      email: u.email || '',
      gender: u.gender === true || u.gender === 'true',
      phoneNumber: (u.phoneNumber || ''),
      address: u.address || '',
      dateOfBirth: u.dateOfBirth ? toYyyyMmDd(u.dateOfBirth) : '',
      roleName: u.roleName || 'Customer',
      isActive: !!u.isActive,
      avatar: u.avatarUrl || u.avatar || null,
    }

    // LƯU GIÁ TRỊ GỐC để so sánh khi submit
    original.value.email       = normEmail(localUser.value.email)
    original.value.phoneNumber = digits10(localUser.value.phoneNumber)

    avatarPreview.value = localUser.value.avatar
  } catch (e) {
    console.error(e)
    showToast('Không tìm thấy người dùng.', 'error')
    // fallback nếu đi thẳng vào trang sửa bằng URL
    router.push('/admin/users/customer')
  } finally {
    loading.value = false
  }
}
onMounted(fetchUser)

// ===== Avatar =====
function handleAvatarUpload(event) {
  avatarFile.value = event.target.files[0] || null
  avatarPreview.value = avatarFile.value ? URL.createObjectURL(avatarFile.value) : localUser.value.avatar
}
function clearNewAvatar() {
  avatarFile.value = null
  avatarPreview.value = localUser.value.avatar || null
}

// ===== Go back (rollback về đúng màn trước) =====
function goBackToList() {
  if (window.history.length > 1) {
    router.back()
  } else {
    // fallback nếu mở trang sửa trực tiếp bằng URL: về danh sách theo role hiện tại
    const fallback = (localUser.value.roleName === 'Staff')
      ? '/admin/users/staff'
      : '/admin/users/customer'
    router.push(fallback)
  }
}

// ===== Validate =====
function validate() {
  let valid = true
  Object.keys(fieldErrors).forEach(k => delete fieldErrors[k])

  // Fullname
  if (!localUser.value.fullname?.trim()) {
    fieldErrors.fullname = 'Vui lòng nhập họ tên'
    valid = false
  } else if (localUser.value.fullname.trim().length > 300) {
    fieldErrors.fullname = 'Họ tên không được vượt quá 300 ký tự'
    valid = false
  }

  // Phone
  if (!localUser.value.phoneNumber?.trim()) {
    fieldErrors.phoneNumber = 'Vui lòng nhập số điện thoại'
    valid = false
  } else if (!PHONE_REGEX.test(localUser.value.phoneNumber)) {
    fieldErrors.phoneNumber = 'Số điện thoại không hợp lệ'
    valid = false
  }

  // Email
  if (!localUser.value.email?.trim()) {
    fieldErrors.email = 'Vui lòng nhập email'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(localUser.value.email)) {
    fieldErrors.email = 'Email không hợp lệ'
    valid = false
  }

  // Address
  if (!localUser.value.address?.trim()) {
    fieldErrors.address = 'Vui lòng nhập địa chỉ'
    valid = false
  } else if (localUser.value.address.trim().length > 300) {
    fieldErrors.address = 'Địa chỉ không được vượt quá 300 ký tự'
    valid = false
  }

  // DOB
  if (!localUser.value.dateOfBirth) {
    fieldErrors.dateOfBirth = 'Ngày sinh không được để trống'
    valid = false
  } else if (localUser.value.roleName === 'Staff') {
    const age = getAgeFromYyyyMmDd(localUser.value.dateOfBirth)
    if (isNaN(age)) {
      fieldErrors.dateOfBirth = 'Ngày sinh không hợp lệ'
      valid = false
    } else if (age < 18) {
      fieldErrors.dateOfBirth = 'Nhân viên phải từ 18 tuổi trở lên'
      valid = false
    }
  }

  // Role
  if (!localUser.value.roleName) {
    fieldErrors.roleName = 'Vui lòng chọn vai trò'
    valid = false
  }

  return valid
}

// ===== Submit (LUÔN multipart) =====
const lastSubmitChanged = { email: false, phone: false }

async function submit() {
  generalError.value = ''
  if (!validate()) return
  saving.value = true
  try {
    const fd  = new FormData()
    const dob = toYyyyMmDd(localUser.value.dateOfBirth)

    const nowEmail = normEmail(localUser.value.email)
    const nowPhone = digits10(localUser.value.phoneNumber)

    fd.append('fullname', (localUser.value.fullname || '').trim())
    fd.append('email', nowEmail)
    fd.append('phoneNumber', nowPhone)
    fd.append('address', (localUser.value.address || '').trim())
    if (dob) fd.append('dateOfBirth', dob)
    fd.append('gender', localUser.value.gender === true ? 'true' : 'false')
    fd.append('roleName', localUser.value.roleName || 'Customer')
    fd.append('isActive', String(!!localUser.value.isActive))
    if (avatarFile.value instanceof File) fd.append('avatar', avatarFile.value)

    // đánh dấu đã đổi để tiện xử lý 409 nếu cần
    lastSubmitChanged.email = (nowEmail !== original.value.email)
    lastSubmitChanged.phone = (nowPhone !== original.value.phoneNumber)

    await updateUserMultipart(localUser.value.id, fd)

    // cập nhật lại gốc
    original.value.email       = nowEmail
    original.value.phoneNumber = nowPhone

    showToast('Cập nhật thành công 🎉', 'success')
    setTimeout(() => { goBackToList() }, 800)
  } catch (err) {
    handleUpdateError(err)
  } finally {
    saving.value = false
  }
}

// ===== Error mapping =====
function handleUpdateError(err) {
  console.error(err)
  const res  = err?.response
  const data = res?.data || {}
  const msg  = data?.message || data?.error || data?.detail || ''
  const code = data?.code || data?.data?.code || data?.errorCode || ''

  // 409: trùng
  if (res?.status === 409) {
    if (code === 'PHONE_EXISTS' || /(phone|số\s*điện\s*thoại)/i.test(msg)) {
      delete fieldErrors.email
      fieldErrors.phoneNumber = msg || 'Số điện thoại đã tồn tại'
      showToast(fieldErrors.phoneNumber, 'error')
      requestAnimationFrame(() => {
        document.querySelector('input[name="phoneNumber"]')?.focus()
      })
      return
    }
    if (code === 'EMAIL_EXISTS' || /email/i.test(msg)) {
      delete fieldErrors.phoneNumber
      fieldErrors.email = msg || 'Email đã tồn tại'
      showToast(fieldErrors.email, 'error')
      requestAnimationFrame(() => {
        document.querySelector('input[type="email"]')?.focus()
      })
      return
    }
  }

  // mảng lỗi field
  const list = data?.errors || data?.violations || data?.fieldErrors
  if (Array.isArray(list) && list.length) {
    Object.keys(fieldErrors).forEach(k => delete fieldErrors[k])
    list.forEach(e => {
      const f = e.field || e.name
      const m = e.defaultMessage || e.message
      if (f && m) fieldErrors[f] = m
    })
    generalError.value = 'Vui lòng kiểm tra lại các trường nhập'
    showToast(generalError.value, 'error')
    const first = list[0]?.field || list[0]?.name
    if (first) requestAnimationFrame(() => {
      document.querySelector(`[name="${first}"]`)?.focus()
    })
    return
  }

  // object lỗi field
  const possibleMap = data?.fieldErrors || data?.errors || null
  const formKeys = new Set(['fullname','email','phoneNumber','address','dateOfBirth','roleName','isActive','gender'])
  if (possibleMap && typeof possibleMap === 'object' && !Array.isArray(possibleMap)) {
    const entries = Object.entries(possibleMap).filter(([k]) => formKeys.has(k))
    if (entries.length) {
      Object.keys(fieldErrors).forEach(k => delete fieldErrors[k])
      entries.forEach(([f, m]) => { fieldErrors[f] = String(m) })
      generalError.value = 'Vui lòng kiểm tra lại các trường nhập'
      showToast(generalError.value, 'error')
      const firstField = entries[0][0]
      requestAnimationFrame(() => {
        document.querySelector(`[name="${firstField}"]`)?.focus()
      })
      return
    }
  }

  if (res?.status === 415) {
    generalError.value = 'Kiểu dữ liệu gửi lên không được hỗ trợ (415).'
    showToast(generalError.value, 'error')
    return
  }
  generalError.value = msg || 'Có lỗi xảy ra, vui lòng thử lại.'
  showToast(generalError.value, 'error')
}
</script>

<template>
  <div class="container-fluid py-4">
    <div v-if="loading" class="text-center">
      <div class="spinner-border text-primary"></div>
    </div>

    <div v-else class="mx-auto user-form-wrapper shadow-sm border rounded overflow-hidden">
      <h2 class="p-4 pb-0">Sửa người dùng</h2>
      <div class="row gx-0">
        <!-- Avatar -->
        <div class="col-md-4 bg-light d-flex flex-column align-items-center py-4 px-3">
          <h5 class="mb-3">Ảnh đại diện</h5>

          <div class="avatar-wrapper mb-2">
            <img v-if="avatarPreview" :src="avatarPreview" alt="Avatar" class="avatar-img" />
            <div v-else class="avatar-placeholder"><small>No Image</small></div>
          </div>

          <div class="d-flex gap-2 w-100 px-2">
            <input type="file" class="form-control" @change="handleAvatarUpload" />
            <button type="button" class="btn btn-outline-secondary" @click="clearNewAvatar">Bỏ chọn</button>
          </div>
        </div>

        <!-- Form -->
        <div class="col-md-8 bg-white p-4">
          <form @submit.prevent="submit">
            <div v-if="generalError" class="alert alert-danger">{{ generalError }}</div>

            <div class="row mb-3">
              <div class="col-md-6 mb-3 mb-md-0">
                <label class="form-label">Họ tên</label>
                <input v-model="localUser.fullname" type="text" class="form-control"
                       :class="{ 'is-invalid': fieldErrors.fullname }"/>
                <div v-if="fieldErrors.fullname" class="invalid-feedback">{{ fieldErrors.fullname }}</div>
              </div>
              <div class="col-md-6">
                <label class="form-label">Số điện thoại</label>
                <input
                  v-model="localUser.phoneNumber"
                  name="phoneNumber"
                  type="text"
                  inputmode="numeric"
                  @input="localUser.phoneNumber = digits10($event.target.value)"
                  class="form-control"
                  :class="{ 'is-invalid': fieldErrors.phoneNumber }"
                />
                <div v-if="fieldErrors.phoneNumber" class="invalid-feedback">{{ fieldErrors.phoneNumber }}</div>
              </div>
            </div>

            <div class="row mb-3">
              <div class="col-md-6 mb-3 mb-md-0">
                <label class="form-label">Email</label>
                <input v-model="localUser.email" type="email" class="form-control"
                       :class="{ 'is-invalid': fieldErrors.email }"/>
                <div v-if="fieldErrors.email" class="invalid-feedback">{{ fieldErrors.email }}</div>
              </div>
              <div class="col-md-6">
                <label class="form-label">Địa chỉ</label>
                <input v-model="localUser.address" type="text" class="form-control"
                       :class="{ 'is-invalid': fieldErrors.address }"/>
                <div v-if="fieldErrors.address" class="invalid-feedback">{{ fieldErrors.address }}</div>
              </div>
            </div>

            <div class="row mb-3">
              <div class="col-md-6">
                <label class="form-label">Ngày sinh</label>
                <input v-model="localUser.dateOfBirth" type="date" class="form-control"
                       :class="{ 'is-invalid': fieldErrors.dateOfBirth }"/>
                <div v-if="fieldErrors.dateOfBirth" class="invalid-feedback">{{ fieldErrors.dateOfBirth }}</div>
              </div>

              <div class="col-md-6">
                <label class="form-label">Giới tính</label>
                <select v-model="localUser.gender" class="form-select">
                  <option :value="true">Nam</option>
                  <option :value="false">Nữ</option>
                </select>
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label">Vai trò</label>
              <select v-model="localUser.roleName" class="form-select" :class="{ 'is-invalid': fieldErrors.roleName }">
                <option value="Admin">{{ getRoleNameVi('Admin') }}</option>
                <option value="Staff">{{ getRoleNameVi('Staff') }}</option>
                <option value="Customer">{{ getRoleNameVi('Customer') }}</option>
              </select>
              <div v-if="fieldErrors.roleName" class="invalid-feedback">{{ fieldErrors.roleName }}</div>
            </div>

            <div class="mb-3">
              <label class="form-label">Trạng thái</label>
              <select v-model="localUser.isActive" class="form-select">
                <option :value="true">{{ getStatusVi(true) }}</option>
                <option :value="false">{{ getStatusVi(false) }}</option>
              </select>
            </div>

            <div class="d-flex justify-content-end gap-2 mt-4">
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
                Lưu
              </button>
              <button type="button" class="btn btn-secondary" @click="goBackToList" :disabled="saving">
                Huỷ
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <ShowToastComponent ref="toastRef" />
  </div>
</template>

<style scoped>
.user-form-wrapper {
  max-width: 1200px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
@media (max-width: 768px) {
  .user-form-wrapper { max-width: 95%; }
}
.avatar-wrapper {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  border: 3px solid #e0e0e0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  background-color: #fff;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  color: #aaa; font-size: 14px;
}
</style>
