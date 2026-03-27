package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProductSizeResponse {
    private Integer id;
    private Integer sizeId;
    private String sizeName;
    private Integer quantity;
}
