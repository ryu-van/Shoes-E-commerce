package com.example.shoozy_shop.dto.response;

import java.time.LocalDateTime;

public class PromotionAIResponse {
    private String name;
    private String code;
    private Double value;
    private String valueDisplay; // "Giảm 20%" hoặc "Giảm 500,000 VND"
    private String productName;
    private LocalDateTime startDate;
    private LocalDateTime expirationDate;
    private String status; // "Còn hiệu lực", "Sắp hết hạn"

    // Constructors
    public PromotionAIResponse() {}

    public PromotionAIResponse(String name, String code, Double value, String productName,
                        LocalDateTime startDate, LocalDateTime expirationDate) {
        this.name = name;
        this.code = code;
        this.value = value;
        this.productName = productName;
        this.startDate = startDate;
        this.expirationDate = expirationDate;

        // Format hiển thị giá trị khuyến mãi
        if (value != null) {
            if (value < 1) {
                this.valueDisplay = String.format("Giảm %.0f%%", value * 100);
            } else {
                this.valueDisplay = String.format("Giảm %,.0f VND", value);
            }
        }

        // Tính status
        LocalDateTime now = LocalDateTime.now();
        if (expirationDate.isBefore(now)) {
            this.status = "Hết hạn";
        } else if (expirationDate.isBefore(now.plusDays(3))) {
            this.status = "Sắp hết hạn";
        } else {
            this.status = "Còn hiệu lực";
        }
    }

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getValueDisplay() { return valueDisplay; }
    public void setValueDisplay(String valueDisplay) { this.valueDisplay = valueDisplay; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
