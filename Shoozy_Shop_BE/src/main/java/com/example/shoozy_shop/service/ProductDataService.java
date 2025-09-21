package com.example.shoozy_shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductDataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private VectorStore vectorStore;

    public void indexProductData() {
        String sql = """
            SELECT
                p.id as product_id,
                p.name as product_name,
                p.thumbnail as product_thumbnail,
                p.description as product_description,
                b.name as brand_name,
                c.name as category_name,
                p.product_gender,
                m.name as material_name,
                MIN(pv.sell_price) as min_price,
                MAX(pv.sell_price) as max_price,
                COUNT(DISTINCT CONCAT(s.value, '-', col.name)) as variant_count,
                (SELECT STRING_AGG(s2.value, ', ') WITHIN GROUP (ORDER BY s2.value)
                 FROM (SELECT DISTINCT s2.value
                       FROM product_variants pv2
                       JOIN sizes s2 ON pv2.size_id = s2.id
                       WHERE pv2.product_id = p.id
                         AND pv2.status = 1
                         AND pv2.quantity > 0) s2) as available_sizes,
                (SELECT STRING_AGG(col2.name, ', ') WITHIN GROUP (ORDER BY col2.name)
                 FROM (SELECT DISTINCT col2.name
                       FROM product_variants pv3
                       JOIN colors col2 ON pv3.color_id = col2.id
                       WHERE pv3.product_id = p.id
                         AND pv3.status = 1
                         AND pv3.quantity > 0) col2) as available_colors,
                SUM(pv.quantity) as total_quantity
            FROM products p
            LEFT JOIN brands b ON p.brand_id = b.id
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN materials m ON p.material_id = m.id
            LEFT JOIN product_variants pv ON p.id = pv.product_id AND pv.status = 1 AND pv.quantity > 0
            LEFT JOIN sizes s ON pv.size_id = s.id
            LEFT JOIN colors col ON pv.color_id = col.id
            WHERE p.status = 1
            GROUP BY p.id, p.name, p.thumbnail, p.description, b.name, c.name, p.product_gender, m.name
            HAVING SUM(pv.quantity) > 0
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            List<Document> documents = new ArrayList<>();

            System.out.println("Found " + results.size() + " unique products");

            for (Map<String, Object> row : results) {
                // Kiểm tra null values trước khi xử lý
                if (row.get("product_id") == null || row.get("product_name") == null) {
                    continue;
                }

                // Format giá
                String priceInfo;
                Object minPrice = row.get("min_price");
                Object maxPrice = row.get("max_price");

                if (minPrice != null && maxPrice != null) {
                    if (minPrice.equals(maxPrice)) {
                        priceInfo = String.format("%,d VND", ((Number) minPrice).longValue());
                    } else {
                        priceInfo = String.format("%,d - %,d VND",
                                ((Number) minPrice).longValue(),
                                ((Number) maxPrice).longValue());
                    }
                } else {
                    priceInfo = "Liên hệ";
                }

                // Convert gender to Vietnamese and get target audience
                String originalGender = safeToString(row.get("product_gender"));
                String vietnameseGender = convertGenderToVietnamese(originalGender);
                String targetAudience = getTargetAudience(originalGender);

                // Tạo nội dung chi tiết cho từng sản phẩm
                String content = String.format(
                        "=== SẢN PHẨM: %s ===\n" +
                                "ID: %s\n" +
                                "Ảnh đại diện: %s\n\n" +
                                "Thương hiệu: %s\n" +
                                "Danh mục: %s\n" +
                                "Dành cho: %s\n" +
                                "Giới tính: %s\n" +
                                "Chất liệu: %s\n" +
                                "Mô tả: %s\n\n" +
                                "=== THÔNG TIN BÁN HÀNG ===\n" +
                                "Giá bán: %s\n" +
                                "Tồn kho: %s đôi\n" +
                                "Số lượng phiên bản: %s variants\n\n" +
                                "=== CÁC LỰA CHỌN ===\n" +
                                "Kích cỡ có sẵn: %s\n" +
                                "Màu sắc có sẵn: %s\n\n" +
                                "=== TƯ VẤN ===\n" +
                                "Sản phẩm phù hợp cho: %s\n" +
                                "Đối tượng khách hàng: %s\n" +
                                "Phong cách: %s %s\n" +
                                "Từ khóa tìm kiếm: %s\n" +
                                "Gọi 0358850836 để được tư vấn cụ thể về size và màu sắc phù hợp.",

                        safeToString(row.get("product_name")),
                        safeToString(row.get("product_id")),
                        safeToString(row.get("product_thumbnail")),
                        safeToString(row.get("brand_name")),
                        safeToString(row.get("category_name")),
                        vietnameseGender,
                        originalGender,
                        safeToString(row.get("material_name")),
                        safeToString(row.get("product_description")),
                        priceInfo,
                        row.get("total_quantity") != null ? row.get("total_quantity") : 0,
                        row.get("variant_count") != null ? row.get("variant_count") : 0,
                        safeToString(row.get("available_sizes")),
                        safeToString(row.get("available_colors")),
                        vietnameseGender,
                        targetAudience,
                        safeToString(row.get("brand_name")),
                        safeToString(row.get("category_name")),
                        generateSearchKeywords(originalGender, safeToString(row.get("category_name")))
                );

                Document doc = new Document(content);

                // Metadata cho search - đảm bảo không null
                doc.getMetadata().put("product_id", String.valueOf(row.get("product_id")));
                doc.getMetadata().put("product_name", safeToString(row.get("product_name")));
                doc.getMetadata().put("product_thumbnail", safeToString(row.get("product_thumbnail")));
                doc.getMetadata().put("brand", safeToString(row.get("brand_name")));
                doc.getMetadata().put("category", safeToString(row.get("category_name")));

                // Lưu cả English và Vietnamese gender
                doc.getMetadata().put("gender", originalGender);
                doc.getMetadata().put("gender_vietnamese", vietnameseGender);
                doc.getMetadata().put("target_audience", targetAudience);
                doc.getMetadata().put("material", safeToString(row.get("material_name")));

                // Lưu cả min và max price để filter - xử lý null
                if (minPrice != null && maxPrice != null) {
                    doc.getMetadata().put("min_price", ((Number) minPrice).doubleValue());
                    doc.getMetadata().put("max_price", ((Number) maxPrice).doubleValue());
                } else {
                    doc.getMetadata().put("min_price", 0.0);
                    doc.getMetadata().put("max_price", 0.0);
                }

                doc.getMetadata().put("total_quantity",
                        row.get("total_quantity") != null ? ((Number) row.get("total_quantity")).intValue() : 0);

                // Metadata cho tìm kiếm
                doc.getMetadata().put("type", "product");
                doc.getMetadata().put("available_sizes", safeToString(row.get("available_sizes")));
                doc.getMetadata().put("available_colors", safeToString(row.get("available_colors")));

                // Thêm search keywords để chatbot dễ tìm
                doc.getMetadata().put("search_keywords",
                        generateSearchKeywords(originalGender, safeToString(row.get("category_name"))));

                documents.add(doc);

                // Log để debug
                System.out.println("Indexed product: " + row.get("product_name") +
                        " - Gender: " + originalGender + " (" + vietnameseGender + ")" +
                        " - Price: " + priceInfo +
                        " - Variants: " + row.get("variant_count"));
            }

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                System.out.println("Successfully indexed " + documents.size() + " unique products");
            } else {
                System.out.println("No products to index");
            }

        } catch (Exception e) {
            System.err.println("Error in indexProductData: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method để convert gender sang tiếng Việt
    private String convertGenderToVietnamese(String gender) {
        if (gender == null) return "Không xác định";

        switch (gender.toLowerCase()) {
            case "male":
                return "Nam";
            case "female":
                return "Nữ";
            case "unisex":
                return "Unisex (Nam/Nữ)";
            case "kids":
                return "Trẻ em";
            default:
                return gender;
        }
    }

    // Helper method để xác định đối tượng khách hàng
    private String getTargetAudience(String gender) {
        if (gender == null) return "Tất cả khách hàng";

        switch (gender.toLowerCase()) {
            case "male":
                return "Nam giới, đàn ông, phái mạnh";
            case "female":
                return "Nữ giới, phụ nữ, chị em";
            case "unisex":
                return "Nam và nữ, mọi giới tính, tất cả mọi người";
            case "kids":
                return "Trẻ em, bé trai, bé gái, học sinh";
            default:
                return "Tất cả khách hàng";
        }
    }

    // Helper method để tạo keywords tìm kiếm
    private String generateSearchKeywords(String gender, String category) {
        StringBuilder keywords = new StringBuilder();

        if (gender != null) {
            switch (gender.toLowerCase()) {
                case "male":
                    keywords.append("giày nam, giày đàn ông, giày cho nam, giày phái mạnh, ");
                    break;
                case "female":
                    keywords.append("giày nữ, giày phụ nữ, giày cho nữ, giày chị em, ");
                    break;
                case "unisex":
                    keywords.append("giày unisex, giày nam nữ, giày cho mọi người, ");
                    break;
                case "kids":
                    keywords.append("giày trẻ em, giày bé trai, giày bé gái, giày học sinh, ");
                    break;
            }
        }

        if (category != null && !category.isEmpty()) {
            keywords.append(category.toLowerCase()).append(", ");
        }

        return keywords.toString();
    }

    // Helper method để xử lý null values
    private String safeToString(Object value) {
        return value != null ? value.toString() : "";
    }

    public void indexPromotionData() {
        String sql = """
            SELECT
                pr.name as promotion_name,
                pr.code as promotion_code,
                pr.value as promotion_value,
                pr.start_date,
                pr.expiration_date,
                p.name as product_name
            FROM promotions pr
            LEFT JOIN product_promotions pp ON pr.id = pp.promotion_id
            LEFT JOIN product_variants pv ON pp.product_variant_id = pv.id
            LEFT JOIN products p ON pv.product_id = p.id
            WHERE pr.status = 1 AND pr.expiration_date > GETDATE()
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            List<Document> documents = new ArrayList<>();

            System.out.println("Found " + results.size() + " promotion records");

            for (Map<String, Object> row : results) {
                String content = String.format(
                        "Khuyến mãi: %s\n" +
                                "Mã giảm giá: %s\n" +
                                "Giá trị giảm: %s\n" +
                                "Áp dụng cho sản phẩm: %s\n" +
                                "Thời gian: từ %s đến %s",
                        row.get("promotion_name"),
                        row.get("promotion_code"),
                        row.get("promotion_value"),
                        row.get("product_name"),
                        row.get("start_date"),
                        row.get("expiration_date")
                );

                Document doc = new Document(content);
                doc.getMetadata().put("type", "promotion");

                // Chuyển đổi an toàn
                Object promotionCode = row.get("promotion_code");
                if (promotionCode != null) {
                    doc.getMetadata().put("promotion_code", promotionCode.toString());
                }

                // Xử lý promotion_value (có thể là số)
                Object promotionValue = row.get("promotion_value");
                if (promotionValue != null) {
                    if (promotionValue instanceof BigDecimal) {
                        doc.getMetadata().put("promotion_value", ((BigDecimal) promotionValue).doubleValue());
                    } else if (promotionValue instanceof Long) {
                        doc.getMetadata().put("promotion_value", ((Long) promotionValue).doubleValue());
                    } else if (promotionValue instanceof Integer) {
                        doc.getMetadata().put("promotion_value", ((Integer) promotionValue).doubleValue());
                    } else {
                        doc.getMetadata().put("promotion_value", Double.valueOf(promotionValue.toString()));
                    }
                }

                documents.add(doc);
            }

            // QUAN TRỌNG: Chỉ add khi có dữ liệu
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                System.out.println("Successfully indexed " + documents.size() + " promotion documents");
            } else {
                System.out.println("No promotion documents to index");
            }

        } catch (Exception e) {
            System.err.println("Error in indexPromotionData: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void indexPoliciesData() {
        List<Document> policyDocuments = new ArrayList<>();

        // Chính sách đổi trả
        Document returnPolicy = new Document(
                "Chính sách đổi trả Shoozy Shop:\n" +
                        "- Đổi trả trong vòng 30 ngày kể từ ngày mua\n" +
                        "- Sản phẩm phải còn nguyên vẹn, chưa sử dụng\n" +
                        "- Có hóa đơn mua hàng\n" +
                        "- Miễn phí đổi trả cho lỗi từ nhà sản xuất\n" +
                        "- Phí đổi trả 50,000 VND cho trường hợp đổi ý"
        );
        returnPolicy.getMetadata().put("type", "policy");
        returnPolicy.getMetadata().put("category", "return");

        // Chính sách vận chuyển
        Document shippingPolicy = new Document(
                "Chính sách vận chuyển Shoozy Shop:\n" +
                        "- Miễn phí vận chuyển cho đơn hàng trên 500,000 VND\n" +
                        "- Phí vận chuyển: 30,000 VND trong nội thành, 50,000 VND ngoại thành\n" +
                        "- Thời gian giao hàng: 2-3 ngày trong nội thành, 3-5 ngày ngoại thành\n" +
                        "- Giao hàng tận nơi, thanh toán khi nhận hàng"
        );
        shippingPolicy.getMetadata().put("type", "policy");
        shippingPolicy.getMetadata().put("category", "shipping");

        policyDocuments.add(returnPolicy);
        policyDocuments.add(shippingPolicy);

        vectorStore.add(policyDocuments);
    }
}