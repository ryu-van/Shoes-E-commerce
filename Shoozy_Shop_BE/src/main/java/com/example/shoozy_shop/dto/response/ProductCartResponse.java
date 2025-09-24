package com.example.shoozy_shop.dto.response;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
public class ProductCartResponse {
  private Long idCartItem;
  private Long idProductVariant;
  private String productName;
  private String productImage;
  private Double price;
  private Double discountPercent;    // % giảm
  private Integer size;
  private String color;
  private Integer quantity;
  private Integer availableQuantity;

  public ProductCartResponse(Long idCartItem,Long idProductVariant, String productName, String productImage,
                             Double price, Double discountPercent, Integer size, String color,
                             Integer quantity, Integer availableQuantity) {
    this.idCartItem = idCartItem;
    this.idProductVariant = idProductVariant;
    this.productName = productName;
    this.productImage = productImage;
    this.price = price;
    this.discountPercent = discountPercent;
    this.size = size;
    this.color = color;
    this.quantity = quantity;
    this.availableQuantity = availableQuantity;
  }
}




