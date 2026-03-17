package com.example.Backend_J2EE.dto.order;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderItemRequest {
    private Integer productSizeId;
    private Integer quantity;
}
