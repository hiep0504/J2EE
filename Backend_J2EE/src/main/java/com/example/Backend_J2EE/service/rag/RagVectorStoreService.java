package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.entity.RagProductVector;
import com.example.Backend_J2EE.repository.RagProductVectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class RagVectorStoreService {

    private final RagProductVectorRepository vectorRepository;
    private final EmbeddingService embeddingService;

    public RagVectorStoreService(RagProductVectorRepository vectorRepository, EmbeddingService embeddingService) {
        this.vectorRepository = vectorRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public void upsertDocuments(List<RagProductDocument> documents) {
        for (RagProductDocument document : documents) {
            Integer productId = document.product().getId();
            if (productId == null) {
                continue;
            }

            String hash = sha256(document.document());
            RagProductVector existing = vectorRepository.findByProductId(productId).orElse(null);
            if (existing != null && hash.equals(existing.getDocumentHash())) {
                continue;
            }

            float[] embedding = embeddingService.embed(document.document());
            String encoded = encode(embedding);

            RagProductVector vector = existing != null ? existing : RagProductVector.builder().productId(productId).build();
            vector.setDocumentHash(hash);
            vector.setEmbeddingData(encoded);
            vectorRepository.save(vector);
        }
    }

    @Transactional(readOnly = true)
    public List<RagProductHit> search(String question, List<RagProductDocument> documents, int topK) {
        if (documents.isEmpty()) {
            return List.of();
        }

        float[] queryVector = embeddingService.embed(question);
        Map<Integer, RagProductDocument> docByProductId = new HashMap<>();
        List<Integer> productIds = new ArrayList<>();
        for (RagProductDocument doc : documents) {
            if (doc.product().getId() != null) {
                Integer id = doc.product().getId();
                docByProductId.put(id, doc);
                productIds.add(id);
            }
        }

        List<RagProductVector> vectors = vectorRepository.findByProductIdIn(productIds);
        List<RagProductHit> hits = new ArrayList<>();
        for (RagProductVector vector : vectors) {
            RagProductDocument doc = docByProductId.get(vector.getProductId());
            if (doc == null) {
                continue;
            }
            float[] candidate = decode(vector.getEmbeddingData());
            double score = cosineSimilarity(queryVector, candidate);
            hits.add(new RagProductHit(doc, score));
        }

        return hits.stream()
                .sorted(Comparator.comparingDouble(RagProductHit::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    private double cosineSimilarity(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        if (length == 0) {
            return 0.0d;
        }

        double dot = 0.0d;
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
        }
        return dot;
    }

    private String encode(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    private float[] decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return new float[0];
        }
        String[] parts = encoded.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record RagProductHit(RagProductDocument document, double score) {
    }
}
