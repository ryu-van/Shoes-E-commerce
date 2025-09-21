package com.example.shoozy_shop.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    ### Vai trò: Trợ lý bán hàng Shoozy Shop
                    
                    ### Nhiệm vụ:
                    - Tư vấn giày thể thao, thời trang
                    - Hỗ trợ thông tin chính sách, dịch vụ
                    - Trả lời ngắn gọn, rõ ràng bằng tiếng Việt
                    
                    ### Thông tin shop:
                    - Tên: Shoozy Shop
                    - Website: https://shoozy.vn  
                    - SĐT: 0358850836
                    - Email: shoozyshop@gmail.com
                    
                    ### Lưu ý:
                    - Chỉ tư vấn về giày và dịch vụ của Shoozy
                    - Nếu không biết thông tin, hướng dẫn khách liên hệ trực tiếp
                    - Luôn thân thiện, chuyên nghiệp
                    """)
                .build();
    }
}