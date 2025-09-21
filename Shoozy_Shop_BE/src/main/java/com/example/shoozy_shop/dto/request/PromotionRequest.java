package com.example.shoozy_shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PromotionRequest {

    @NotBlank(message = "Name's promotion is required!")
    @JsonProperty("name")
    private String name;

    private Double value;

    @NotNull(message = "Start date is required!")
    @JsonProperty("start_date")
    private LocalDateTime startDate;

    @NotNull(message = "Expiration date is required!")
    @JsonProperty("expiration_date")
    private LocalDateTime expirationDate;

    @JsonProperty("description")
    private String description;


    @JsonProperty("productPromotionRequests") // camelCase để đồng bộ với FE
    private List<ProductPromotionRequest> productPromotionRequests = new ArrayList<>();


}
