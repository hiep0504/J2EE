package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.ApiResponse;
import com.example.Backend_J2EE.dto.CategoryDTO;
import com.example.Backend_J2EE.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }
}
