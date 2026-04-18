package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminProductResponse {
    private Integer id;
    private String name;
    private BigDecimal price;
    private String description;
    private String image;
    private Integer categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private List<AdminProductImageResponse> images;
    private List<AdminProductSizeResponse> sizes;
}
