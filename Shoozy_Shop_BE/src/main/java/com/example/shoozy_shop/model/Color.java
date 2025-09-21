package com.example.shoozy_shop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "colors")

public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Name color can't be blank")
    private String name;
    @Column(name = "hex_code")
    @NotBlank(message = "Hex code can't be blank")
    private String hexCode;
    @Column(insertable = false)
    private Boolean status;
}
