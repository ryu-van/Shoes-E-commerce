package com.example.shoozy_shop.service;

import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.OrderDetail;
import com.example.shoozy_shop.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {

    private final JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.sender-name}")
    private String senderName;

    /**
     * Helper method để tính tổng tiền an toàn
     */
    private BigDecimal calculateTotalAmount(Order order) {
        // Chỉ sử dụng totalMoney vì các trường khác đã bị xóa khỏi model
        return order.getTotalMoney() != null ? order.getTotalMoney() : BigDecimal.ZERO;
    }

    /**
     * Gửi email thông báo đơn hàng được đặt thành công
     */
    public void sendOrderSuccessEmail(Order order, User user) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(user.getEmail());
            helper.setSubject("Đặt hàng thành công - Đơn hàng #" + order.getId());

            String htmlContent = createOrderSuccessTemplate(order, user);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("Order success email sent successfully for order ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order success email for order ID: {}", order.getId(), e);
            throw new RuntimeException("Không thể gửi email thông báo đặt hàng thành công");
        }
    }

    /**
     * Gửi email thông báo đơn hàng bị hủy bởi người dùng
     */
    public void sendOrderCancelledByUserEmail(Order order, User user) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(user.getEmail());
            helper.setSubject("Đơn hàng đã được hủy - Đơn hàng #" + order.getId());

            String htmlContent = createOrderCancelledByUserTemplate(order, user);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("Order cancelled by user email sent successfully for order ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order cancelled by user email for order ID: {}", order.getId(), e);
            throw new RuntimeException("Không thể gửi email thông báo hủy đơn hàng");
        }
    }

    /**
     * Gửi email thông báo đơn hàng hoàn thành
     */
    public void sendOrderCompletedEmail(Order order, User user) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(user.getEmail());
            helper.setSubject("Đơn hàng đã hoàn thành - Đơn hàng #" + order.getId());

            String htmlContent = createOrderCompletedTemplate(order, user);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("Order completed email sent successfully for order ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order completed email for order ID: {}", order.getId(), e);
            throw new RuntimeException("Không thể gửi email thông báo hoàn thành đơn hàng");
        }
    }

    /**
     * Gửi email thông báo đơn hàng bị hủy bởi shop
     */
    public void sendOrderCancelledByShopEmail(Order order, User user, String reason) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(user.getEmail());
            helper.setSubject("Đơn hàng bị hủy bởi shop - Đơn hàng #" + order.getId());

            String htmlContent = createOrderCancelledByShopTemplate(order, user, reason);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("Order cancelled by shop email sent successfully for order ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order cancelled by shop email for order ID: {}", order.getId(), e);
            throw new RuntimeException("Không thể gửi email thông báo shop hủy đơn hàng");
        }
    }

    /**
     * Tạo template HTML cho email đặt hàng thành công
     */
    private String createOrderSuccessTemplate(Order order, User user) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderDetail item : order.getOrderDetails()) {
            itemsHtml.append("<tr>")
                    .append("<td style='padding: 10px; border-bottom: 1px solid #eee;'>")
                    .append("<img src='").append(item.getProductVariant().getThumbnail() != null ? item.getProductVariant().getThumbnail() : "").append("' ")
                    .append("style='width: 60px; height: 60px; object-fit: cover; border-radius: 8px;' alt='Product'>")
                    .append("</td>")
                    .append("<td style='padding: 10px; border-bottom: 1px solid #eee;'>")
                    .append("<div style='font-weight: 600; color: #333;'>").append(item.getProductVariant().getProduct().getName()).append("</div>")

                    .append("<div style='font-size: 14px; color: #666;'>Màu: ").append(item.getProductVariant().getColor().getName()).append("</div>")
                    .append("</td>")
                    .append("<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: center;'>").append(item.getQuantity()).append("</td>")
                    .append("<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right; font-weight: 600; color: #e74c3c;'>")
                    .append(String.format("%,.0f VNĐ", item.getPrice() * item.getQuantity()))
                    .append("</td>")
                    .append("</tr>");
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Đặt hàng thành công</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\">" +
                "<!-- Header -->" +
                "<div style=\"background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;\">" +
                "<h1 style=\"color: white; margin: 0; font-size: 28px;\">🎉 Đặt hàng thành công!</h1>" +
                "<p style=\"color: white; margin: 10px 0 0 0; font-size: 16px;\">Cảm ơn bạn đã tin tưởng Shoozy Shop</p>" +
                "</div>" +
                "<!-- Content -->" +
                "<div style=\"padding: 30px;\">" +
                "<div style=\"background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px;\">" +
                "<h2 style=\"color: #333; margin: 0 0 15px 0; font-size: 20px;\">Xin chào " + user.getFullname() + "!</h2>" +
                "<p style=\"color: #666; margin: 0; line-height: 1.6;\">" +
                "Đơn hàng của bạn đã được đặt thành công. Chúng tôi sẽ xử lý và giao hàng trong thời gian sớm nhất." +
                "</p>" +
                "</div>" +
                "<!-- Order Info -->" +
                "<div style=\"background-color: #fff; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #333; margin: 0 0 15px 0; font-size: 18px;\">📋 Thông tin đơn hàng</h3>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Mã đơn hàng:</span>" +
                "<span style=\"font-weight: 600; color: #333;\">#" + order.getId() + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Ngày đặt:</span>" +
                "<span style=\"color: #333;\">" + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Trạng thái:</span>" +
                "<span style=\"color: #28a745; font-weight: 600;\">Đã đặt hàng</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Phương thức thanh toán:</span>" +
                "<span style=\"color: #333;\">" + order.getPaymentMethod().getName() + "</span>" +
                "</div>" +
                "</div>" +
                "<!-- Order Items -->" +
                "<div style=\"background-color: #fff; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #333; margin: 0 0 15px 0; font-size: 18px;\">🛍️ Chi tiết sản phẩm</h3>" +
                "<table style=\"width: 100%; border-collapse: collapse;\">" +
                "<thead>" +
                "<tr style=\"background-color: #f8f9fa;\">" +
                "<th style=\"padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6;\">Hình ảnh</th>" +
                "<th style=\"padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6;\">Sản phẩm</th>" +
                "<th style=\"padding: 10px; text-align: center; border-bottom: 2px solid #dee2e6;\">Số lượng</th>" +
                "<th style=\"padding: 10px; text-align: right; border-bottom: 2px solid #dee2e6;\">Thành tiền</th>" +
                "</tr>" +
                "</thead>" +
                "<tbody>" +
                itemsHtml.toString() +
                "</tbody>" +
                "</table>" +
                "</div>" +
                "<!-- Total -->" +
                "<div style=\"background-color: #fff; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Tạm tính:</span>" +
                "<span style=\"color: #333;\">" + String.format("%,.0f VNĐ", order.getTotalMoney() != null ? order.getTotalMoney() : java.math.BigDecimal.ZERO) + "</span>" +
                "</div>" +
                // Các trường shippingFee và couponDiscountAmount đã bị xóa khỏi model Order +
                "<hr style=\"border: none; border-top: 2px solid #e9ecef; margin: 15px 0;\">" +
                "<div style=\"display: flex; justify-content: space-between;\">" +
                "<span style=\"font-size: 18px; font-weight: 600; color: #333;\">Tổng cộng:</span>" +
                "<span style=\"font-size: 18px; font-weight: 600; color: #e74c3c;\">" + String.format("%,.0f VNĐ", calculateTotalAmount(order)) + "</span>" +
                "</div>" +
                "</div>" +
                "<!-- Next Steps -->" +
                "<div style=\"background-color: #e8f5e8; border-left: 4px solid #28a745; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #155724; margin: 0 0 10px 0; font-size: 16px;\">📝 Bước tiếp theo</h3>" +
                "<ul style=\"color: #155724; margin: 0; padding-left: 20px;\">" +
                "<li>Chúng tôi sẽ xác nhận đơn hàng trong vòng 24 giờ</li>" +
                "<li>Đơn hàng sẽ được đóng gói và giao trong 2-3 ngày làm việc</li>" +
                "<li>Bạn sẽ nhận được thông báo khi đơn hàng được giao</li>" +
                "</ul>" +
                "</div>" +
                "</div>" +
                "<!-- Footer -->" +
                "<div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "<p style=\"color: #666; margin: 0 0 10px 0;\">Cảm ơn bạn đã mua sắm tại Shoozy Shop!</p>" +
                "<p style=\"color: #666; margin: 0; font-size: 14px;\">" +
                "Nếu có thắc mắc, vui lòng liên hệ: <a href=\"mailto:shopshoozy@gmail.com\" style=\"color: #667eea;\">shopshoozy@gmail.com</a>" +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Tạo template HTML cho email đơn hàng bị hủy bởi người dùng
     */
    private String createOrderCancelledByUserTemplate(Order order, User user) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Đơn hàng đã được hủy</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\">" +
                "<!-- Header -->" +
                "<div style=\"background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%); padding: 30px; text-align: center;\">" +
                "<h1 style=\"color: white; margin: 0; font-size: 28px;\">❌ Đơn hàng đã được hủy</h1>" +
                "<p style=\"color: white; margin: 10px 0 0 0; font-size: 16px;\">Thông báo hủy đơn hàng</p>" +
                "</div>" +
                "<!-- Content -->" +
                "<div style=\"padding: 30px;\">" +
                "<div style=\"background-color: #fff5f5; padding: 20px; border-radius: 8px; margin-bottom: 20px; border-left: 4px solid #ff6b6b;\">" +
                "<h2 style=\"color: #333; margin: 0 0 15px 0; font-size: 20px;\">Xin chào " + user.getFullname() + "!</h2>" +
                "<p style=\"color: #666; margin: 0; line-height: 1.6;\">" +
                "Chúng tôi xác nhận rằng đơn hàng của bạn đã được hủy thành công theo yêu cầu của bạn." +
                "</p>" +
                "</div>" +
                "<!-- Order Info -->" +
                "<div style=\"background-color: #fff; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #333; margin: 0 0 15px 0; font-size: 18px;\">📋 Thông tin đơn hàng đã hủy</h3>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Mã đơn hàng:</span>" +
                "<span style=\"font-weight: 600; color: #333;\">#" + order.getId() + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Ngày đặt:</span>" +
                "<span style=\"color: #333;\">" + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Ngày hủy:</span>" +
                "<span style=\"color: #333;\">" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Trạng thái:</span>" +
                "<span style=\"color: #ff6b6b; font-weight: 600;\">Đã hủy bởi khách hàng</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Tổng tiền:</span>" +
                "<span style=\"color: #333; font-weight: 600;\">" + String.format("%,.0f VNĐ", calculateTotalAmount(order)) + "</span>" +
                "</div>" +
                "</div>" +
                "<!-- Refund Info -->" +
                "<div style=\"background-color: #e8f5e8; border-left: 4px solid #28a745; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #155724; margin: 0 0 10px 0; font-size: 16px;\">💰 Thông tin hoàn tiền</h3>" +
                "<ul style=\"color: #155724; margin: 0; padding-left: 20px;\">" +
                "<li>Nếu bạn đã thanh toán, tiền sẽ được hoàn lại trong vòng 3-5 ngày làm việc</li>" +
                "<li>Tiền sẽ được hoàn về phương thức thanh toán ban đầu</li>" +
                "<li>Bạn sẽ nhận được thông báo khi hoàn tiền thành công</li>" +
                "</ul>" +
                "</div>" +
                "<!-- Next Steps -->" +
                "<div style=\"background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #333; margin: 0 0 10px 0; font-size: 16px;\">🛍️ Tiếp tục mua sắm</h3>" +
                "<p style=\"color: #666; margin: 0 0 15px 0;\">Chúng tôi hy vọng bạn sẽ tiếp tục mua sắm tại Shoozy Shop!</p>" +
                "<a href=\"http://localhost:5173\" style=\"display: inline-block; background-color: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600;\">" +
                "Xem sản phẩm mới" +
                "</a>" +
                "</div>" +
                "</div>" +
                "<!-- Footer -->" +
                "<div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "<p style=\"color: #666; margin: 0 0 10px 0;\">Cảm ơn bạn đã tin tưởng Shoozy Shop!</p>" +
                "<p style=\"color: #666; margin: 0; font-size: 14px;\">" +
                "Nếu có thắc mắc, vui lòng liên hệ: <a href=\"mailto:shopshoozy@gmail.com\" style=\"color: #667eea;\">shopshoozy@gmail.com</a>" +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Tạo template HTML cho email đơn hàng bị hủy bởi shop
     */
    private String createOrderCancelledByShopTemplate(Order order, User user, String reason) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Đơn hàng bị hủy bởi shop</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\">" +
                "<!-- Header -->" +
                "<div style=\"background: linear-gradient(135deg, #ffa726 0%, #ff7043 100%); padding: 30px; text-align: center;\">" +
                "<h1 style=\"color: white; margin: 0; font-size: 28px;\">⚠️ Đơn hàng bị hủy</h1>" +
                "<p style=\"color: white; margin: 10px 0 0 0; font-size: 16px;\">Thông báo từ Shoozy Shop</p>" +
                "</div>" +
                "<!-- Content -->" +
                "<div style=\"padding: 30px;\">" +
                "<div style=\"background-color: #fff3e0; padding: 20px; border-radius: 8px; margin-bottom: 20px; border-left: 4px solid #ffa726;\">" +
                "<h2 style=\"color: #333; margin: 0 0 15px 0; font-size: 20px;\">Xin chào " + user.getFullname() + "!</h2>" +
                "<p style=\"color: #666; margin: 0; line-height: 1.6;\">" +
                "Chúng tôi rất tiếc phải thông báo rằng đơn hàng của bạn đã bị hủy bởi shop do một số lý do khách quan." +
                "</p>" +
                "</div>" +
                "<!-- Order Info -->" +
                "<div style=\"background-color: #fff; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #333; margin: 0 0 15px 0; font-size: 18px;\">📋 Thông tin đơn hàng</h3>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Mã đơn hàng:</span>" +
                "<span style=\"font-weight: 600; color: #333;\">#" + order.getId() + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Ngày đặt:</span>" +
                "<span style=\"color: #333;\">" + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Ngày hủy:</span>" +
                "<span style=\"color: #333;\">" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Trạng thái:</span>" +
                "<span style=\"color: #ffa726; font-weight: 600;\">Đã hủy bởi shop</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Tổng tiền:</span>" +
                "<span style=\"color: #333; font-weight: 600;\">" + String.format("%,.0f VNĐ", calculateTotalAmount(order)) + "</span>" +
                "</div>" +
                "</div>" +
                "<!-- Reason -->" +
                "<div style=\"background-color: #fff; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #333; margin: 0 0 15px 0; font-size: 18px;\">📝 Lý do hủy đơn hàng</h3>" +
                "<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 6px; border-left: 4px solid #ffa726;\">" +
                "<p style=\"color: #666; margin: 0; line-height: 1.6;\">" + (reason != null ? reason : "Sản phẩm tạm thời hết hàng hoặc có vấn đề kỹ thuật") + "</p>" +
                "</div>" +
                "</div>" +
                "<!-- Apology -->" +
                "<div style=\"background-color: #e3f2fd; border-left: 4px solid #2196f3; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #1565c0; margin: 0 0 10px 0; font-size: 16px;\">🙏 Lời xin lỗi</h3>" +
                "<p style=\"color: #1565c0; margin: 0; line-height: 1.6;\">" +
                "Chúng tôi chân thành xin lỗi vì sự bất tiện này. Chúng tôi cam kết sẽ cải thiện dịch vụ để tránh tình trạng tương tự trong tương lai." +
                "</p>" +
                "</div>" +
                "<!-- Refund Info -->" +
                "<div style=\"background-color: #e8f5e8; border-left: 4px solid #28a745; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #155724; margin: 0 0 10px 0; font-size: 16px;\">💰 Thông tin hoàn tiền</h3>" +
                "<ul style=\"color: #155724; margin: 0; padding-left: 20px;\">" +
                "<li>Tiền sẽ được hoàn lại 100% trong vòng 1-2 ngày làm việc</li>" +
                "<li>Tiền sẽ được hoàn về phương thức thanh toán ban đầu</li>" +
                "<li>Bạn sẽ nhận được thông báo khi hoàn tiền thành công</li>" +

                "</ul>" +
                "</div>" +
                "<!-- Compensation -->" +
                "<div style=\"background-color: #f3e5f5; border: 1px solid #e1bee7; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #7b1fa2; margin: 0 0 10px 0; font-size: 16px;\">🎁 Đền bù</h3>" +
                "<p style=\"color: #7b1fa2; margin: 0 0 15px 0;\">Để bù đắp sự bất tiện, chúng tôi tặng bạn:</p>" +
                "<ul style=\"color: #7b1fa2; margin: 0; padding-left: 20px;\">" +
                "<li>Mã giảm giá 10% cho đơn hàng tiếp theo</li>" +
//                "<li>Miễn phí vận chuyển cho đơn hàng từ 300k</li>" +
                "</ul>" +
                "<div style=\"margin-top: 15px;\">" +
                "<a href=\"http://localhost:5173\" style=\"display: inline-block; background-color: #7b1fa2; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600;\">" +
                "Sử dụng ưu đãi ngay" +
                "</a>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<!-- Footer -->" +
                "<div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "<p style=\"color: #666; margin: 0 0 10px 0;\">Cảm ơn bạn đã tin tưởng Shoozy Shop!</p>" +
                "<p style=\"color: #666; margin: 0; font-size: 14px;\">" +
                "Nếu có thắc mắc, vui lòng liên hệ: <a href=\"mailto:shopshoozy@gmail.com\" style=\"color: #667eea;\">shopshoozy@gmail.com</a>" +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Tạo template HTML cho email đơn hàng hoàn thành
     */
    private String createOrderCompletedTemplate(Order order, User user) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Đơn hàng đã hoàn thành</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\">" +
                "<!-- Header -->" +
                "<div style=\"background: linear-gradient(135deg, #28a745 0%, #20c997 100%); padding: 30px; text-align: center;\">" +
                "<h1 style=\"color: white; margin: 0; font-size: 28px;\">🎉 Đơn hàng đã hoàn thành!</h1>" +
                "<p style=\"color: white; margin: 10px 0 0 0; font-size: 16px;\">Cảm ơn bạn đã mua sắm tại Shoozy Shop</p>" +
                "</div>" +
                "<!-- Content -->" +
                "<div style=\"padding: 30px;\">" +
                "<div style=\"background-color: #d4edda; padding: 20px; border-radius: 8px; margin-bottom: 20px; border-left: 4px solid #28a745;\">" +
                "<h2 style=\"color: #155724; margin: 0 0 15px 0; font-size: 20px;\">Xin chào " + user.getFullname() + "!</h2>" +
                "<p style=\"color: #155724; margin: 0; line-height: 1.6;\">" +
                "Chúc mừng! Đơn hàng của bạn đã được giao thành công. Chúng tôi hy vọng bạn hài lòng với sản phẩm." +
                "</p>" +
                "</div>" +
                "<!-- Order Info -->" +
                "<div style=\"background-color: #fff; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #333; margin: 0 0 15px 0; font-size: 18px;\">📋 Thông tin đơn hàng</h3>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Mã đơn hàng:</span>" +
                "<span style=\"font-weight: 600; color: #333;\">#" + order.getId() + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Ngày đặt:</span>" +
                "<span style=\"color: #333;\">" + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Ngày hoàn thành:</span>" +
                "<span style=\"color: #333;\">" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Trạng thái:</span>" +
                "<span style=\"color: #28a745; font-weight: 600;\">Đã hoàn thành</span>" +
                "</div>" +
                "<div style=\"display: flex; justify-content: space-between; margin-bottom: 10px;\">" +
                "<span style=\"color: #666;\">Tổng tiền:</span>" +
                "<span style=\"color: #333; font-weight: 600;\">" + String.format("%,.0f VNĐ", calculateTotalAmount(order)) + "</span>" +
                "</div>" +
                "</div>" +
                "<!-- Thank You -->" +
                "<div style=\"background-color: #e8f5e8; border-left: 4px solid #28a745; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #155724; margin: 0 0 10px 0; font-size: 16px;\">🙏 Lời cảm ơn</h3>" +
                "<p style=\"color: #155724; margin: 0; line-height: 1.6;\">" +
                "Cảm ơn bạn đã tin tưởng và mua sắm tại Shoozy Shop. Chúng tôi rất vui khi được phục vụ bạn!" +
                "</p>" +
                "</div>" +
                "<!-- Review Request -->" +
                "<div style=\"background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #856404; margin: 0 0 10px 0; font-size: 16px;\">⭐ Đánh giá sản phẩm</h3>" +
                "<p style=\"color: #856404; margin: 0 0 15px 0;\">Bạn có hài lòng với sản phẩm không? Hãy để lại đánh giá để giúp chúng tôi cải thiện dịch vụ!</p>" +
                "<a href=\"http://localhost:5173/orders\" style=\"display: inline-block; background-color: #ffc107; color: #212529; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600;\">" +
                "Đánh giá ngay" +
                "</a>" +
                "</div>" +
                "<!-- Next Shopping -->" +
                "<div style=\"background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #333; margin: 0 0 10px 0; font-size: 16px;\">🛍️ Tiếp tục mua sắm</h3>" +
                "<p style=\"color: #666; margin: 0 0 15px 0;\">Khám phá thêm nhiều sản phẩm mới và ưu đãi hấp dẫn!</p>" +
                "<a href=\"http://localhost:5173\" style=\"display: inline-block; background-color: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600;\">" +
                "Xem sản phẩm mới" +
                "</a>" +
                "</div>" +
                "<!-- Support -->" +
                "<div style=\"background-color: #e3f2fd; border-left: 4px solid #2196f3; padding: 20px; margin-bottom: 20px;\">" +
                "<h3 style=\"color: #1565c0; margin: 0 0 10px 0; font-size: 16px;\">🆘 Hỗ trợ khách hàng</h3>" +
                "<p style=\"color: #1565c0; margin: 0; line-height: 1.6;\">" +
                "Nếu bạn có bất kỳ thắc mắc nào về đơn hàng hoặc cần hỗ trợ, vui lòng liên hệ với chúng tôi:" +
                "</p>" +
                "<ul style=\"color: #1565c0; margin: 10px 0 0 0; padding-left: 20px;\">" +
                "<li>Email: shopshoozy@gmail.com</li>" +
                "<li>Thời gian: 8:00 - 22:00 (T2-CN)</li>" +
                "</ul>" +
                "</div>" +
                "</div>" +
                "<!-- Footer -->" +
                "<div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "<p style=\"color: #666; margin: 0 0 10px 0;\">Cảm ơn bạn đã tin tưởng Shoozy Shop!</p>" +
                "<p style=\"color: #666; margin: 0; font-size: 14px;\">" +
                "Nếu có thắc mắc, vui lòng liên hệ: <a href=\"mailto:shopshoozy@gmail.com\" style=\"color: #667eea;\">shopshoozy@gmail.com</a>" +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
