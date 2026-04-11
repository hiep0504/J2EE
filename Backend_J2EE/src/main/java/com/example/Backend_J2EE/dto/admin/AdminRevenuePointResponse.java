package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AdminRevenuePointResponse {
    private LocalDate periodStart;
    private String label;
    private BigDecimal revenue;
    private Integer orderCount;
}
