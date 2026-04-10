package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminRevenueSummaryResponse {
    private Integer months;
    private BigDecimal totalRevenue;
    private Integer totalOrders;
    private BigDecimal averageRevenuePerMonth;
    private BigDecimal trendPercent;
    private List<AdminRevenuePointResponse> points;
    private List<AdminRevenueCustomerResponse> topCustomers;
}
