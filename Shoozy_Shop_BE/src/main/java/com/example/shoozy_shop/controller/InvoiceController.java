package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.response.InvoiceResponseDTO;
import com.example.shoozy_shop.service.InvoiceService;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.shoozy_shop.util.InvoiceExcelExporter;

@RestController
@RequestMapping("${api.prefix}/invoice")
@CrossOrigin(origins = "http://localhost:5173")
@Validated
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    // Lấy toàn bộ hóa đơn
    @GetMapping
    public ResponseEntity<List<InvoiceResponseDTO>> getAllOrders() {
        List<InvoiceResponseDTO> invoices = invoiceService.getAllOrders();
        return ResponseEntity.ok(invoices);
    }

    // Lấy hóa đơn theo ID
    @GetMapping("/{orderId}")
    public ResponseEntity<InvoiceResponseDTO> getInvoice(@PathVariable Long orderId) {
        try {
            InvoiceResponseDTO invoice = invoiceService.getInvoiceById(orderId);
            return ResponseEntity.ok(invoice);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    // Gửi hóa đơn qua email
    @PostMapping("/{orderId}/send-email")
    public ResponseEntity<String> sendInvoiceEmail(
            @PathVariable Long orderId,
            @RequestParam @Email(message = "Email không hợp lệ") String recipientEmail) {
        try {
            invoiceService.sendInvoiceEmail(orderId, recipientEmail);
            return ResponseEntity.ok("Hóa đơn đã được gửi thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body("Lỗi khi gửi hóa đơn: " + e.getMessage());
        }
    }

    // Xuất danh sách hóa đơn ra file Excel
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportInvoicesToExcel() {
        List<InvoiceResponseDTO> invoices = invoiceService.getAllOrders();
        byte[] excelData = InvoiceExcelExporter.exportInvoicesToExcel(invoices);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoices.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }
}