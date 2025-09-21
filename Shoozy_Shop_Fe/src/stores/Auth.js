import { defineStore } from 'pinia'
import apiClient from "@/auth/api.js"
import router from '@/router'

let logoutTimerId = null

function decodeJwtExp(token) {
    try {
        const seg = token.split('.')[1]
        const pad = '='.repeat((4 - (seg.length % 4)) % 4) // fix padding
        const json = atob(seg.replace(/-/g, '+').replace(/_/g, '/') + pad)
        const payload = JSON.parse(json)
        return payload?.exp || null // seconds
    } catch { return null }
}

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(localStorage.getItem('user')) || null,
        token: localStorage.getItem('token') || null,
        userRole: localStorage.getItem('userRole') || null,
    }),

    actions: {
        scheduleAutoLogoutFromToken(token) {
            if (logoutTimerId) clearTimeout(logoutTimerId)
            if (!token) return
            const exp = decodeJwtExp(token)
            if (!exp) return
            const delay = Math.max(exp * 1000 - Date.now() - 5000, 0) // sớm 5s

            logoutTimerId = setTimeout(async () => {
                // hết hạn -> dọn state + điều hướng
                this.clearAuth({ code: 'TOKEN_EXPIRED', message: 'Phiên đăng nhập đã hết hạn' })
                if (router.currentRoute.value.path !== '/login') {
                    await router.replace({ path: '/login', query: { reason: 'expired' } })
                }
            }, delay)
        },

        clearAutoLogoutTimer() {
            if (logoutTimerId) {
                clearTimeout(logoutTimerId)
                logoutTimerId = null
            }
        },

        async login(email, password) {
            try {
                const response = await apiClient.post('/auth/login', { email, password })
                this.token = response.data.data.accessToken
                this.userRole = response.data.data.role || 'Customer'
                localStorage.setItem('token', this.token)
                localStorage.setItem('userRole', this.userRole)

                this.scheduleAutoLogoutFromToken(this.token)

                const profileResponse = await apiClient.get('/users/profile')
                this.user = {
                    id: profileResponse.data.data.id,
                    avatar: profileResponse.data.data.avatar,
                    email: profileResponse.data.data.email,
                    fullname: profileResponse.data.data.fullname,
                    gender: profileResponse.data.data.gender,
                    phoneNumber: profileResponse.data.data.phoneNumber,
                    address: profileResponse.data.data.address,
                    dateOfBirth: profileResponse.data.data.dateOfBirth,
                }
                localStorage.setItem('user', JSON.stringify(this.user))
                return true
            } catch (error) {
                if (error.response?.data?.message) throw error.response.data.message
                else if (error.response?.data) throw error.response.data
                else throw error.message || 'Đăng nhập thất bại!'
            }
        },

        async register({ fullname, email, phoneNumber, password, confirmPassword }) {
            try {
                const response = await apiClient.post('/auth/register', {
                    fullname,
                    email,
                    phoneNumber,
                    password,
                    confirmPassword,
                })
                this.token = response.data.data.accessToken
                this.userRole = response.data.data.role || 'Customer'
                localStorage.setItem('token', this.token)
                localStorage.setItem('userRole', this.userRole)

                this.scheduleAutoLogoutFromToken(this.token)

                const profileResponse = await apiClient.get('/users/profile')
                this.user = {
                    id: profileResponse.data.data.id,
                    avatar: profileResponse.data.data.avatar,
                    email: profileResponse.data.data.email,
                    fullname: profileResponse.data.data.fullname,
                    gender: profileResponse.data.data.gender,
                    phoneNumber: profileResponse.data.data.phoneNumber,
                    address: profileResponse.data.data.address,
                    dateOfBirth: profileResponse.data.data.dateOfBirth,
                }
                localStorage.setItem('user', JSON.stringify(this.user))
                return true
            } catch (error) {
                if (error.response?.data?.message) throw error.response.data.message
                else if (error.response?.data) throw error.response.data
                else throw error.message || 'Đăng ký thất bại!'
            }
        },

        async logout() {
            try {
                await apiClient.post('/auth/logout', null, {
                    headers: { Authorization: `Bearer ${this.token}` },
                })
            } catch (error) {
                console.error('Đăng xuất thất bại:', error.response?.data?.data || error.message)
            } finally {
                this.clearAuth()
                if (router.currentRoute.value.path !== '/login') {
                    await router.replace({ path: '/login' })
                }
            }
        },

        async forgotPassword(email) {
            try {
                await apiClient.post('/auth/forgot-password', { email })
                return true
            } catch (error) {
                console.error('Quên mật khẩu thất bại:', error.response?.data?.data || error.message)
                throw error.response?.data?.data || error.message
            }
        },

        async resetPassword(token, newPassword) {
            try {
                await apiClient.post('/auth/reset-password', { token, newPassword })
                return true
            } catch (error) {
                console.error('Đặt lại mật khẩu thất bại:', error.response?.data?.data || error.message)
                throw error.response?.data?.data || error.message
            }
        },

        async checkAuth() {
            const token = localStorage.getItem('token')
            if (token) {
                this.token = token
                this.userRole = localStorage.getItem('userRole') || 'Customer'
                this.scheduleAutoLogoutFromToken(this.token)

                try {
                    const profileResponse = await apiClient.get('/users/profile')
                    this.user = {
                        id: profileResponse.data.data.id,
                        avatar: profileResponse.data.data.avatar,
                        email: profileResponse.data.data.email,
                        fullname: profileResponse.data.data.fullname,
                        gender: profileResponse.data.data.gender,
                        phoneNumber: profileResponse.data.data.phoneNumber,
                        address: profileResponse.data.data.address,
                        dateOfBirth: profileResponse.data.data.dateOfBirth,
                    }
                    this.userRole = profileResponse.data.data.roleName || this.userRole
                    localStorage.setItem('user', JSON.stringify(this.user))
                    localStorage.setItem('userRole', this.userRole)
                } catch (error) {
                    const code = error?.response?.data?.code
                    const msg = error?.response?.data?.message
                    this.clearAuth({ code, message: msg })
                }
            } else {
                this.clearAuth()
            }
        },

        clearAuth(reason) {
            this.clearAutoLogoutTimer()

            this.user = null
            this.token = null
            this.userRole = null
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            localStorage.removeItem('userRole')

            if (reason) {
                sessionStorage.setItem('logoutReason', reason.code || 'ACCOUNT_LOCKED')
                sessionStorage.setItem('logoutMessage', reason.message || 'Tài khoản đã bị khoá')
            }
        },

        hasPermission(requiredRoles) {
            return requiredRoles.includes(this.userRole)
        },
    },

    getters: {
        isAuthenticated: (state) => !!state.token,
        getUser: (state) => state.user,
        getUserRole: (state) => state.userRole,
    },
})
