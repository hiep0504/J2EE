package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.CategoryDTO;
import com.example.Backend_J2EE.entity.Category;
import com.example.Backend_J2EE.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getAllCategories_sortsCaseInsensitiveAndMapsToDto() {
        Category alpha = Category.builder().id(2).name("alpha").build();
        Category beta = Category.builder().id(1).name("Beta").build();

        when(categoryRepository.findAll()).thenReturn(List.of(beta, alpha));

        List<CategoryDTO> categories = categoryService.getAllCategories();

        assertEquals(2, categories.size());
        assertEquals(2, categories.get(0).getId());
        assertEquals("alpha", categories.get(0).getName());
        assertEquals(1, categories.get(1).getId());
        assertEquals("Beta", categories.get(1).getName());

        verify(categoryRepository).findAll();
    }

    @Test
    void getAllCategories_returnsEmptyListWhenRepositoryIsEmpty() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryDTO> categories = categoryService.getAllCategories();

        assertTrue(categories.isEmpty());
        verify(categoryRepository).findAll();
    }

    @Test
    void getAllCategories_returnsSingleItemWithoutChanges() {
        Category only = Category.builder().id(99).name("Only").build();
        when(categoryRepository.findAll()).thenReturn(List.of(only));

        List<CategoryDTO> categories = categoryService.getAllCategories();

        assertEquals(1, categories.size());
        assertEquals(99, categories.get(0).getId());
        assertEquals("Only", categories.get(0).getName());
    }

    @Test
    void getAllCategories_keepsOriginalOrderWhenNamesEqualIgnoringCase() {
        Category first = Category.builder().id(1).name("shoe").build();
        Category second = Category.builder().id(2).name("Shoe").build();
        when(categoryRepository.findAll()).thenReturn(List.of(first, second));

        List<CategoryDTO> categories = categoryService.getAllCategories();

        assertEquals(2, categories.size());
        assertEquals(1, categories.get(0).getId());
        assertEquals(2, categories.get(1).getId());
    }

    @Test
    void getAllCategories_throwsWhenCategoryNameIsNull() {
        Category invalid = Category.builder().id(3).name(null).build();
        Category valid = Category.builder().id(4).name("Ball").build();
        when(categoryRepository.findAll()).thenReturn(List.of(invalid, valid));

        assertThrows(NullPointerException.class, () -> categoryService.getAllCategories());
    }
}