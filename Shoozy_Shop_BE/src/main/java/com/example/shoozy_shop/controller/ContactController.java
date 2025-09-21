package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.ContactRequest;
import com.example.shoozy_shop.dto.response.ApiResponse;
import com.example.shoozy_shop.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/contact")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendContactEmail(@Valid @RequestBody ContactRequest contactRequest) {
        try {
            log.info("Received contact request from: {}", contactRequest.getEmail());

            contactService.sendContactEmail(contactRequest);

            return ResponseEntity.ok(ApiResponse.success(
                    "Tin nhắn của bạn đã được gửi thành công. Chúng tôi sẽ phản hồi sớm nhất có thể!",
                    "Email sent successfully"
            ));

        } catch (Exception e) {
            log.error("Error sending contact email: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Có lỗi xảy ra khi gửi tin nhắn. Vui lòng thử lại sau."
            ));
        }
    }
}