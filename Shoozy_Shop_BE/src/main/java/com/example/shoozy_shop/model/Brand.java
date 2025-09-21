package com.example.shoozy_shop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "brands")

public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Name brand cannot be blank")
    private String name;
    @NotBlank(message = "Description brand cannot be blank")
    private String description;
    @NotBlank(message = "Country brand cannot be blank")
    private String country;
    @Column(insertable = false)
    private Boolean status;

}
