package com.example.Backend_J2EE.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailDTO {

    private Integer id;
    private String name;
    private BigDecimal price;
    private String description;
    private String image;
    private String categoryName;
    private LocalDateTime createdAt;
    private List<ProductImageDTO> images;
    private List<ProductSizeDTO> sizes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductImageDTO {
        private Integer id;
        private String imageUrl;
        private Boolean isMain;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSizeDTO {
        private Integer id;
        private String sizeName;
        private Integer quantity;
    }
}
