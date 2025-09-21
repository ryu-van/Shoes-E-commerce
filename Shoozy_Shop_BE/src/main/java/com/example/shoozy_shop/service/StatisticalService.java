package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.response.ChartResponse;
import com.example.shoozy_shop.repository.OrderDetailRepository;
import com.example.shoozy_shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class StatisticalService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    public List<ChartResponse> getRevenueProfitChartByYear(int year, String orderType) {
        Boolean type = getOrderTypeBoolean(orderType);

        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    BigDecimal revenue = orderRepository.sumRevenueByMonthAndType(year, month, type);
                    BigDecimal cost = orderDetailRepository.sumCostByMonthAndType(year, month, type);
                    BigDecimal profit = revenue.subtract(cost);

                    return new ChartResponse(
                            String.valueOf(month), // Hiển thị tháng (1-12)
                            revenue,
                            cost,
                            profit
                    );
                })
                .collect(Collectors.toList());
    }

    private Boolean getOrderTypeBoolean(String orderType) {
        switch (orderType) {
            case "ONLINE":
                return true;
            case "OFFLINE":
                return false;
            default:
                return null; // null sẽ được xử lý trong query để lấy tất cả
        }
    }
}