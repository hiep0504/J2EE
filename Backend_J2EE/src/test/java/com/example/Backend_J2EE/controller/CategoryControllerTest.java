package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.CategoryDTO;
import com.example.Backend_J2EE.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    void getAllCategories_delegatesToService() {
        List<CategoryDTO> categories = List.of(new CategoryDTO(1, "Shoes"));
        when(categoryService.getAllCategories()).thenReturn(categories);

        var response = categoryController.getAllCategories();

        assertSame(categories, response.getBody());
        verify(categoryService).getAllCategories();
    }

    @Test
    void getAllCategories_returnsEmptyListWhenServiceIsEmpty() {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        var response = categoryController.getAllCategories();

        assertTrue(response.getBody().isEmpty());
        verify(categoryService).getAllCategories();
    }
}