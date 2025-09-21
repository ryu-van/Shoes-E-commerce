package com.example.shoozy_shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.MappedSuperclass;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Data
@MappedSuperclass

public class BaseResponse {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}