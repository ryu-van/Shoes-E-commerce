package com.example.shoozy_shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Builder
@ToString
@Table(name = "product_variants")

public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    @JsonIgnore
    private Product product;

    @ManyToOne
    @JoinColumn(name = "size_id",  referencedColumnName = "id")
    private Size size;

    @ManyToOne
    @JoinColumn(name = "color_id", referencedColumnName = "id")
    private Color color;

    private String thumbnail;

    private Integer quantity;

    @Column(name = "cost_price")
    private Double costPrice;

    @Column(name = "sell_price")
    private Double sellPrice;

    @Column(insertable = false)
    private Boolean status;

    @OneToMany(mappedBy = "productVariant")
    @JsonIgnore
    List<ProductVariantImage> productVariantImages;

    @OneToMany(mappedBy = "productVariant")
    @JsonIgnore
    private List<ProductPromotion> productPromotions;

    public ProductVariant() {
    }

    public ProductVariant(Long id, Product product, Size size, Color color, String thumbnail, Integer quantity, Double costPrice, Double sellPrice, Boolean status, List<ProductVariantImage> productVariantImages, List<ProductPromotion> productPromotions) {
        this.id = id;
        this.product = product;
        this.size = size;
        this.color = color;
        this.thumbnail = thumbnail;
        this.quantity = quantity;
        this.costPrice = costPrice;
        this.sellPrice = sellPrice;
        this.status = status;
        this.productVariantImages = productVariantImages;
        this.productPromotions = productPromotions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(Double costPrice) {
        this.costPrice = costPrice;
    }

    public Double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(Double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public List<ProductVariantImage> getProductVariantImages() {
        return productVariantImages;
    }

    public void setProductVariantImages(List<ProductVariantImage> productVariantImages) {
        this.productVariantImages = productVariantImages;
    }
    
    public List<ProductPromotion> getProductPromotions() {
        return productPromotions;
    }

    public void setProductPromotions(List<ProductPromotion> productPromotions) {
        this.productPromotions = productPromotions;
    }

}
