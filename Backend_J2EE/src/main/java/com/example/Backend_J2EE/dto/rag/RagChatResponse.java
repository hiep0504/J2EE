package com.example.Backend_J2EE.dto.rag;

import java.util.List;

public record RagChatResponse(
        String answer,
        List<RagProductSuggestion> products
) {
}
