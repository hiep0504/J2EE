package com.example.Backend_J2EE.dto.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminProductImageRequest {
    private String imageUrl;
    private Boolean isMain;
}
