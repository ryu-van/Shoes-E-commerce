package com.example.shoozy_shop.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupons")

public class Coupon extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    private String description;

    private Boolean type;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    private Double value;

    private Double condition;

    private Integer quantity;

    private Integer status;

    @Column(name = "value_limit")
    private Double valueLimit;
}
