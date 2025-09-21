package com.example.shoozy_shop.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.example.shoozy_shop.dto.response.InvoiceResponseDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class InvoiceExcelExporter {
    // DTO đơn giản chỉ chứa các trường primitive/String
    public static class InvoiceExcelSimpleDTO {
        @ExcelProperty("Mã đơn hàng")
        private String orderCode;
        @ExcelProperty("Khách hàng")
        private String customerName;
        @ExcelProperty("SĐT")
        private String phoneNumber;
        @ExcelProperty("Email")
        private String email;
        @ExcelProperty("Địa chỉ giao hàng")
        private String shippingAddress;
        @ExcelProperty("Trạng thái")
        private String status;
        @ExcelProperty("Ngày đặt")
        private String orderDate;
        @ExcelProperty("Tổng thanh toán")
        private BigDecimal totalMoney;
        // ... thêm các trường khác nếu muốn

        // Getters & Setters
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        public BigDecimal getTotalMoney() { return totalMoney; }
        public void setTotalMoney(BigDecimal totalMoney) { this.totalMoney = totalMoney; }
    }

    public static byte[] exportInvoicesToExcel(List<InvoiceResponseDTO> invoices) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            List<InvoiceExcelSimpleDTO> excelList = invoices.stream().map(invoice -> {
                InvoiceExcelSimpleDTO dto = new InvoiceExcelSimpleDTO();
                dto.setOrderCode(invoice.getOrderCode());
                dto.setCustomerName(invoice.getCustomerName());
                dto.setPhoneNumber(invoice.getPhoneNumber());
                dto.setEmail(invoice.getEmail());
                dto.setShippingAddress(invoice.getShippingAddress());
                dto.setStatus(invoice.getStatus());
                dto.setOrderDate(invoice.getOrderDate() != null ? invoice.getOrderDate().format(dtf) : "");
                dto.setTotalMoney(invoice.getTotalMoney());
                return dto;
            }).collect(Collectors.toList());
            EasyExcel.write(out, InvoiceExcelSimpleDTO.class)
                    .sheet("Invoices")
                    .doWrite(excelList);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel: " + e.getMessage(), e);
        }
    }
} 