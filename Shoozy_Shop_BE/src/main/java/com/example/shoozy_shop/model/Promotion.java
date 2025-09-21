package com.example.shoozy_shop.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "promotions")
@Builder
public class Promotion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    private Double value;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private Integer status;


}
