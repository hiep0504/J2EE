package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminCategoryResponse {
    private Integer id;
    private String name;
    private String description;
    private long productCount;
}
