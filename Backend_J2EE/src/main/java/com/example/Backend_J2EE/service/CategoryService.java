package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.CategoryDTO;
import com.example.Backend_J2EE.entity.Category;
import com.example.Backend_J2EE.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDTO> getAllCategories() {
        Map<String, Category> uniqueByName = new LinkedHashMap<>();

        categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getId))
                .forEach(category -> {
                    String key = normalizeName(category.getName());
                    if (!key.isBlank()) {
                        uniqueByName.putIfAbsent(key, category);
                    }
                });

        return uniqueByName.values().stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(category -> new CategoryDTO(category.getId(), category.getName()))
                .toList();
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
