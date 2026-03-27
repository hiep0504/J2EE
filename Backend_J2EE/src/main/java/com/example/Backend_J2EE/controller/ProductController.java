package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.ProductDTO;
import com.example.Backend_J2EE.dto.ProductSizeDTO;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.repository.ProductSizeRepository;
import com.example.Backend_J2EE.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ProductController {

    private final ProductService productService;
    private final ProductSizeRepository productSizeRepository;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}/sizes")
    public ResponseEntity<List<ProductSizeDTO>> getProductSizes(@PathVariable Integer productId) {
        List<ProductSize> productSizes = productSizeRepository.findByProduct_Id(productId);
        List<ProductSizeDTO> dtos = productSizes.stream()
                .map(ps -> new ProductSizeDTO(
                        ps.getId(),
                        ps.getSize().getId(),
                        ps.getSize().getSizeName(),
                        ps.getQuantity()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}

