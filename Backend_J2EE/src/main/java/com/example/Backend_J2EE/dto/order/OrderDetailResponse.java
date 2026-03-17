package com.example.Backend_J2EE.dto.order;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderDetailResponse extends OrderSummaryResponse {
    private List<OrderItemResponse> items = new ArrayList<>();
}
