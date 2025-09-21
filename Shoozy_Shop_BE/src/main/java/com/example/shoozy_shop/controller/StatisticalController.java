package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.response.*;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.repository.OrderRepository;
import com.example.shoozy_shop.service.OrderService;
import com.example.shoozy_shop.service.ProductService;
import com.example.shoozy_shop.service.StatisticalService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/statisticals")
@RequiredArgsConstructor
public class StatisticalController {

    private final StatisticalService statisticalService;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final OrderService orderService;

    // API mới cho thống kê tổng quan hôm nay
    @GetMapping("/daily-overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyOverview() {
        LocalDate today = LocalDate.now();

        // Lấy thống kê tại quầy (type = false)
        DailySummary offlineOrders = orderRepository.countOfflineOrdersByDate(today);

        // Lấy thống kê online (type = true)
        DailySummary onlineOrders = orderRepository.countOnlineOrdersByDate(today);

        // Lấy tổng số đơn hàng và doanh thu
        DailySummary totalStats = orderRepository.countTotalOrdersByDate(today);

        // Tạo response theo format như hình
        Map<String, Object> response = new HashMap<>();

        // Tại quầy
        Map<String, Object> offlineData = new HashMap<>();
        offlineData.put("revenue", offlineOrders.getRevenue());
        offlineData.put("count", offlineOrders.getCount());
        response.put("taiQuay", offlineData);

        // Bán online
        Map<String, Object> onlineData = new HashMap<>();
        onlineData.put("revenue", onlineOrders.getRevenue());
        onlineData.put("count", onlineOrders.getCount());
        response.put("banOnline", onlineData);

        // Tổng đơn hàng
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("count", totalStats.getCount());
        response.put("donHang", orderData);

        // Doanh thu tổng
        Map<String, Object> revenueData = new HashMap<>();
        revenueData.put("amount", totalStats.getRevenue());
        revenueData.put("count", totalStats.getCount());
        response.put("doanhThuTong", revenueData);

        return ResponseEntity.ok(ApiResponse.success("Thống kê tổng quan hôm nay", response));
    }

    @GetMapping("/revenue-profit-chart/{year}")
    public ResponseEntity<ApiResponse<List<ChartResponse>>> getRevenueProfitChart(
            @PathVariable int year,
            @RequestParam(value = "orderType", defaultValue = "ALL") String orderType) {

        try {
            // Validate orderType
            if (!orderType.equals("ALL") && !orderType.equals("ONLINE") && !orderType.equals("OFFLINE")) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "orderType phải là ALL, ONLINE hoặc OFFLINE"));
            }

            List<ChartResponse> data = statisticalService.getRevenueProfitChartByYear(year, orderType);

            String message = String.format("Thống kê doanh thu - lợi nhuận năm %d - Loại: %s", year, orderType);

            return ResponseEntity.ok(ApiResponse.success(message, data));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/top-selling-products")
    public ResponseEntity<ApiResponse<List<TopSellingProductResponse>>> getTopSellingProducts(@RequestParam("filter") String filterType) {
        List<TopSellingProductResponse> result = productService.getTopSellingProducts(filterType);
        return ResponseEntity.ok(ApiResponse.success("Top sản phẩm bán chạy", result));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<LowStockProductResponse>>> getLowStockProducts() {
        List<LowStockProductResponse> products = productService.findLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success("Danh sách sản phẩm sắp hết hàng", products));
    }

    @GetMapping("/status-chart")
    public ResponseEntity<ApiResponse<List<OrderStatusStatisticsDto>>> getOrderStatusStatistics(
            @RequestParam(value = "filter", defaultValue = "7days") String filterType) {
        try {
            List<OrderStatusStatisticsDto> statistics = orderService.getOrderStatusStatistics(filterType);
            return ResponseEntity.ok(ApiResponse.success("Thống kê đơn hàng theo trạng thái", statistics));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "Lỗi hệ thống"));
        }
    }
}