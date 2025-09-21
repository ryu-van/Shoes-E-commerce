<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createUser } from '@/service/UserApis'
import ShowToastComponent from "@/components/ShowToastComponent.vue"

const props = defineProps({
  roleName: {
    type: String,
    required: true
  }
})

function getRoleNameVi(roleName) {
  switch (roleName) {
    case 'Staff': return 'Nhân viên'
    case 'Customer': return 'Khách hàng'
    default: return roleName
  }
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

const router = useRouter()
const newUser = ref({
  fullname: '',
  email: '',
  gender: true,
  phoneNumber: '',
  address: '',
  dateOfBirth: '',
  roleName: props.roleName || 'Customer',
  password: ''
})
const avatarFile = ref(null)
const avatarPreview = ref(null)
const fieldErrors = reactive({})
const generalError = ref('')
const saving = ref(false)
const toastRef = ref(null)

function handleFileUpload(event) {
  avatarFile.value = event.target.files[0]
  if (avatarFile.value) {
    avatarPreview.value = URL.createObjectURL(avatarFile.value)
  }
}

onMounted(() => {
  newUser.value.roleName = props.roleName
})

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type)
}

// ==== Đồng bộ với BE: ^0\d{9}$ ====
const PHONE_REGEX = /^0\d{9}$/

// chỉ giữ số và cắt tối đa 10 ký tự
function digits10(v = '') {
  return String(v).replace(/\D/g, '').slice(0, 10)
}

function validate() {
  let valid = true
  Object.keys(fieldErrors).forEach(k => delete fieldErrors[k])

  // Họ tên
  if (!newUser.value.fullname.trim()) {
    fieldErrors.fullname = 'Vui lòng nhập họ tên'
    valid = false
  }

  // Email
  if (!newUser.value.email.trim()) {
    fieldErrors.email = 'Vui lòng nhập email'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newUser.value.email)) {
    fieldErrors.email = 'Email không hợp lệ'
    valid = false
  }

  // Mật khẩu
  const pwd = newUser.value.password || ''
  if (!pwd) {
    fieldErrors.password = 'Vui lòng nhập mật khẩu'
    valid = false
  } else if (pwd.length < 6) {
    fieldErrors.password = 'Mật khẩu phải từ 6 ký tự trở lên'
    valid = false
  } else {
    const hasUpper = /[A-Z]/.test(pwd)
    const hasLower = /[a-z]/.test(pwd)
    const hasDigit = /\d/.test(pwd)
    if (!hasUpper || !hasLower || !hasDigit) {
      fieldErrors.password = 'Mật khẩu phải có ít nhất 1 chữ hoa, chữ thường, số'
      valid = false
    }
  }

  // Số điện thoại (đồng bộ regex với BE)
  if (!newUser.value.phoneNumber.trim()) {
    fieldErrors.phoneNumber = 'Vui lòng nhập số điện thoại'
    valid = false
  } else if (!PHONE_REGEX.test(newUser.value.phoneNumber)) {
    fieldErrors.phoneNumber = 'Số điện thoại không hợp lệ'
    valid = false
  }

  // Địa chỉ
  if (!newUser.value.address.trim()) {
    fieldErrors.address = 'Vui lòng nhập địa chỉ'
    valid = false
  }

  // Ngày sinh
  const dobStr = newUser.value.dateOfBirth
  if (!dobStr) {
    fieldErrors.dateOfBirth = 'Ngày sinh không được để trống'
    valid = false
  } else if (newUser.value.roleName === 'Staff') {
    const age = getAgeFromYyyyMmDd(dobStr)
    if (isNaN(age)) {
      fieldErrors.dateOfBirth = 'Ngày sinh không hợp lệ'
      valid = false
    } else if (age < 18) {
      fieldErrors.dateOfBirth = 'Nhân viên phải từ 18 tuổi trở lên'
      valid = false
    }
  }

  // Vai trò
  if (!newUser.value.roleName) {
    fieldErrors.roleName = 'Vui lòng chọn vai trò'
    valid = false
  }

  return valid
}

