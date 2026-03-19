package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.CategoryDTO;
import com.example.Backend_J2EE.entity.Category;
import com.example.Backend_J2EE.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
