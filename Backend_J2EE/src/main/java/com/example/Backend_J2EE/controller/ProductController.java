package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.product.ProductSummaryResponse;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.repository.ProductRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<ProductSummaryResponse> getAllProducts() {
        List<Product> products = productRepository.findAllByOrderByCreatedAtDesc();
        return products.stream()
                .map(product -> new ProductSummaryResponse(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getImage()
                ))
                .toList();
    }
}
