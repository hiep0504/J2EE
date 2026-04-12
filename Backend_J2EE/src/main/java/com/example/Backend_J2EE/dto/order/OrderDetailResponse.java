package com.example.Backend_J2EE.dto.order;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailResponse extends OrderSummaryResponse {
    private List<OrderItemResponse> items = new ArrayList<>();
}
