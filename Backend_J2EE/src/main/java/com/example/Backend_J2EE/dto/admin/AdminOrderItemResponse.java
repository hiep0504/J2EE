package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AdminOrderItemResponse {
    private Integer id;
    private Integer productSizeId;
    private String productName;
    private String productImage;
    private String sizeName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal lineTotal;
}
