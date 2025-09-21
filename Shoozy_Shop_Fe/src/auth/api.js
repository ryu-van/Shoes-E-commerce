import axios from 'axios';
import router from '@/router';

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add request interceptor to include JWT token
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Flag to prevent multiple logout attempts
let isLoggingOut = false;

// Add response interceptor for error handling
apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        // Chỉ xử lý 401 nếu KHÔNG phải là request login
        if ((error.response?.status === 401 || error.response?.status === 403) && !isLoggingOut) {
            const requestUrl = error.config?.url;

            // Không redirect nếu đang ở trang login hoặc đang thực hiện login
            if (requestUrl && !requestUrl.includes('/auth/login')) {
                isLoggingOut = true;

                try {
                    // Token invalid hoặc expired, clear token
                    localStorage.removeItem('token');
                    localStorage.removeItem('user');
                    localStorage.removeItem('userRole');

                    // Clear any other auth-related state if using Vuex/Pinia
                    // Example: store.dispatch('auth/logout');

                    // Navigate to login page
                    if (router && router.currentRoute.value.path !== '/login') {
                        await router.replace({
                            path: '/login',
                            query: { redirect: router.currentRoute.value.fullPath }
                        });
                    }
                } catch (navigationError) {
                    console.error('Navigation error:', navigationError);
                    // Fallback to direct navigation
                    window.location.href = '/login';
                } finally {
                    // Reset flag after a delay
                    setTimeout(() => {
                        isLoggingOut = false;
                    }, 1000);
                }
            }
        }
        return Promise.reject(error);
    }
);

export default apiClient;