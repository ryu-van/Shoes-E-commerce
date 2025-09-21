package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.ProductRequest;
import com.example.shoozy_shop.dto.response.*;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.*;
import com.example.shoozy_shop.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProductService implements IProducService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final MaterialRepository materialRepository;

    @Override
    public Page<ProductResponse> searchProductByQueryBuilder(
            String keyword, List<Long> brandIds, List<Long> categoryIds,
            Double minPrice, Double maxPrice, List<Long> sizeIds, List<Long> colorIds, List<Long> materialIds,
            String gender, Boolean status, Integer pageNo, Integer pageSize, String sortBy, String sortDirection)
    {
        Specification<Product> specification = ((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            Join<Product, ProductVariant> variantJoin = root.join("productVariants", JoinType.LEFT);

            // Keyword search
            if (keyword != null && !keyword.trim().isEmpty()) {
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + keyword.toLowerCase() + "%"
                );

                Predicate skuPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("sku")),
                        "%" + keyword.toLowerCase() + "%"
                );

                // Kết hợp 2 điều kiện bằng OR
                Predicate keywordPredicate = criteriaBuilder.or(namePredicate, skuPredicate);
                predicates.add(keywordPredicate);
            }

            if (brandIds != null && !brandIds.isEmpty()) {
                predicates.add(root.get("brand").get("id").in(brandIds));
            }

            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            if (materialIds != null && !materialIds.isEmpty()) {
                predicates.add(root.get("material").get("id").in(materialIds));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(variantJoin.get("price"), maxPrice));
            }

            if (sizeIds != null && !sizeIds.isEmpty()) {
                predicates.add(variantJoin.get("size").get("id").in(sizeIds));
            }

            if (colorIds != null && !colorIds.isEmpty()) {
                predicates.add(variantJoin.get("color").get("id").in(colorIds));
            }

            if (gender != null && !gender.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });

        // Xử lý sorting đặc biệt cho price
        Pageable pageable;
        if ("price".equalsIgnoreCase(sortBy)) {
            List<Product> allProducts = productRepository.findAll(specification);
            // Remove duplicates vì có thể có duplicate do join với variants
            List<Product> distinctProducts = allProducts.stream()
                    .distinct()
                    .collect(Collectors.toList());

            // Tính giá thấp nhất cho mỗi product và sort
            List<ProductWithMinPrice> productsWithPrice = distinctProducts.stream()
                    .map(product -> {
                        Double productMinPrice = product.getProductVariants().stream()
                                .filter(variant -> variant.getSellPrice() != null)
                                .mapToDouble(ProductVariant::getSellPrice)
                                .min()
                                .orElse(Double.MAX_VALUE);
                        return new ProductWithMinPrice(product, productMinPrice);
                    })
                    .sorted((p1, p2) -> {
                        int comparison = Double.compare(p1.minPrice, p2.minPrice);
                        return "desc".equalsIgnoreCase(sortDirection) ? -comparison : comparison;
                    })
                    .collect(Collectors.toList());

            int start = (pageNo - 1) * pageSize;
            int end = Math.min(start + pageSize, productsWithPrice.size());

            List<Product> paginatedProducts = productsWithPrice.subList(start, end)
                    .stream()
                    .map(pwp -> pwp.product)
                    .collect(Collectors.toList());

            List<ProductResponse> productResponses = paginatedProducts.stream()
                    .map(product -> ProductResponse.fromProduct(product, sizeIds, colorIds, materialIds))
                    .collect(Collectors.toList());

            pageable = PageRequest.of(pageNo - 1, pageSize);
            return new PageImpl<>(productResponses, pageable, productsWithPrice.size());

        } else if ("rating".equalsIgnoreCase(sortBy)) {
            List<Product> allProducts = productRepository.findAll(specification);
            List<Product> distinctProducts = allProducts.stream().distinct().collect(Collectors.toList());

            List<ProductWithRating> productsWithRating = distinctProducts.stream()
                    .map(product -> {
                        Double avgRating = product.getReviews().stream()
                                .filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                                .filter(r -> r.getRating() != null)
                                .mapToInt(r -> r.getRating())
                                .average()
                                .orElse(0.0);
                        return new ProductWithRating(product, avgRating);
                    })
                    .sorted((p1, p2) -> {
                        int comparison = Double.compare(p1.rating, p2.rating);
                        return "desc".equalsIgnoreCase(sortDirection) ? -comparison : comparison;
                    })
                    .collect(Collectors.toList());

            int start = (pageNo - 1) * pageSize;
            int end = Math.min(start + pageSize, productsWithRating.size());

            List<Product> paginatedProducts = productsWithRating.subList(start, end)
                    .stream().map(pwr -> pwr.product).collect(Collectors.toList());

            List<ProductResponse> productResponses = paginatedProducts.stream()
                    .map(product -> ProductResponse.fromProduct(product, sizeIds, colorIds, materialIds))
                    .collect(Collectors.toList());

            pageable = PageRequest.of(pageNo - 1, pageSize);
            return new PageImpl<>(productResponses, pageable, productsWithRating.size());

        } else {
            // Sorting bình thường
            Sort sort = createSingleSort(sortBy, sortDirection);
            pageable = PageRequest.of(pageNo - 1, pageSize, sort);

            Page<Product> productPage = productRepository.findAll(specification, pageable);
            return productPage.map(product -> ProductResponse.fromProduct(product, sizeIds, colorIds, materialIds));
        }
    }

    private Sort createSingleSort(String sortBy, String sortDirection) {
        Sort.Direction direction = Sort.Direction.ASC;
        if ("desc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.DESC;
        }

        String sortField = "id";
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            switch (sortBy.toLowerCase().trim()) {
                case "createdat":
                case "created_at":
                    sortField = "createdAt";
                    break;
                default:
                    sortField = "id";
                    break;
            }
        }
        return Sort.by(direction, sortField);
    }

    // Helper class để lưu product và min price
    private static class ProductWithMinPrice {
        final Product product;
        final Double minPrice;

        ProductWithMinPrice(Product product, Double minPrice) {
            this.product = product;
            this.minPrice = minPrice;
        }
    }

    private static class ProductWithRating {
        final Product product;
        final Double rating;

        ProductWithRating(Product product, Double rating) {
            this.product = product;
            this.rating = rating;
        }
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product", id));
        return ProductResponse.fromProduct(product);
    }

    @Override
    @Transactional
    public Product addProduct(ProductRequest productRequest) {
        if (productRepository.existsByNameIgnoreCase(productRequest.getName())) {
            throw new IllegalArgumentException("Product " + productRequest.getName() + " already exists");
        }
        Brand existingBrand = brandRepository.findById(productRequest.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("brand", productRequest.getBrandId()));
        Category existingCategory = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("category", productRequest.getCategoryId()));
        Material existingMaterial = materialRepository.findById(productRequest.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("material", productRequest.getMaterialId()));
        String generatedSku = generateUniqueSku();

        Product newProduct = Product.builder()
                .sku(generatedSku)
                .thumbnail("http://localhost:9000/product-variant-images/%E3%83%8E%E3%83%BC%E3%82%A4%E3%83%A1%E3%83%BC%E3%82%B7%E3%82%99-760x460.png")
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .brand(existingBrand)
                .category(existingCategory)
                .gender(productRequest.getGender())
                .weight(productRequest.getWeight())
                .material(existingMaterial)
                .status(true)
                .build();
        return productRepository.save(newProduct);
    }

    private String generateUniqueSku() {
        String sku;
        do {
            sku = generateSku();
        } while (productRepository.existsBySku(sku));
        return sku;
    }

    private String generateSku() {
        int random = new Random().nextInt(1_000_000); // 0 → 999999
        return String.format("SP%06d", random);       // SP041241
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductRequest productRequest) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product", id));

        Brand brand = brandRepository.findById(productRequest.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("brand", productRequest.getBrandId()));

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("category", productRequest.getCategoryId()));

        Material material = materialRepository.findById(productRequest.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("material", productRequest.getMaterialId()));

        existingProduct.setName(productRequest.getName());
        existingProduct.setDescription(productRequest.getDescription());
        existingProduct.setBrand(brand);
        existingProduct.setCategory(category);
        existingProduct.setGender(productRequest.getGender());
        existingProduct.setWeight(productRequest.getWeight());
        existingProduct.setMaterial(material);
        existingProduct.setStatus(productRequest.getStatus());
        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product", id));
        existingProduct.setStatus(false);
        productRepository.save(existingProduct);
    }

    @Override
    public boolean existsProductByName(String name) {
        return productRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public List<ProductResponseForPromotion> getAllActiveProductsForPromotion(List<Long> productIds) {
        List<Object[]> rows = productRepository.findAllActiveProductAndVariantsByProductIds(productIds);
        Map<Long, ProductResponseForPromotion> map = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            String productName = String.valueOf(row[1]);

            // SUM(...) trên kiểu nguyên thường trả Long/BigInteger → ép qua Number
            Integer totalQuantity = ((Number) row[2]).intValue();

            Long variantId = ((Number) row[3]).longValue();

            // ⬇️ Tránh cast cứng sang String
            String size  = row[4] == null ? null : String.valueOf(row[4]);
            String color = row[5] == null ? null : String.valueOf(row[5]);

            // ⬇️ Giá dùng BigDecimal cho chuẩn tiền tệ
            java.math.BigDecimal price =
                    row[6] == null ? null :
                            (row[6] instanceof java.math.BigDecimal bd ? bd : new java.math.BigDecimal(row[6].toString()));

            Integer quantity = ((Number) row[7]).intValue();

            ProductVariantForPromotionResponse variant = ProductVariantForPromotionResponse.builder()
                    .id(variantId)
                    .size(size)
                    .color(color)
                    .price(price)         // đổi field sang BigDecimal trong DTO nếu có thể
                    .quantity(quantity)
                    .build();

            map.computeIfAbsent(productId, id -> ProductResponseForPromotion.builder()
                            .id(productId)
                            .name(productName)
                            .quantity(totalQuantity)
                            .variants(new ArrayList<>())
                            .build())
                    .getVariants().add(variant);
        }
        return new ArrayList<>(map.values());
    }





    @Override
    public List<ProductResponse> getProductsByCategoryId(Long categoryId, Long productId) {
        List<Product> productListByCategory = productRepository.findProductByCategoryIdAndIdNot(categoryId, productId);
        return productListByCategory.stream()
                .map(ProductResponse::fromProduct)
                .collect(Collectors.toList());
    }

    @Override
    public List<TopSellingProductResponse> getTopSellingProducts(String filterType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (filterType) {
            case "7days" -> startDate = now.minusDays(7);
            case "month" -> startDate = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            case "year" -> startDate = now.withDayOfYear(1).toLocalDate().atStartOfDay();
            default -> throw new IllegalArgumentException("Invalid filter type");
        }
        Pageable top5 = PageRequest.of(0, 5);
        return productRepository.findTopSellingProductsBetween(startDate, now, top5);
    }

    @Override
    public List<LowStockProductResponse> findLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    @Override
    public Page<ProductPromotionResponse> getProductPromotionPage(String productName, Long categoryId, Pageable pageable) {
        return productRepository.pageProductForPromotion(productName, categoryId, pageable);
    }

    @Override
    public Boolean restoreProduct(Long id) {
        Optional<Product> optional = productRepository.findById(id);
        if (optional.isPresent()) {
            Product product = optional.get();
            product.setStatus(true); // khôi phục
            productRepository.save(product);
            return true;
        }
        return false;
    }

}
