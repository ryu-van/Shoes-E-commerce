package com.example.shoozy_shop.dto.response;

import java.util.Arrays;
import java.util.List;

public class ProductAIResponse {
    private Long productId;
    private String name;
    private String thumbnail;
    private String description;
    private String brand;
    private String category;
    private String gender;
    private String material;
    private Long minPrice;
    private Long maxPrice;
    private String priceDisplay; // "2,500,000 VND" hoặc "2,000,000 - 3,000,000 VND"
    private Long totalQuantity;
    private List<String> availableSizes;
    private List<String> availableColors;

    // Constructors
    public ProductAIResponse() {
    }

    // Constructor từ database row
    public ProductAIResponse(Long productId, String name, String thumbnail, String description, String brand,
                             String category, String gender, String material,
                             Long minPrice, Long maxPrice, Long totalQuantity,
                             String availableSizes, String availableColors) {
        this.productId = productId;
        this.name = name;
        this.thumbnail = thumbnail;
        this.description = description;
        this.brand = brand;
        this.category = category;
        this.gender = gender;
        this.material = material;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.totalQuantity = totalQuantity;

        // Format giá hiển thị
        if (minPrice.equals(maxPrice)) {
            this.priceDisplay = String.format("%,d VND", minPrice);
        } else {
            this.priceDisplay = String.format("%,d - %,d VND", minPrice, maxPrice);
        }

        // Parse sizes và colors từ string
        this.availableSizes = availableSizes != null ?
                Arrays.asList(availableSizes.split(", ")) : List.of();
        this.availableColors = availableColors != null ?
                Arrays.asList(availableColors.split(", ")) : List.of();
    }

    // Getters & Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public Long getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Long minPrice) {
        this.minPrice = minPrice;
    }

    public Long getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Long maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getPriceDisplay() {
        return priceDisplay;
    }

    public void setPriceDisplay(String priceDisplay) {
        this.priceDisplay = priceDisplay;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public List<String> getAvailableSizes() {
        return availableSizes;
    }

    public void setAvailableSizes(List<String> availableSizes) {
        this.availableSizes = availableSizes;
    }

    public List<String> getAvailableColors() {
        return availableColors;
    }

    public void setAvailableColors(List<String> availableColors) {
        this.availableColors = availableColors;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
}