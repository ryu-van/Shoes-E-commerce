package com.example.shoozy_shop.dto.response;

public class OrderStatusStatisticsDto {
    private String status;
    private Long totalOrders;
    private Double percentage;

    public OrderStatusStatisticsDto() {
    }

    public OrderStatusStatisticsDto(String status, Long totalOrders) {
        this.status = status;
        this.totalOrders = totalOrders;
    }

    public OrderStatusStatisticsDto(String status, Long totalOrders, Double percentage) {
        this.status = status;
        this.totalOrders = totalOrders;
        this.percentage = percentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }
}
