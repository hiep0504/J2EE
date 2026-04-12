package com.example.Backend_J2EE.service.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final RestClient restClient;
    private final String provider;
    private final String ragApiKey;
    private final String ragBaseUrl;
    private final String embeddingModel;
    private final int fallbackDimensions;

    public EmbeddingService(
            @Value("${app.rag.provider:openai}") String provider,
            @Value("${app.rag.api-key:}") String ragApiKey,
            @Value("${app.rag.base-url:https://api.openai.com/v1}") String ragBaseUrl,
            @Value("${app.rag.embedding.model:text-embedding-3-small}") String embeddingModel,
            @Value("${app.rag.embedding.fallback-dimensions:256}") int fallbackDimensions
    ) {
        this.provider = provider;
        this.ragApiKey = ragApiKey;
        this.ragBaseUrl = ragBaseUrl;
        this.embeddingModel = embeddingModel;
        this.fallbackDimensions = fallbackDimensions;
        this.restClient = RestClient.builder().build();
    }

    public float[] embed(String text) {
        if ("openai".equalsIgnoreCase(provider) && StringUtils.hasText(ragApiKey)) {
            return embedWithOpenAi(text);
        }

        if ("gemini".equalsIgnoreCase(provider) && StringUtils.hasText(ragApiKey)) {
            return embedWithGemini(text);
        }

        if (!StringUtils.hasText(ragApiKey)) {
            throw new IllegalStateException("RAG API key is missing. Set APP_RAG_API_KEY in backend .env");
        }

        throw new IllegalStateException("Unsupported RAG provider: " + provider);
    }

    private float[] embedWithOpenAi(String text) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", embeddingModel);
        payload.put("input", text);

        Map<?, ?> response = restClient.post()
        .uri(ragBaseUrl + "/embeddings")
        .header("Authorization", "Bearer " + ragApiKey)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .body(Map.class);

        List<Double> embedding = extractEmbedding(response);
        if (embedding.isEmpty()) {
            throw new IllegalStateException("Embedding response is empty");
        }

        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i).floatValue();
        }
        return normalize(vector);
    }

    private float[] embedWithGemini(String text) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", Map.of("parts", List.of(Map.of("text", text))));

        Map<?, ?> response = restClient.post()
                .uri(ragBaseUrl + "/models/" + embeddingModel + ":embedContent?key=" + ragApiKey)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .body(Map.class);

        List<Double> embedding = extractGeminiEmbedding(response);
        if (embedding.isEmpty()) {
            throw new IllegalStateException("Gemini embedding response is empty");
        }

        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i).floatValue();
        }
        return normalize(vector);
    }

    private float[] normalize(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return vector;
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }

    private List<Double> extractEmbedding(Map<?, ?> response) {
        Object dataObj = response == null ? null : response.get("data");
        if (!(dataObj instanceof List<?> data) || data.isEmpty()) {
            return List.of();
        }
        Object first = data.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return List.of();
        }
        Object embeddingObj = firstMap.get("embedding");
        if (!(embeddingObj instanceof List<?> embeddingRaw)) {
            return List.of();
        }

        List<Double> embedding = new ArrayList<>();
        for (Object item : embeddingRaw) {
            if (item instanceof Number number) {
                embedding.add(number.doubleValue());
            }
        }
        return embedding;
    }

    private List<Double> extractGeminiEmbedding(Map<?, ?> response) {
        Object embeddingObj = response == null ? null : response.get("embedding");
        if (!(embeddingObj instanceof Map<?, ?> embeddingMap)) {
            return List.of();
        }

        Object valuesObj = embeddingMap.get("values");
        if (!(valuesObj instanceof List<?> valuesRaw)) {
            return List.of();
        }

        List<Double> embedding = new ArrayList<>();
        for (Object item : valuesRaw) {
            if (item instanceof Number number) {
                embedding.add(number.doubleValue());
            }
        }
        return embedding;
    }
}
