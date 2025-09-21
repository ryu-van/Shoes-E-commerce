package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.OrderTimelineRequest;
import com.example.shoozy_shop.dto.response.OrderTimelineResponse;
import com.example.shoozy_shop.model.OrderTimeline;

import java.util.List;

public interface IOrderTimeline {
    List<OrderTimelineResponse> getOrderTimelinesByOrderId(Long orderId);
    OrderTimeline createOrderTimeline(OrderTimelineRequest orderTimelineRequest);
}
