package com.example.Backend_J2EE.dto.admin;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AdminProductRequest {
    private String name;
    private BigDecimal price;
    private String description;
    private String image;
    private Integer categoryId;
    private List<AdminProductImageRequest> images;
    private List<AdminProductSizeRequest> sizes;
}
