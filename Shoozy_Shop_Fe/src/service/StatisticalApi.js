import apiClient from "@/auth/api.js";

const BASE_URL = '/statisticals';

export const getDailyOverview = () => apiClient.get(`${BASE_URL}/daily-overview`);

export const getStatisticalByYear = (year) => apiClient.get(`${BASE_URL}/revenue-product/${year}`);

export function getTopSellingProducts(filter) {
    return apiClient.get(`${BASE_URL}/top-selling-products`, {
        params: { filter }
    });
}
export const getLowStockProducts = () => apiClient.get(`${BASE_URL}/low-stock`);

export const getStatusChart = (filter = '7days') => {
    return apiClient.get(`${BASE_URL}/status-chart`, {
        params: { filter }
    });
}

export async function getRevenueProfitChart(year, orderType = 'ALL') {
    return await apiClient.get(`${BASE_URL}/revenue-profit-chart/${year}?orderType=${orderType}`);
}