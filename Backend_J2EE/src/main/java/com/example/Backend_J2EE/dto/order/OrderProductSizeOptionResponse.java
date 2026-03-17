package com.example.Backend_J2EE.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductSizeOptionResponse {
    private Integer productSizeId;
    private Integer productId;
    private String productName;
    private String sizeName;
    private Integer stock;
    private BigDecimal price;
}
