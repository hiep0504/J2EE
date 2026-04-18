package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReviewMediaResponse {
    private Integer id;
    private String mediaType;
    private String mediaUrl;
}
