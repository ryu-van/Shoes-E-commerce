package com.example.shoozy_shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @JsonProperty("name")
    @NotBlank(message = "Product name must not be blank")
    private String name;

//    @JsonProperty("brand_id")
    @NotNull(message = "Brand ID must not be null")
    private Long brandId;

//    @JsonProperty("category_id")
    @NotNull(message = "Category ID must not be null")
    private Long categoryId;

    @NotBlank(message = "Gender must not be blank")
    private String gender;

    @NotNull(message = "Weight must not be null")
    private Double weight;

    @NotNull(message = "Material ID must not be null")
    private Long materialId;

    @JsonProperty("description")
    @NotBlank(message = "Description must not be blank")
    private String description;

    private Boolean status;
}
