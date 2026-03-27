package com.example.Backend_J2EE.dto.account;

import com.example.Backend_J2EE.dto.review.ReviewMediaDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AccountOwnedReviewResponse {
    private Integer id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private List<ReviewMediaDto> media = new ArrayList<>();
}
