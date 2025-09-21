package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.OrderRequest;
import com.example.shoozy_shop.dto.request.StatusOrderRequest;
import com.example.shoozy_shop.dto.request.UpdateUserOrderInfoRequest;
import com.example.shoozy_shop.dto.response.OrderResponse;
import com.example.shoozy_shop.dto.response.OrderStatusStatisticsDto;
import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.OrderDetail;

import java.util.List;

public interface IOrderService {
    List<Order> getAllOrders();

    Order addOrder(OrderRequest orderRequest);

    Order updateOrder(Long id, OrderRequest orderRequest);

    OrderResponse getOrderById(Long id);

    void deleteOrder(Long id);

    Order updateStatus(Long id, StatusOrderRequest statusOrderRequest);

    Order updateInfo(Long id, UpdateUserOrderInfoRequest updateUserOrderInfoRequest);

    List<OrderStatusStatisticsDto> getOrderStatusStatistics(String filterType);

    List<OrderResponse> getAllOrderByUserId(Long userId);

    List<OrderDetail> getReturnableItems(Long orderId, Long userId);
}