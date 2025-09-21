import apiClient from "@/auth/api";
const BASE_URL = '/payment-methods';
export default {
    getAllPaymentMethods() {
        return apiClient.get(`${BASE_URL}/all`);
    }
}
