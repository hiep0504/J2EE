package com.example.Backend_J2EE.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutRequest {

    private String address;
    private String phone;
    private String paymentMethod;
    private List<CartCheckoutItemRequest> items;
}
