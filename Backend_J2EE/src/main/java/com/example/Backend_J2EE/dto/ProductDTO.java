package com.example.Backend_J2EE.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Integer id;
    private String name;
    private BigDecimal price;
    private String description;
    private String image;
    private String categoryName;
    private LocalDateTime createdAt;
}
