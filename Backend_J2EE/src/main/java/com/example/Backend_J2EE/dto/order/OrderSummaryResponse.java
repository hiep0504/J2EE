package com.example.Backend_J2EE.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderSummaryResponse {
    private Integer id;
    private Integer accountId;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice;
    private String status;
    private String address;
    private String phone;
}
