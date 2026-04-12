package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.dto.rag.RagProductSuggestion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
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

    public String generateAnswer(String question, String productsContext, List<RagProductSuggestion> products) {
        if ("openai".equalsIgnoreCase(provider) && StringUtils.hasText(ragApiKey)) {
            return callOpenAi(question, productsContext);
        }

        if ("gemini".equalsIgnoreCase(provider) && StringUtils.hasText(ragApiKey)) {
            return callGemini(question, productsContext);
        }

        if (!StringUtils.hasText(ragApiKey)) {
            throw new IllegalStateException("RAG API key is missing. Set APP_RAG_API_KEY in backend .env");
        }

        throw new IllegalStateException("Unsupported RAG provider: " + provider);
    }

    private String callOpenAi(String question, String productsContext) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.2);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", "Du lieu:\n" + productsContext + "\n\nCau hoi:\n" + question)
        ));

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

    private String callGemini(String question, String productsContext) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))));
        payload.put("contents", List.of(
                Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", "Du lieu:\n" + productsContext + "\n\nCau hoi:\n" + question))
                )
        ));
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

}
