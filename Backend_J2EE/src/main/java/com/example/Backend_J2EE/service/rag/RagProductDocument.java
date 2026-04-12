package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.entity.Product;

import java.util.List;

public record RagProductDocument(
        Product product,
        String document,
        List<String> sizes,
        double averageRating
) {
}
