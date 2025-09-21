package com.example.shoozy_shop.util;

import com.example.shoozy_shop.dto.response.InvoiceResponseDTO;
import com.example.shoozy_shop.dto.response.OrderItemResponseDTO;

import java.time.format.DateTimeFormatter;

public class EmailContentGenerator {

    public static String generateEmailContent(InvoiceResponseDTO invoice) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Hóa đơn #" + invoice.getOrderCode() + "</h2>");
        sb.append("<p>Kính gửi " + invoice.getCustomerName() + ",</p>");
        sb.append("<p>Cảm ơn quý khách đã mua sắm tại Shoozy Shop. Dưới đây là thông tin hóa đơn của quý khách:</p>");
        sb.append("<p><b>Mã hóa đơn:</b> " + invoice.getOrderCode() + "</p>");
        sb.append("<p><b>Ngày đặt hàng:</b> " + invoice.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "</p>");
        sb.append("<p><b>Địa chỉ giao hàng:</b> " + invoice.getShippingAddress() + "</p>");
        sb.append("<p><b>Phương thức thanh toán:</b> " + invoice.getPaymentMethod() + "</p>");
        sb.append("<p><b>Phương thức vận chuyển:</b> " + invoice.getShippingMethod() + "</p>");
        sb.append("<h3>Chi tiết sản phẩm</h3>");
        sb.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        sb.append("<tr><th>Sản phẩm</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr>");
        for (OrderItemResponseDTO item : invoice.getItems()) {
            sb.append("<tr>");
            sb.append("<td>" + item.getProductName() + " (" + item.getSize() + ", " + item.getColor() + ")</td>");
            sb.append("<td>" + item.getQuantity() + "</td>");
            sb.append("<td>" + String.format("%,.0f VND", item.getPrice()) + "</td>");
            sb.append("<td>" + String.format("%,.0f VND", item.getTotalMoney()) + "</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");

        sb.append("<p><b>Tổng tiền hàng:</b> " + String.format("%,.0f VND", invoice.getTotalItems()) + "</p>");
        if (invoice.getCouponValue() != null) {
            sb.append("<p><b>Giảm giá (" + invoice.getCouponName() + "):</b> " + String.format("%,.0f VND", invoice.getCouponValue()) + "</p>");
        }
        sb.append("<p><b>Tổng thanh toán:</b> " + String.format("%,.0f VND", invoice.getTotalMoney()) + "</p>");
        sb.append("<p>Vui lòng kiểm tra file PDF đính kèm để xem chi tiết hóa đơn.</p>");
        sb.append("<p>Cảm ơn quý khách và hẹn gặp lại!</p>");
        sb.append("<p>Shoozy Shop<br>Email: shopshoozy@gmail.com");

        return sb.toString();
    }
}