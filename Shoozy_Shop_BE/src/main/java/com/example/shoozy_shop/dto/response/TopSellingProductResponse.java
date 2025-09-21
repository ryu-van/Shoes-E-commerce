package com.example.shoozy_shop.dto.response;

public class TopSellingProductResponse {

    private Long productId;
    private String thumbnail;
    private String productName;
    private Long totalSold;

    public TopSellingProductResponse() {
    }

    public TopSellingProductResponse(Long productId, String productName, String thumbnail, Long totalSold) {
        this.productId = productId;
        this.thumbnail = thumbnail;
        this.productName = productName;
        this.totalSold = totalSold;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getTotalSold() {
        return totalSold;
    }

    public void setTotalSold(Long totalSold) {
        this.totalSold = totalSold;
    }
}