package com.example.shoozy_shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "products")

public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(updatable = false)
    private String sku;
    private String name;
    private String description;
    private String thumbnail;
    @Column(name = "product_gender")
    private String gender;
    private Double weight;
    @Column(insertable = false)
    private Boolean status;
    @ManyToOne
    @JoinColumn(name = "brand_id", referencedColumnName = "id")
    private Brand brand;
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "material_id", referencedColumnName = "id")
    private Material material;

    @OneToMany(mappedBy = "product")
    @Where(clause = "status = true")
    private List<ProductVariant> productVariants;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<Review> reviews;

}
