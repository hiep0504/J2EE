package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.dto.rag.RagChatTurn;
import com.example.Backend_J2EE.dto.rag.RagProductSuggestion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LlmAnswerService {

    private static final String SYSTEM_PROMPT = """
            Ban la chatbot tu van do bong da.
            Du lieu duoc cung cap ben duoi la du lieu duy nhat duoc phep dung.
            Khong duoc tu tao thong tin ngoai du lieu.
            Goi y toi da 3 san pham.
            Tra loi ngan gon, ro rang, de hieu.
            """;

    private final RestClient restClient;
    private final String provider;
    private final String ragApiKey;
    private final String ragBaseUrl;
    private final String model;

    public LlmAnswerService(
            @Value("${app.rag.provider:openai}") String provider,
            @Value("${app.rag.api-key:}") String ragApiKey,
            @Value("${app.rag.base-url:https://api.openai.com/v1}") String ragBaseUrl,
            @Value("${app.rag.chat.model:gpt-4o-mini}") String model
    ) {
        this.provider = provider;
        this.ragApiKey = ragApiKey;
        this.ragBaseUrl = ragBaseUrl;
        this.model = model;
        this.restClient = RestClient.builder().build();
    }

    public String generateAnswer(
            String question,
            String productsContext,
            List<RagProductSuggestion> products,
            List<RagChatTurn> history,
            String focusProductName
    ) {
        if ("openai".equalsIgnoreCase(provider) && StringUtils.hasText(ragApiKey)) {
            try {
                return callOpenAi(question, productsContext, history, focusProductName);
            } catch (RuntimeException ex) {
                return buildFallbackAnswer(question, products, focusProductName);
            }
        }

        if ("gemini".equalsIgnoreCase(provider) && StringUtils.hasText(ragApiKey)) {
            try {
                return callGemini(question, productsContext, history, focusProductName);
            } catch (RuntimeException ex) {
                return buildFallbackAnswer(question, products, focusProductName);
            }
        }

        return buildFallbackAnswer(question, products, focusProductName);
    }

    private String callOpenAi(String question, String productsContext, List<RagChatTurn> history, String focusProductName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.2);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "system", "content", "Ngu canh san pham hien tai:\n" + productsContext));
        if (StringUtils.hasText(focusProductName)) {
            messages.add(Map.of("role", "system", "content", "San pham dang duoc hoi den: " + focusProductName.trim()));
        }
        if (history != null) {
            for (RagChatTurn turn : history) {
                messages.add(Map.of("role", turn.getRole(), "content", turn.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", question));
        payload.put("messages", messages);

        Map<?, ?> response = restClient.post()
            .uri(ragBaseUrl + "/chat/completions")
            .header("Authorization", "Bearer " + ragApiKey)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .body(Map.class);

        String answer = extractAnswer(response).trim();
        if (!StringUtils.hasText(answer)) {
            throw new IllegalStateException("LLM response empty");
        }
        return answer;
    }

    private String callGemini(String question, String productsContext, List<RagChatTurn> history, String focusProductName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))));
        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "Ngu canh san pham hien tai:\n" + productsContext))
        ));
        if (StringUtils.hasText(focusProductName)) {
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", "San pham dang duoc hoi den: " + focusProductName.trim()))
            ));
        }
        if (history != null) {
            for (RagChatTurn turn : history) {
                String role = "assistant".equals(turn.getRole()) ? "model" : "user";
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", turn.getContent()))));
            }
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", question))));
        payload.put("contents", contents);
        payload.put("generationConfig", Map.of("temperature", 0.2));

        Map<?, ?> response = restClient.post()
                .uri(ragBaseUrl + "/models/" + model + ":generateContent?key=" + ragApiKey)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .body(Map.class);

        String answer = extractGeminiAnswer(response).trim();
        if (!StringUtils.hasText(answer)) {
            throw new IllegalStateException("Gemini response empty");
        }
        return answer;
    }

    private String extractAnswer(Map<?, ?> response) {
        Object choicesObj = response == null ? null : response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object choice0 = choices.get(0);
        if (!(choice0 instanceof Map<?, ?> choiceMap)) {
            return "";
        }
        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return "";
        }
        Object content = messageMap.get("content");
        return content == null ? "" : content.toString();
    }

    private String extractGeminiAnswer(Map<?, ?> response) {
        Object candidatesObj = response == null ? null : response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return "";
        }

        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidateMap)) {
            return "";
        }

        Object contentObj = candidateMap.get("content");
        if (!(contentObj instanceof Map<?, ?> contentMap)) {
            return "";
        }

        Object partsObj = contentMap.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof Map<?, ?> partMap) {
                Object partText = partMap.get("text");
                if (partText != null) {
                    text.append(partText);
                }
            }
        }
        return text.toString();
    }

    private String buildFallbackAnswer(String question, List<RagProductSuggestion> products, String focusProductName) {
        if (products == null || products.isEmpty()) {
            return "Minh chua tim thay san pham phu hop tu du lieu hien tai. Ban thu noi ro hon ve gia, loai san, size hoac thuong hieu nhe.";
        }

        String normalizedQuestion = question == null ? "" : question.toLowerCase(Locale.ROOT);
        String normalizedFocus = focusProductName == null ? "" : focusProductName.trim();

        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(normalizedFocus)) {
            sb.append("Theo ngu canh hien tai (san pham dang quan tam: ")
                    .append(normalizedFocus)
                    .append("):\n");
        } else {
            sb.append("Minh goi y mot vai lua chon phu hop:\n");
        }

        int limit = Math.min(3, products.size());
        for (int i = 0; i < limit; i++) {
            RagProductSuggestion item = products.get(i);
            sb.append(i + 1)
                    .append(". ")
                    .append(item.name() == null ? "San pham" : item.name());

            if (item.price() != null) {
                sb.append(" - ").append(item.price().toPlainString()).append(" VND");
            }

            if (item.category() != null && !item.category().isBlank()) {
                sb.append(" - ").append(item.category());
            }

            if (item.sizes() != null && !item.sizes().isEmpty()) {
                sb.append(" - Size: ").append(String.join(", ", item.sizes()));
            }

            if (item.averageRating() != null && item.averageRating() > 0) {
                sb.append(" - Rating: ").append(String.format(Locale.ROOT, "%.1f", item.averageRating()));
            }

            sb.append("\n");
        }

        if (!normalizedQuestion.isBlank()) {
            sb.append("\nBan co the hoi tiep de loc ky hon, vi du: duoi 2 trieu, size 42, dung cho san co nhan tao.");
        }
        return sb.toString().trim();
    }

}
