package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.ProductDTO;
import com.example.Backend_J2EE.entity.Category;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getAllProducts_mapsProductsToDtos() {
        Category category = Category.builder().id(7).name("Shoes").build();
        Product withCategory = Product.builder()
                .id(1)
                .name("Running Shoe")
                .price(new BigDecimal("199.99"))
                .description("Fast")
                .image("shoe.jpg")
                .category(category)
                .createdAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();
        Product withoutCategory = Product.builder()
                .id(2)
                .name("Cap")
                .price(new BigDecimal("29.99"))
                .description("Simple")
                .image("cap.jpg")
                .createdAt(LocalDateTime.of(2026, 4, 2, 10, 0))
                .build();

        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(withCategory, withoutCategory));

        List<ProductDTO> products = productService.getAllProducts();

        assertEquals(2, products.size());

        ProductDTO first = products.get(0);
        assertEquals(1, first.getId());
        assertEquals(7, first.getCategoryId());
        assertEquals("Running Shoe", first.getName());
        assertEquals(new BigDecimal("199.99"), first.getPrice());
        assertEquals("Fast", first.getDescription());
        assertEquals("shoe.jpg", first.getImage());
        assertEquals("Shoes", first.getCategoryName());
        assertEquals(LocalDateTime.of(2026, 4, 1, 10, 0), first.getCreatedAt());

        ProductDTO second = products.get(1);
        assertEquals(2, second.getId());
        assertNull(second.getCategoryId());
        assertNull(second.getCategoryName());

        verify(productRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getAllProducts_returnsEmptyListWhenRepositoryIsEmpty() {
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<ProductDTO> products = productService.getAllProducts();

        assertTrue(products.isEmpty());
        verify(productRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getAllProducts_handlesNullCategory() {
        Product product = Product.builder()
                .id(3)
                .name("Ball")
                .price(new BigDecimal("49.99"))
                .description("Outdoor")
                .image("ball.jpg")
                .createdAt(LocalDateTime.of(2026, 4, 3, 10, 0))
                .build();

        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(product));

        List<ProductDTO> products = productService.getAllProducts();

        assertEquals(1, products.size());
        assertNull(products.get(0).getCategoryId());
        assertNull(products.get(0).getCategoryName());
        verify(productRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getAllProducts_keepsRepositoryOrder() {
        Product newest = Product.builder().id(20).name("Newest").build();
        Product older = Product.builder().id(10).name("Older").build();
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(newest, older));

        List<ProductDTO> products = productService.getAllProducts();

        assertEquals(2, products.size());
        assertEquals(20, products.get(0).getId());
        assertEquals(10, products.get(1).getId());
    }

    @Test
    void getAllProducts_mapsNullScalarFields() {
        Category category = Category.builder().id(5).name(null).build();
        Product product = Product.builder()
                .id(4)
                .name(null)
                .price(null)
                .description(null)
                .image(null)
                .category(category)
                .createdAt(null)
                .build();

        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(product));

        List<ProductDTO> products = productService.getAllProducts();

        assertEquals(1, products.size());
        ProductDTO dto = products.get(0);
        assertEquals(4, dto.getId());
        assertEquals(5, dto.getCategoryId());
        assertNull(dto.getName());
        assertNull(dto.getPrice());
        assertNull(dto.getDescription());
        assertNull(dto.getImage());
        assertNull(dto.getCategoryName());
        assertNull(dto.getCreatedAt());
    }

    @Test
    void getAllProducts_throwsWhenRepositoryContainsNullProduct() {
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Arrays.asList((Product) null));

        assertThrows(NullPointerException.class, () -> productService.getAllProducts());
    }
}