package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProductImageResponse {
    private Integer id;
    private String imageUrl;
    private Boolean isMain;
}
