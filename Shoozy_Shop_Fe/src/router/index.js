import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from "@/stores/Auth.js";
import CategoryManagement from '@/views/admin/product-management/category/CategoryManagement.vue';
import ProductManagement from '@/views/admin/product-management/product/ProductManagement.vue';
import BrandManagement from '@/views/admin/product-management/brand/BrandManagement.vue';
import MaterialManagement from '@/views/admin/product-management/material/MaterialManagement.vue';
import ColorManagement from '@/views/admin/product-management/color/ColorManagement.vue';
import SizeManagement from '@/views/admin/product-management/size/SizeManagement.vue';
import ProductForm from '@/views/admin/product-management/product/ProductForm.vue';
import PromotionManagement from '@/views/admin/promotion-management/PromotionPage.vue';
import CouponManagement from '@/views/admin/coupon-management/Coupon-Page.vue';
import FormPromotion from '@/views/admin/promotion-management/PromotionForm.vue';
import InvoiceManagement from '@/views/admin/invoice-management/InvoiceManagement.vue';
import StatisticalView from '@/views/admin/statistical-management/StatisticalView.vue';
import ReviewManagement from '@/views/admin/review-management/ReviewManagement.vue';
import UserManagement from "@/views/admin/user-management/UserManagement.vue";
import FormCoupon from '@/views/admin/coupon-management/CouponForm.vue';
import AdminLayout from '@/layouts/admin/AdminLayout.vue';
import UserLayout from '@/layouts/user/UserLayout.vue';
import HomeView from '@/views/user/HomeView.vue';
import CartView from '@/views/user/order/CartView.vue';
import ProductDetailView from '@/views/user/product/ProductDetailView.vue';
import ProductView from '@/views/user/product/ProductView.vue';
import LoginView from '@/views/user/account/LoginView.vue';
import OrderManagement from '@/views/admin/order-management/OrderManagement.vue';
import OrderDetailManagement from '@/views/admin/order-management/OrderDetailManagement.vue';
import UnauthorizedView from "@/views/UnauthorizedView.vue";
import ForgotPasswordView from '@/views/user/account/ForgotPasswordView.vue'; // From previous setup

import ProfileView from "@/views/user/account/ProfileView.vue";
import RegisterView from "@/views/user/account/RegisterView.vue"; // From previous setup
import ResetPasswordView from '@/views/user/account/ResetPasswordView.vue'; // From previous setup
import CheckOut from '@/views/user/order/CheckOut.vue';
import OrderSuccess from '@/views/user/order/OrderSuccess.vue';
import OrderManagementView from "@/views/user/order/OrderManagementView.vue";
import OrderDetailView from "@/views/user/order/OrderDetailView.vue"; // From previous setup
import UserCreateForm from '@/views/admin/user-management/components/UserCreateModal.vue';
import ChatBotComponent from "@/views/user/chat-bot/ChatBotComponent.vue";
import ProfileManagement from "@/views/admin/profile-management/ProfileManagement.vue";
import SellOffManagement from "@/views/admin/sell-off-management/sellOffManagement.vue";
import ContactView from "@/views/user/contact/ContactView.vue";
import CheckOutFail from '@/views/user/order/OrderFail.vue';


