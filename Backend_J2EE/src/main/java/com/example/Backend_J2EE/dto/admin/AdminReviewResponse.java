package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminReviewResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private Integer accountId;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private List<AdminReviewMediaResponse> media;
}
