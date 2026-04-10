package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AdminRevenueCustomerResponse {
    private Integer accountId;
    private String username;
    private BigDecimal revenue;
    private Integer orderCount;
    private BigDecimal percentOfTotal;
}
