package com.example.Backend_J2EE.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Integer productId;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal lineTotal;
}
