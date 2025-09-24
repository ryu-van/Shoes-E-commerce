package com.example.shoozy_shop.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "return_items")
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "return_request_id")
    private ReturnRequest returnRequest;

    @ManyToOne
    @JoinColumn(name = "order_detail_id")
    private OrderDetail orderDetail;

    private int quantity;

    // @Column(name = "return_status")
    // private String returnStatus = "WAITING";

    private String note;

    @OneToMany(mappedBy = "returnItem", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ReturnItemImage> images = new ArrayList<>();

    public List<String> getImageUrls() {
        if (images == null)
            return new ArrayList<>();
        return images.stream().map(ReturnItemImage::getImageUrl).toList();
    }

}