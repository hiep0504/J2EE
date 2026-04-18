package com.example.Backend_J2EE.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutItemRequest {

    private Integer productId;
    private Integer sizeId;
}
