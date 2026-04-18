package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.ProductDTO;
import com.example.Backend_J2EE.dto.ProductSizeDTO;
import com.example.Backend_J2EE.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @Test
    void getAllProducts_delegatesToService() {
        List<ProductDTO> products = List.of(new ProductDTO());
        when(productService.getAllProducts()).thenReturn(products);

        var response = productController.getAllProducts();

        assertSame(products, response.getBody());
        verify(productService).getAllProducts();
    }

    @Test
    void getProductSizes_delegatesToService() {
        ProductSizeDTO productSizeDTO = new ProductSizeDTO(9, 2, "42", 5);
        when(productService.getProductSizes(1)).thenReturn(List.of(productSizeDTO));

        var response = productController.getProductSizes(1);

        assertEquals(1, response.getBody().size());
        ProductSizeDTO dto = response.getBody().get(0);
        assertEquals(9, dto.getId());
        assertEquals(2, dto.getSizeId());
        assertEquals("42", dto.getSizeName());
        assertEquals(5, dto.getQuantity());
        verify(productService).getProductSizes(1);
    }

    @Test
    void emptyProductAndSizeListsAreReturnedAsIs() {
        when(productService.getAllProducts()).thenReturn(List.of());
        when(productService.getProductSizes(99)).thenReturn(List.of());

        assertTrue(productController.getAllProducts().getBody().isEmpty());
        assertTrue(productController.getProductSizes(99).getBody().isEmpty());
        verify(productService).getAllProducts();
        verify(productService).getProductSizes(99);
    }

    @Test
    void getAllProducts_returnsHttp200() {
        when(productService.getAllProducts()).thenReturn(List.of());

        var response = productController.getAllProducts();

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getProductSizes_allowsNullFieldsFromServiceDto() {
        ProductSizeDTO productSizeDTO = new ProductSizeDTO(9, 2, null, null);
        when(productService.getProductSizes(1)).thenReturn(List.of(productSizeDTO));

        var response = productController.getProductSizes(1);

        assertEquals(1, response.getBody().size());
        ProductSizeDTO dto = response.getBody().get(0);
        assertEquals(9, dto.getId());
        assertEquals(2, dto.getSizeId());
        assertEquals(null, dto.getSizeName());
        assertEquals(null, dto.getQuantity());
    }

    @Test
    void getProductSizes_propagatesServiceExceptions() {
        when(productService.getProductSizes(1)).thenThrow(new NullPointerException());

        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> productController.getProductSizes(1));
    }

    @Test
    void getProductSizes_passesNullProductIdToService() {
        when(productService.getProductSizes(null)).thenReturn(List.of());

        var response = productController.getProductSizes(null);

        assertTrue(response.getBody().isEmpty());
        verify(productService).getProductSizes(null);
    }
}
