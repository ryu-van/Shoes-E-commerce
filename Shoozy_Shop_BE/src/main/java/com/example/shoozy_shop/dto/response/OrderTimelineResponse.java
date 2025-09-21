package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.OrderTimeline;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderTimelineResponse {
    private Long id;
    private String orderCode;
    private String userFullName;
    private String type;
    private String description;
    private LocalDateTime createDate;

    public static OrderTimelineResponse fromEntity(OrderTimeline orderTimeline) {
        return new OrderTimelineResponse(
                orderTimeline.getId(),
                orderTimeline.getOrder().getOrderCode(),
                orderTimeline.getUser().getFullname(),
                orderTimeline.getType(),
                orderTimeline.getDescription(),
                orderTimeline.getCreateDate()
        );
    }
}
