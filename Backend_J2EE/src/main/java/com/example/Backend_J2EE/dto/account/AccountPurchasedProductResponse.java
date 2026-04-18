package com.example.Backend_J2EE.dto.account;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountPurchasedProductResponse {
    private Integer productId;
    private String productName;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime lastPurchasedAt;
    private boolean canReview;
    private AccountOwnedReviewResponse review;
}