const routes = [
    {
        path: '/admin',
        component: AdminLayout,
        meta: { requiresAuth: true, roles: ['Admin', 'Staff'] },
        children: [
            {
                path: '',
                name: 'Statistics',
                component: StatisticalView,
            },
            // Staff
            {
                path: 'users/staff',
                name: 'StaffManagement',
                component: UserManagement,
                meta: { requiresAuth: true, roles: ['Admin'] },
                props: { type: 'staff' },
            },
            {
                path: 'users/staff/create',
                name: 'CreateStaff',
                component: UserCreateForm,
                props: { roleName: 'Staff' },
                meta: { requiresAuth: true, roles: ['Admin'] },
            },
            {
                path: '/admin/users/staff/update',
                name: 'UpdateStaff',
                component: () => import('@/views/admin/user-management/components/UserEditModal.vue')
            },
            {
                path: '/admin/users/customer/update',
                name: 'UpdateCustomer',
                component: () => import('@/views/admin/user-management/components/UserEditModal.vue')
            },
            {
                path: 'return-requests',
                name: 'ReturnRequestManagement',
                component: () => import('@/views/admin/return-request-management/ReturnRequestManagement.vue'),
                meta: { requiresAuth: true, roles: ['Admin', 'Staff'] },
            },
            {
                path: 'return-requests/:id',
                name: 'ReturnRequestDetail',
                component: () => import('@/views/admin/return-request-management/ReturnRequestDetail.vue'),
                props: true,
                meta: { requiresAuth: true, roles: ['Admin', 'Staff'] },
            },

            // Customer
            {
                path: 'users/customer',
                name: 'CustomerManagement',
                component: UserManagement,
                meta: { requiresAuth: true, roles: ['Admin', 'Staff'] },
                props: { type: 'customer' },
            },
            {
                path: 'users/customer/create',
                name: 'CreateCustomer',
                component: UserCreateForm,
                props: { roleName: 'Customer' },
                meta: { requiresAuth: true, roles: ['Admin'] },
            },



            {
                path: 'products',
                name: 'ProductManagement',
                component: ProductManagement,
            },
            {
                path: 'products/new',
                name: 'ProductForm',
                component: ProductForm,
            },
            {
                path: 'products/:id',
                name: 'EditProduct',
                component: ProductForm,
                props: true,
            },
            {
                path: 'categories',
                name: 'CategoryManagement',
                component: CategoryManagement,
            },
            {
                path: 'brands',
                name: 'BrandManagement',
                component: BrandManagement,
            },
            {
                path: 'materials',
                name: 'MaterialManagement',
                component: MaterialManagement,
            },
            {
                path: 'colors',
                name: 'ColorManagement',
                component: ColorManagement,
            },
            {
                path: 'sizes',
                name: 'SizeManagement',
                component: SizeManagement,
            },
            {
                path: 'invoices',
                name: 'InvoiceManagement',
                component: InvoiceManagement,
            },
            {
                path: 'reviews',
                name: 'ReviewManagement',
                component: ReviewManagement,
            },
            {
                path: 'promotions',
                name: 'PromotionManagement',
                component: PromotionManagement,
            },
            {
                path: 'promotions/create',
                name: 'CreatePromotion',
                component: FormPromotion,
                props: { mode: 'create' },
            },
            {
                path: 'promotions/update/:id',
                name: 'EditPromotion',
                component: FormPromotion,
                props: (route) => ({ mode: 'update', id: route.params.id }),
            },
            {
                path: 'promotions/:id',
                name: 'PromotionDetail',
                component: FormPromotion,
                props: (route) => ({ mode: 'detail', id: route.params.id }),
            },
            {
                path: 'coupons',
                name: 'CouponManagement',
                component: CouponManagement,
            },
            {
                path: 'coupons/create',
                name: 'CreateCoupon',
                component: FormCoupon,
                props: { mode: 'create' },
            },
            {
                path: 'coupons/update/:id',
                name: 'UpdateCoupon',
                component: FormCoupon,
                props: (route) => ({ mode: 'update', id: route.params.id }),
            },
            {
                path: 'coupons/:id',
                name: 'CouponDetail',
                component: FormCoupon,
                props: (route) => ({ mode: 'detail', id: route.params.id }),
            },
            {
                path: 'orders',
                name: 'OrderManagement',
                component: OrderManagement,
            },
            {
                path: 'orders/:id',
                name: 'OrderDetailManagement',
                component: OrderDetailManagement,
                props: true,
            },

            {
                path: 'selloff',
                name: 'SellOffManagement',
                component: SellOffManagement

            },
            {
                path: 'profile',
                name: 'ProfileManagement',
                component: ProfileManagement,
                meta: { requiresAuth: true, roles: ['Staff', 'Admin'] },
            }

        ],
    },
    {
        path: '/',
        component: UserLayout,
        children: [
            {
                path: '/',
                name: 'HomeView',
                component: HomeView,
                meta: { requiresAuth: false },
            },
            {
                path: '/carts',
                name: 'CartView',
                component: CartView,
                meta: { requiresAuth: true, roles: ['Customer', 'Staff', 'Admin'] },
            },
            {
                path: '/product-detail/:id',
                name: 'ProductDetailView',
                component: ProductDetailView,
                props: true,
                meta: { requiresAuth: false }
            },
            {
                path: '/products',
                name: 'ProductView',
                component: ProductView,
                meta: { requiresAuth: false },
            },
            {
                path: 'profile',
                name: 'Profile',
                component: ProfileView,
                meta: { requiresAuth: true, roles: ['Customer', 'Staff', 'Admin'] },
            },
            {
                path: '/checkout',
                name: 'CheckoutView',
                component: CheckOut,
                meta: { requiresAuth: false },
            },
            {
                path: '/orders/success',
                name: 'CheckoutSuccess',
                component: OrderSuccess,
            },
            {
                path: '/orders/failed',
                name: 'CheckoutFail',
                component: CheckOutFail,

            },
            {
                path: '/orders',
                name: 'OrderManagementView',
                component: OrderManagementView,
            },
            {
                path: 'order-detail/:id',
                name: 'OrderDetailView',
                component: OrderDetailView,
                props: true,
            },
            {
                path: '/return-requests',
                name: 'ReturnHistory',
                component: () => import('@/views/user/return-rq/ReturnHistory.vue'),
                meta: { requiresAuth: true, roles: ['Customer', 'Staff', 'Admin'] },
            },
            {
                path: '/return-requests/:id',
                name: 'ReturnDetail',
                component: () => import('@/views/user/return-rq/ReturnDetail.vue'),
                props: true,
                meta: { requiresAuth: true, roles: ['Customer', 'Staff', 'Admin'] },
            },
            {
                path: '/chat-bot',
                name: 'ChatBotComponent',
                component: ChatBotComponent,
            },
            {
                path: '/contact',
                name: 'ContactView',
                component: ContactView,
                meta: { requiresGuest: false },
            },
            // router/index.js
            {
                path: '/vnpay-return',
                component: () => import('@/views/user/order/VerifyTransaction.vue')
            }

        ],
    },
    {
        path: '/login',
        name: 'Login',
        component: LoginView,
        meta: { requiresGuest: true }, // Only for unauthenticated users
    },
    {
        path: '/forgot-password',
        name: 'ForgotPasswordView',
        component: ForgotPasswordView,
        meta: { requiresGuest: true },
    },
    {
        path: '/reset-password',
        name: 'ResetPassword',
        component: ResetPasswordView,
        meta: { requiresGuest: true },
    },
    {
        path: '/unauthorized',
        name: 'Unauthorized',
        component: UnauthorizedView,
        meta: { requiresAuth: false }, // Accessible to all
    },
    {
        path: '/register',
        name: 'RegisterView',
        component: RegisterView,
        meta: { requiresGuest: true },
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
});

// Navigation guard
router.beforeEach((to, from, next) => {
    const authStore = useAuthStore();
    authStore.checkAuth();
    const isAuthenticated = authStore.isAuthenticated;
    const userRole = authStore.userRole;

    // Nếu là route dành cho guest (login, register...) mà đã login thì redirect về home
    if (to.meta.requiresGuest && isAuthenticated) {
        return next('/');
    }

    // Kiểm tra tất cả route cha và con có requiresAuth
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
    const allowedRoles = to.matched
        .filter(record => record.meta.roles)
        .flatMap(record => record.meta.roles);

    if (requiresAuth && !isAuthenticated) {
        return next('/login');
    }

    if (allowedRoles.length > 0 && !allowedRoles.includes(userRole)) {
        return next('/unauthorized');
    }

    next();
});

export default router;