async function submit() {
  generalError.value = ''
  if (!validate()) return
  saving.value = true
  try {
    const formData = new FormData()
    formData.append('fullname', newUser.value.fullname?.trim() || '')
    formData.append('email', newUser.value.email?.trim() || '')
    // đảm bảo phone chỉ là số 10 ký tự (phù hợp regex BE)
    formData.append('phoneNumber', digits10(newUser.value.phoneNumber))
    formData.append('address', newUser.value.address?.trim() || '')
    formData.append('password', newUser.value.password || '')
    formData.append('gender', newUser.value.gender === true ? 'true' : 'false')

    const dob = new Date(newUser.value.dateOfBirth)
    if (!isNaN(dob.getTime())) {
      const yyyyMMdd = dob.toISOString().split('T')[0]
      formData.append('dateOfBirth', yyyyMMdd)
    }

    formData.append('roleName', newUser.value.roleName || 'Customer')
    if (avatarFile.value instanceof File) {
      formData.append('avatar', avatarFile.value)
    }

    await createUser(formData)
    showToast('Tạo người dùng thành công 🎉', 'success')
    setTimeout(() => {
      router.push(newUser.value.roleName === 'Staff'
        ? '/admin/users/staff'
        : '/admin/users/customer'
      )
    }, 500)
  } catch (err) {
    console.error(err)

    // Đọc lỗi từ BE (axios)
    const res = err?.response
    const data = res?.data
    const msg  = data?.message || data?.error || data?.detail || ''
    const code = data?.code || ''

    const is409      = res?.status === 409
    const isPhoneDup = is409 && (code === 'PHONE_EXISTS' || /(phone|số\s*điện\s*thoại)/i.test(msg))
    const isEmailDup = is409 && (code === 'EMAIL_EXISTS' || /email/i.test(msg))

    // 1) Ưu tiên SĐT trước
    if (isPhoneDup) {
      delete fieldErrors.email
      fieldErrors.phoneNumber = msg || 'Số điện thoại đã tồn tại'
      generalError.value = ''
      showToast(fieldErrors.phoneNumber, 'error')
      requestAnimationFrame(() => {
        document.querySelector('input[name="phoneNumber"]')?.focus()
      })
      return
    }

    // 2) Sau đó email
    if (isEmailDup) {
      delete fieldErrors.phoneNumber
      fieldErrors.email = msg || 'Email đã tồn tại'
      generalError.value = ''
      showToast(fieldErrors.email, 'error')
      requestAnimationFrame(() => {
        document.querySelector('input[type="email"]')?.focus()
      })
      return
    }

    // 3) Map lỗi validate từ BE (hỗ trợ cả list và object map)
    const list = data?.errors || data?.violations || data?.fieldErrors
    const mapObj =
      (data && typeof data?.errors === 'object' && !Array.isArray(data.errors) && data.errors) ||
      (data && typeof data?.data   === 'object' && !Array.isArray(data.data)   && data.data)   ||
      (data && typeof data?.fieldErrors === 'object' && !Array.isArray(data.fieldErrors) && data.fieldErrors) ||
      null

    if (Array.isArray(list) && list.length) {
      list.forEach(e => {
        const f = e.field || e.name
        const m = e.defaultMessage || e.message
        if (f && m) fieldErrors[f] = m
      })
      generalError.value = ''
      showToast('Vui lòng kiểm tra lại các trường nhập', 'error')
      const first = list[0]?.field || list[0]?.name
      if (first) {
        requestAnimationFrame(() => {
          document.querySelector(`[name="${first}"]`)?.focus()
        })
      }
      return
    }

    if (mapObj) {
      Object.entries(mapObj).forEach(([f, m]) => {
        if (f && m) fieldErrors[f] = String(m)
      })
      generalError.value = ''
      showToast('Vui lòng kiểm tra lại các trường nhập', 'error')
      const firstField = Object.keys(mapObj)[0]
      if (firstField) {
        requestAnimationFrame(() => {
          document.querySelector(`[name="${firstField}"]`)?.focus()
        })
      }
      return
    }

    // 4) Các lỗi khác
    generalError.value = msg || 'Có lỗi xảy ra. Vui lòng thử lại.'
    showToast(generalError.value, 'error')
  } finally {
    saving.value = false
  }
}

