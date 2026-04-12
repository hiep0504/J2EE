package com.example.Backend_J2EE.dto.rag;

import java.math.BigDecimal;
import java.util.List;

public record RagProductSuggestion(
        Integer id,
        String name,
        BigDecimal price,
        String category,
        String image,
        String description,
        List<String> sizes,
        Double averageRating
) {
}
