package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.dto.rag.RagChatTurn;
import com.example.Backend_J2EE.dto.rag.RagProductSuggestion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmAnswerServiceTest {

    @Test
    void generateAnswerFallsBackWhenNoApiKeyConfigured() {
        LlmAnswerService service = new LlmAnswerService("openai", "", "https://example.com", "gpt-4o-mini");

        RagProductSuggestion suggestion = new RagProductSuggestion(
            1,
            "Shoe A",
            new BigDecimal("199000"),
            "Shoes",
            null,
            null,
            List.of("42", "43"),
            4.5
        );
        RagChatTurn turn = new RagChatTurn();
        turn.setRole("user");
        turn.setContent("hello");

        String answer = service.generateAnswer(
            "Can giay nao tot?",
            "1. Shoe A",
            List.of(suggestion),
            List.of(turn),
            "Shoe A"
        );

        assertTrue(answer.contains("Shoe A"));
        assertTrue(answer.contains("Ban co the hoi tiep"));
    }

    @Test
    void generateAnswerReturnsGenericMessageWhenNoProductsMatch() {
        LlmAnswerService service = new LlmAnswerService("gemini", "", "https://example.com", "gemini-1.5-flash");

        String answer = service.generateAnswer("Size nao phu hop?", "", List.of(), List.of(), null);

        assertTrue(answer.startsWith("Minh chua tim thay san pham phu hop"));
        assertFalse(answer.contains("Ban co the hoi tiep"));
    }

    @Test
    void generateAnswerFallsBackWhenProductsAreNull() {
        LlmAnswerService service = new LlmAnswerService("openai", "", "https://example.com", "gpt-4o-mini");

        String answer = service.generateAnswer(null, "ctx", null, null, null);

        assertTrue(answer.startsWith("Minh chua tim thay san pham phu hop tu du lieu hien tai"));
    }

    @Test
    void generateAnswerLimitsFallbackToThreeProductsAndIncludesFocus() {
        LlmAnswerService service = new LlmAnswerService("openai", "", "https://example.com", "gpt-4o-mini");

        List<RagProductSuggestion> products = List.of(
                new RagProductSuggestion(1, "A", new BigDecimal("100"), "Cat", null, null, List.of("40"), 4.0),
                new RagProductSuggestion(2, "B", new BigDecimal("200"), "Cat", null, null, List.of("41"), 4.5),
                new RagProductSuggestion(3, "C", new BigDecimal("300"), "Cat", null, null, List.of("42"), 4.8),
                new RagProductSuggestion(4, "D", new BigDecimal("400"), "Cat", null, null, List.of("43"), 5.0)
        );

        String answer = service.generateAnswer("Tim giay", "ctx", products, List.of(), "Shoe X");

        assertTrue(answer.contains("Theo ngu canh hien tai (san pham dang quan tam: Shoe X)"));
        assertTrue(answer.contains("1. A"));
        assertTrue(answer.contains("2. B"));
        assertTrue(answer.contains("3. C"));
        assertFalse(answer.contains("4. D"));
    }
}