function cancel() {
  router.back()
}
</script>

<template>
  <div class="container-fluid py-4">
    <div class="mx-auto user-form-wrapper shadow-sm border rounded overflow-hidden">
      <h2 class="p-4 pb-0">Thêm {{ getRoleNameVi(newUser.roleName) }}</h2>

      <div class="row gx-0">
        <!-- Avatar -->
        <div class="col-md-4 bg-light d-flex flex-column align-items-center py-4 px-3">
          <h5 class="mb-3">Ảnh đại diện</h5>
          <div class="avatar-wrapper mb-2">
            <img
              v-if="avatarPreview"
              :src="avatarPreview"
              alt="Avatar Preview"
              class="avatar-img"
            />
            <div v-else class="avatar-placeholder">
              <small>No Image</small>
            </div>
          </div>
          <input type="file" class="form-control mt-2" @change="handleFileUpload" />
        </div>

        <!-- Form -->
        <div class="col-md-8 bg-white p-4">
          <form @submit.prevent="submit">
            <div v-if="generalError" class="alert alert-danger">{{ generalError }}</div>

            <div class="row mb-3">
              <div class="col-md-6 mb-3 mb-md-0">
                <label class="form-label">Họ tên</label>
                <input v-model="newUser.fullname" type="text" class="form-control"
                       :class="{ 'is-invalid': fieldErrors.fullname }"/>
                <div v-if="fieldErrors.fullname" class="invalid-feedback">{{ fieldErrors.fullname }}</div>
              </div>
              <div class="col-md-6">
                <label class="form-label">Số điện thoại</label>
                <input
                  v-model="newUser.phoneNumber"
                  name="phoneNumber"
                  type="text"
                  inputmode="numeric"
                  @input="newUser.phoneNumber = digits10($event.target.value)"
                  class="form-control"
                  :class="{ 'is-invalid': fieldErrors.phoneNumber }"
                />
                <div v-if="fieldErrors.phoneNumber" class="invalid-feedback">{{ fieldErrors.phoneNumber }}</div>
              </div>
            </div>

            <div class="row mb-3">
              <div class="col-md-6 mb-3 mb-md-0">
                <label class="form-label">Email</label>
                <input v-model="newUser.email" type="email" class="form-control"
                       :class="{ 'is-invalid': fieldErrors.email }"/>
                <div v-if="fieldErrors.email" class="invalid-feedback">{{ fieldErrors.email }}</div>
              </div>
              <div class="col-md-6">
                <label class="form-label">Mật khẩu</label>
                <input v-model="newUser.password" type="password" class="form-control"
                       :class="{ 'is-invalid': fieldErrors.password }"/>
                <div v-if="fieldErrors.password" class="invalid-feedback">{{ fieldErrors.password }}</div>
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label">Địa chỉ</label>
              <textarea v-model="newUser.address" class="form-control" rows="3"
                        :class="{ 'is-invalid': fieldErrors.address }"></textarea>
              <div v-if="fieldErrors.address" class="invalid-feedback">{{ fieldErrors.address }}</div>
            </div>

            <div class="row mb-3">
              <div class="col-md-6">
                <label class="form-label">Ngày sinh</label>
                <input v-model="newUser.dateOfBirth" type="date" class="form-control"
                       :class="{ 'is-invalid': fieldErrors.dateOfBirth }"/>
                <div v-if="fieldErrors.dateOfBirth" class="invalid-feedback">{{ fieldErrors.dateOfBirth }}</div>
              </div>

              <div class="col-md-6">
                <label class="form-label">Giới tính</label>
                <select v-model="newUser.gender" class="form-select">
                  <option :value="true">Nam</option>
                  <option :value="false">Nữ</option>
                </select>
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label">Vai trò</label>
              <select v-model="newUser.roleName" class="form-select" disabled>
                <option :value="props.roleName">{{ getRoleNameVi(props.roleName) }}</option>
              </select>
            </div>

            <div class="d-flex justify-content-end gap-2 mt-4">
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
                Thêm mới
              </button>
              <button type="button" class="btn btn-secondary" @click="cancel" :disabled="saving">
                Hủy
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
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  color: #aaa; font-size: 14px;
}
</style>
