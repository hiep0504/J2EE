package com.example.Backend_J2EE.dto.order;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {
    private Integer accountId;
    private String address;
    private String phone;
    private List<CreateOrderItemRequest> items = new ArrayList<>();
}
