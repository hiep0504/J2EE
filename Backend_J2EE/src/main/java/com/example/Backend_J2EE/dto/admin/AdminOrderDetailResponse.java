package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminOrderDetailResponse {
    private Integer id;
    private Integer accountId;
    private String username;
    private BigDecimal totalPrice;
    private String status;
    private String address;
    private String phone;
    private LocalDateTime orderDate;
    private List<AdminOrderItemResponse> items;
}
