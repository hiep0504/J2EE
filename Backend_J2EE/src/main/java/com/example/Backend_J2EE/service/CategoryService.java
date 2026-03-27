package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.CategoryDTO;
import com.example.Backend_J2EE.entity.Category;
import com.example.Backend_J2EE.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(category -> new CategoryDTO(category.getId(), category.getName()))
                .toList();
    }
}
