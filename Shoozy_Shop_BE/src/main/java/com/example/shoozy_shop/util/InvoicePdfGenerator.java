package com.example.shoozy_shop.util;

import com.example.shoozy_shop.dto.response.InvoiceResponseDTO;
import com.example.shoozy_shop.dto.response.OrderItemResponseDTO;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class InvoicePdfGenerator {

    public static byte[] generateInvoicePdf(InvoiceResponseDTO invoice) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Nạp font DejaVuSans hỗ trợ tiếng Việt từ resources
            String fontResourcePath = "/font/dejavu-fonts-ttf-2.37/ttf/DejaVuSans.ttf";
            InputStream fontStream = InvoicePdfGenerator.class.getResourceAsStream(fontResourcePath);
            if (fontStream == null) {
                throw new RuntimeException("Không tìm thấy file font: " + fontResourcePath);
            }
            PdfFont font = PdfFontFactory.createFont(fontStream.readAllBytes(), "Identity-H", EmbeddingStrategy.PREFER_EMBEDDED);

            // Tiêu đề
            document.add(new Paragraph("HÓA ĐƠN #" + invoice.getOrderCode())
                    .setFont(font).setBold().setFontSize(20));
            document.add(new Paragraph("Shoozy Shop").setFont(font));
//            document.add(new Paragraph("Mã số thuế: " + invoice.getShopTaxCode()).setFont(font));
            document.add(new Paragraph("Địa chỉ: " + invoice.getShopAddress()).setFont(font));
            document.add(new Paragraph("Điện thoại: " + invoice.getShopPhone()).setFont(font));
            document.add(new Paragraph("\n").setFont(font));

            // Thông tin khách hàng
            document.add(new Paragraph("Khách hàng: " + invoice.getCustomerName()).setFont(font));
            document.add(new Paragraph("Điện thoại: " + invoice.getPhoneNumber()).setFont(font));
            document.add(new Paragraph("Địa chỉ giao hàng: " + invoice.getShippingAddress()).setFont(font));
            document.add(new Paragraph("Ngày đặt hàng: " + invoice.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFont(font));
            document.add(new Paragraph("\n").setFont(font));

            // Bảng chi tiết sản phẩm
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1, 2}));
            table.addHeaderCell(new Paragraph("Sản phẩm").setFont(font));
            table.addHeaderCell(new Paragraph("Số lượng").setFont(font));
            table.addHeaderCell(new Paragraph("Đơn giá").setFont(font));
            table.addHeaderCell(new Paragraph("Thành tiền").setFont(font));

            for (OrderItemResponseDTO item : invoice.getItems()) {
                table.addCell(new Paragraph(item.getProductName() + " (" + item.getSize() + ", " + item.getColor() + ")").setFont(font));
                table.addCell(new Paragraph(String.valueOf(item.getQuantity())).setFont(font));
                table.addCell(new Paragraph(String.format("%,.0f VND", item.getPrice())).setFont(font));
                table.addCell(new Paragraph(String.format("%,.0f VND", item.getTotalMoney())).setFont(font));
            }

            document.add(table);
            document.add(new Paragraph("\n").setFont(font));

            // Tổng tiền
            document.add(new Paragraph("Tổng tiền hàng: " + String.format("%,.0f VND", invoice.getTotalItems())).setFont(font));
            if (invoice.getCouponValue() != null) {
                document.add(new Paragraph("Giảm giá (" + invoice.getCouponName() + "): " + String.format("%,.0f VND", invoice.getCouponValue())).setFont(font));
            }
            document.add(new Paragraph("Tổng thanh toán: " + String.format("%,.0f VND", invoice.getTotalMoney())).setFont(font));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo PDF hóa đơn: " + e.getMessage(), e);
        }
    }
}