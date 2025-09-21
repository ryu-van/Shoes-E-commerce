package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.response.AIResponse;
import com.example.shoozy_shop.dto.response.ProductAIResponse;
import com.example.shoozy_shop.dto.response.PromotionAIResponse;
import com.example.shoozy_shop.service.ProductDataService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@RestController
@RequestMapping("${api.prefix}/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private static final int AI_TIMEOUT_SECONDS = 20;

    @Autowired
    private ProductDataService productDataService;

    public ChatController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/message")
    public ResponseEntity<AIResponse> chatWithBot(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.ok(new AIResponse("text",
                    "👋 Xin chào! Chào mừng đến Shoozy Shop! " +
                            "🔥 Tôi có thể giúp bạn tìm giày sneaker phù hợp! " +
                            "📞 Hotline: 0358850836"));
        }

        try {
            message = message.trim();
            logger.info("Processing query: '{}'", message);

            // 1) Kiểm tra câu hỏi cơ bản trước
            AIResponse basicResponse = getBasicResponse(message);
            if (basicResponse != null) {
                return ResponseEntity.ok(basicResponse);
            }

            // 2) Phân tích xem cần tìm sản phẩm hay khuyến mãi
            QueryType queryType = analyzeQueryType(message);

            if (queryType == QueryType.PRODUCT) {
                return handleProductQuery(message);
            } else if (queryType == QueryType.PROMOTION) {
                return handlePromotionQuery(message);
            } else {
                return handleGeneralQuery(message);
            }

        } catch (Exception e) {
            logger.error("Unexpected error in chat: ", e);
            return ResponseEntity.ok(new AIResponse("text",
                    "⚠️ Hệ thống tạm thời quá tải. " +
                            "📞 Hotline: 0358850836 (24/7). " +
                            "Vui lòng thử lại sau ít phút!"));
        }
    }

    private enum QueryType {
        PRODUCT, PROMOTION, GENERAL
    }

    private QueryType analyzeQueryType(String message) {
        String lower = message.toLowerCase();

        // Từ khóa sản phẩm
        if (lower.contains("giày") || lower.contains("nike") || lower.contains("adidas") ||
                lower.contains("vans") || lower.contains("converse") || lower.contains("sneaker") ||
                lower.contains("tìm") || lower.contains("mua") || lower.contains("có") ||
                lower.contains("size") || lower.contains("màu") || lower.contains("giá") ||
                lower.matches(".*\\b(dưới|tầm|khoảng)\\s*[0-9]+\\s*(triệu|tr).*")) {
            return QueryType.PRODUCT;
        }

        // Từ khóa khuyến mãi
        if (lower.contains("khuyến mãi") || lower.contains("giảm giá") ||
                lower.contains("sale") || lower.contains("mã giảm") || lower.contains("ưu đãi")) {
            return QueryType.PROMOTION;
        }

        return QueryType.GENERAL;
    }

    private ResponseEntity<AIResponse> handleProductQuery(String message) {
        try {
            // Tìm sản phẩm từ vector store
            SearchContext searchContext = analyzeMessage(message);
            List<Document> productDocs = searchProducts(searchContext);

            if (!productDocs.isEmpty()) {
                // Convert Document thành ProductDTO
                List<ProductAIResponse> products = convertToProductDTOs(productDocs);

                String aiMessage = generateProductMessage(message, products, searchContext);

                return ResponseEntity.ok(new AIResponse("product", aiMessage, products));
            } else {
                // Không tìm thấy sản phẩm phù hợp
                return ResponseEntity.ok(new AIResponse("text",
                        "🔍 Không tìm thấy sản phẩm phù hợp với: \"" + message + "\"\n" +
                                "💡 Thử tìm với từ khóa khác như: Nike, Adidas, giày nam, giày nữ...\n" +
                                "📞 Hotline: 0358850836 để tư vấn trực tiếp!"));
            }

        } catch (Exception e) {
            logger.error("Error handling product query: ", e);
            return ResponseEntity.ok(new AIResponse("text", "❌ Lỗi khi tìm kiếm sản phẩm. Vui lòng thử lại!"));
        }
    }

    private ResponseEntity<AIResponse> handlePromotionQuery(String message) {
        try {
            List<Document> promotionDocs = searchPromotions(message);

            if (!promotionDocs.isEmpty()) {
                List<PromotionAIResponse> promotions = convertToPromotionDTOs(promotionDocs);

                String aiMessage = "🔥 Các khuyến mãi hiện tại tại Shoozy Shop:";

                return ResponseEntity.ok(new AIResponse("promotion", aiMessage, promotions));
            } else {
                return ResponseEntity.ok(new AIResponse("text",
                        "🎯 Hiện tại chúng tôi có các ưu đãi:\n" +
                                "• 🚚 Freeship đơn từ 1.000.000đ\n" +
                                "• 🧦 Tặng vớ cao cấp khi mua từ 2 đôi\n" +
                                "📞 Gọi 0358850836 để biết thêm khuyến mãi!"));
            }

        } catch (Exception e) {
            logger.error("Error handling promotion query: ", e);
            return ResponseEntity.ok(new AIResponse("text", "❌ Lỗi khi tìm kiếm khuyến mãi. Vui lòng thử lại!"));
        }
    }

    private ResponseEntity<AIResponse> handleGeneralQuery(String message) {
        // Xử lý câu hỏi chung bằng AI
        try {
            String context = searchVectorStoreGeneral(message);
            String aiResponse = getAIResponseWithTimeout(message, context);

            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                return ResponseEntity.ok(new AIResponse("text", aiResponse));
            } else {
                return ResponseEntity.ok(new AIResponse("text",
                        "💬 Để được tư vấn nhanh về: \"" + message + "\"\n" +
                                "📞 Gọi ngay: 0358850836\n" +
                                "🌐 Website: https://shoozy.vn"));
            }

        } catch (Exception e) {
            logger.error("Error handling general query: ", e);
            return ResponseEntity.ok(new AIResponse("text",
                    "📞 Vui lòng gọi 0358850836 để được tư vấn trực tiếp!"));
        }
    }

    // ===== BASIC RESPONSES =====
    private AIResponse getBasicResponse(String message) {
        if (message == null) return null;
        String lower = message.toLowerCase().trim();

        // Chào hỏi
        if (lower.matches("^(hi|hello|xin chào|chào|alo|yo|hey)[!. ]*$")) {
            return new AIResponse("text",
                    "👋 Xin chào! Chào mừng đến Shoozy Shop!\n" +
                            "🔥 Chuyên giày sneaker chính hãng (Nike, Adidas, Vans, Converse...)\n" +
                            "💬 Bạn muốn tìm mẫu hoặc mức giá nào?\n" +
                            "📞 Hotline: 0358850836");
        }

        // Liên hệ
        if (lower.contains("liên hệ") || lower.contains("hotline") ||
                lower.contains("số điện thoại") || lower.contains("contact") ||
                lower.contains("phone") || lower.contains("địa chỉ")) {
            return new AIResponse("contact",
                    "📞 **Liên hệ Shoozy Shop:**\n" +
                            "• **Hotline:** 0358850836 (24/7)\n" +
                            "• **Email:** shoozyshop@gmail.com\n" +
                            "• **Website:** https://shoozy.vn\n" +
                            "• **Địa chỉ:** Bắc Từ Liêm, Hà Nội\n" +
                            "💬 Gọi ngay để tư vấn size & giá nhanh!");
        }

        // Chính sách đổi trả
        if (lower.contains("đổi trả") || lower.contains("trả hàng") ||
                lower.contains("hoàn hàng") || lower.contains("bảo hành") ||
                lower.contains("chính sách")) {
            return new AIResponse("policy",
                    "♻️ **Chính sách đổi trả Shoozy Shop:**\n" +
                            "• ✅ Đổi trả trong **7 ngày** khi chưa sử dụng, còn tem mác\n" +
                            "• 🔄 Hỗ trợ đổi size miễn phí (cùng mẫu, cùng giá)\n" +
                            "• 💰 Hoàn tiền 100% nếu lỗi từ shop\n" +
                            "• 📦 Bảo hành keo chỉ theo chính sách hãng\n" +
                            "📞 Hỗ trợ nhanh: 0358850836\n" +
                            "🌐 Chi tiết: https://shoozy.vn/chinh-sach");
        }

        // Hướng dẫn size
        if (lower.contains("size") || lower.contains("cỡ") ||
                lower.contains("đo chân") || lower.contains("chọn size")) {
            return new AIResponse("policy",
                    "📏 **Hướng dẫn chọn size giày:**\n" +
                            "1️⃣ Đo chiều dài bàn chân (cm)\n" +
                            "2️⃣ Tra bảng size trên trang sản phẩm\n" +
                            "3️⃣ **Lưu ý:** Nike thường nhỏ hơn Adidas 0.5 size\n" +
                            "4️⃣ Phân vân → chọn tăng 0.5 size\n" +
                            "💬 Gửi chiều dài bàn chân để mình tư vấn size chuẩn!\n" +
                            "📞 Hotline: 0358850836");
        }

        // Khuyến mãi chung
        if ((lower.contains("sale") || lower.contains("khuyến mãi") ||
                lower.contains("giảm giá") || lower.contains("mã giảm") ||
                lower.contains("ưu đãi")) && !isSpecificPromotionQuery(lower)) {
            return new AIResponse("text",
                    "🔥 **Khuyến mãi hiện tại:**\n" +
                            "• 🎯 Giảm đến **30%** một số mẫu Nike/Adidas\n" +
                            "• 🚚 **Freeship** đơn từ 1.000.000đ\n" +
                            "• 🧦 Tặng vớ cao cấp khi mua từ 2 đôi\n" +
                            "• 💳 Ưu đãi thêm khi thanh toán online\n" +
                            "📞 Hotline: 0358850836 (tư vấn mã giảm)\n" +
                            "🌐 Xem thêm: https://shoozy.vn/khuyen-mai");
        }

        // Giao hàng
        if (lower.contains("giao hàng") || lower.contains("ship") ||
                lower.contains("vận chuyển") || lower.contains("bao lâu") ||
                lower.contains("khi nào nhận")) {
            return new AIResponse("policy",
                    "🚚 **Thông tin giao hàng:**\n" +
                            "• ⚡ Nội thành HN/HCM: **1–2 ngày**\n" +
                            "• 🌍 Tỉnh thành khác: **2–4 ngày** (GHN/GHTK)\n" +
                            "• 🆓 **Freeship** đơn từ 1.000.000đ\n" +
                            "• 📦 Kiểm hàng trước khi thanh toán\n" +
                            "📞 Hotline: 0358850836\n" +
                            "🌐 Tra cứu đơn hàng: https://shoozy.vn");
        }

        // Thanh toán
        if (lower.contains("thanh toán") || lower.contains("payment") ||
                lower.contains("trả tiền") || lower.contains("cod")) {
            return new AIResponse("policy",
                    "💳 **Phương thức thanh toán:**\n" +
                            "• 🏪 **COD:** Thanh toán khi nhận hàng\n" +
                            "• 🏧 **Chuyển khoản:** Techcombank, VietcomBank\n" +
                            "• 💳 **Ví điện tử:** MoMo, ZaloPay\n" +
                            "• 🎯 **Ưu đãi:** Giảm thêm khi thanh toán online\n" +
                            "📞 Hỗ trợ: 0358850836");
        }

        // Thông tin shop
        if (lower.contains("về shop") || lower.contains("giới thiệu") ||
                lower.contains("about") || lower.contains("shoozy là gì")) {
            return new AIResponse("text",
                    "🏪 **Về Shoozy Shop:**\n" +
                            "• 👟 Chuyên giày sneaker chính hãng từ 2020\n" +
                            "• ✅ **Cam kết:** 100% hàng real, check trước ship\n" +
                            "• 🌟 **Thương hiệu:** Nike, Adidas, Vans, Converse, Puma...\n" +
                            "• 📍 **Địa chỉ:** Quốc Oai, Hà Nội\n" +
                            "• 🤝 **Phục vụ:** 1000+ khách hàng hài lòng\n" +
                            "📞 Hotline: 0358850836");
        }

        return null;
    }

    // Kiểm tra xem có phải hỏi khuyến mãi cụ thể không
    private boolean isSpecificPromotionQuery(String lower) {
        return lower.contains("nike") || lower.contains("adidas") ||
                lower.contains("vans") || lower.contains("converse") ||
                lower.matches(".*\\b(dưới|tầm|khoảng)\\s*[0-9]+\\s*(triệu|tr).*");
    }

    // ===== SEARCH METHODS =====
    private List<Document> searchProducts(SearchContext context) {
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(context.enhancedQuery)
                    .topK(8)
                    .similarityThreshold(0.4);

            if (context.hasPrice()) {
                String priceFilter = String.format("min_price <= %f && max_price >= %f",
                        context.maxPrice, context.minPrice);
                builder.filterExpression(priceFilter + " && type == 'product'");
            } else {
                builder.filterExpression("type == 'product'");
            }

            return vectorStore.similaritySearch(builder.build());

        } catch (Exception e) {
            logger.error("Product search failed: ", e);
            return new ArrayList<>();
        }
    }

    private List<Document> searchPromotions(String message) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message + " khuyến mãi ưu đãi")
                            .topK(5)
                            .filterExpression("type == 'promotion'")
                            .similarityThreshold(0.4)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Promotion search failed: ", e);
            return new ArrayList<>();
        }
    }

    // ===== DTO CONVERTERS =====
    private List<ProductAIResponse> convertToProductDTOs(List<Document> docs) {
        return docs.stream().map(doc -> {
            Map<String, Object> metadata = doc.getMetadata();
            return new ProductAIResponse(
                    Long.parseLong(metadata.get("product_id").toString()),
                    metadata.get("product_name").toString(),
                    metadata.get("product_thumbnail").toString(),
                    extractDescriptionFromContent(doc.getFormattedContent()),
                    metadata.get("brand").toString(),
                    metadata.get("category").toString(),
                    metadata.get("gender").toString(),
                    metadata.get("material").toString(),
                    ((Double) metadata.get("min_price")).longValue(),
                    ((Double) metadata.get("max_price")).longValue(),
                    (Long) metadata.get("total_quantity"),
                    metadata.get("available_sizes").toString(),
                    metadata.get("available_colors").toString()
            );
        }).collect(Collectors.toList());
    }

    private List<PromotionAIResponse> convertToPromotionDTOs(List<Document> docs) {
        // Implement conversion từ Document sang PromotionDTO
        // Tương tự như ProductDTO
        return new ArrayList<>(); // Placeholder
    }

    private String extractDescriptionFromContent(String content) {
        // Extract description from document content
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("Mô tả:")) {
                return line.substring(6).trim();
            }
        }
        return "";
    }

    // ===== AI MESSAGE GENERATION =====
    private String generateProductMessage(String query, List<ProductAIResponse> products, SearchContext context) {
        StringBuilder message = new StringBuilder();

        message.append("🔍 Tìm thấy ").append(products.size()).append(" sản phẩm phù hợp");
        if (context.hasPrice()) {
            message.append(" trong khoảng giá ").append(context.getPriceRangeText());
        }
        message.append(":\n\n");

        // Highlight một vài sản phẩm nổi bật
        if (!products.isEmpty()) {
            ProductAIResponse firstProduct = products.get(0);
            message.append("✨ Nổi bật: ").append(firstProduct.getName())
                    .append(" - ").append(firstProduct.getPriceDisplay()).append("\n");
        }

        message.append("\n📞 Gọi 0358850836 để tư vấn size và đặt hàng!");

        return message.toString();
    }

    // ===== SEARCH CONTEXT (giữ nguyên từ code cũ) =====
    private static class SearchContext {
        String originalQuery;
        String enhancedQuery;
        double minPrice = 0;
        double maxPrice = Double.MAX_VALUE;
        boolean hasPriceFilter = false;

        public boolean hasPrice() { return hasPriceFilter; }

        public String getPriceRangeText() {
            if (maxPrice == Double.MAX_VALUE) {
                return String.format("dưới %,.0f VND", minPrice);
            } else if (minPrice == 0) {
                return String.format("dưới %,.0f VND", maxPrice);
            } else {
                return String.format("từ %,.0f - %,.0f VND", minPrice, maxPrice);
            }
        }
    }

    private SearchContext analyzeMessage(String message) {
        SearchContext context = new SearchContext();
        context.originalQuery = message;
        context.enhancedQuery = message;

        String lowerMessage = message.toLowerCase();

        // Phân tích giá (giữ logic cũ)
        Pattern pricePattern = Pattern.compile("(dưới|under)\\s*([0-9.,]+)\\s*(triệu|tr)");
        Matcher priceMatcher = pricePattern.matcher(lowerMessage);
        if (priceMatcher.find()) {
            try {
                String priceStr = priceMatcher.group(2).replace(",", "").replace(".", "");
                double price = Double.parseDouble(priceStr) * 1_000_000;
                context.maxPrice = price;
                context.hasPriceFilter = true;
            } catch (NumberFormatException e) {
                logger.warn("Could not parse price: {}", priceMatcher.group(2));
            }
        }

        return context;
    }

    // ===== OTHER METHODS (giữ nguyên) =====
    private String searchVectorStoreGeneral(String message) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(3)
                            .similarityThreshold(0.4)
                            .build()
            );

            return docs.stream()
                    .map(Document::getFormattedContent)
                    .collect(Collectors.joining("\n\n---\n\n"));
        } catch (Exception e) {
            logger.error("General vector search failed: ", e);
            return "";
        }
    }

    private String getAIResponseWithTimeout(String message, String context) {
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                String prompt = createShortPrompt(message, context);
                return chatClient.prompt().user(prompt).call().content();
            }, executorService);

            return future.get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("AI response failed: ", e);
            return null;
        }
    }

    private String createShortPrompt(String message, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là Bot tư vấn Shoozy Shop. Trả lời NGẮN bằng tiếng Việt.\n");

        if (context != null && !context.isEmpty()) {
            String miniContext = context.length() > 300 ? context.substring(0, 300) + "..." : context;
            prompt.append("Data: ").append(miniContext).append("\n");
        }

        prompt.append("Hỏi: ").append(message).append("\n");
        prompt.append("Trả lời:\n");

        return prompt.toString();
    }

    // ===== INDEX DATA =====
    @PostMapping("/index-data")
    public ResponseEntity<Map<String, String>> indexData() {
        try {
            productDataService.indexProductData();
            productDataService.indexPromotionData();
            productDataService.indexPoliciesData();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Đã index dữ liệu thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error indexing data: ", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Lỗi khi index dữ liệu: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}