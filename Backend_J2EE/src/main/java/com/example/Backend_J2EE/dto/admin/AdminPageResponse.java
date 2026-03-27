package com.example.Backend_J2EE.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminPageResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}
