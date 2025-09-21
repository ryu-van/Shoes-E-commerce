package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.OrderTimelineRequest;
import com.example.shoozy_shop.dto.response.OrderTimelineResponse;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.OrderTimeline;
import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.repository.OrderRepository;
import com.example.shoozy_shop.repository.OrderTimelineRepository;
import com.example.shoozy_shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class OrderTimelineService implements IOrderTimeline {

    private final OrderTimelineRepository orderTimelineRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public List<OrderTimelineResponse> getOrderTimelinesByOrderId(Long orderId) {
        return orderTimelineRepository.findByOrderIdOrderByCreateDateDesc(orderId)
                .stream()
                .map(OrderTimelineResponse::fromEntity).toList();
    }

    @Override
    public OrderTimeline createOrderTimeline(OrderTimelineRequest orderTimelineRequest) {
        Order existingOrder = orderRepository.findById(orderTimelineRequest.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("order", orderTimelineRequest.getOrderId()));
        User existingUser = userRepository.findById(orderTimelineRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("user", orderTimelineRequest.getUserId()));
        OrderTimeline orderTimeline = OrderTimeline.builder()
                .order(existingOrder)
                .user(existingUser)
                .type("ORDER_UPDATE")
                .description(orderTimelineRequest.getDescription())
                .createDate(LocalDateTime.now())
                .build();
        return orderTimelineRepository.save(orderTimeline);
    }
}
