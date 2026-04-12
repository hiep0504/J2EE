package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.dto.rag.RagChatResponse;
import com.example.Backend_J2EE.dto.rag.RagProductSuggestion;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.entity.Review;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class RagChatService {

	private final ProductRepository productRepository;
	private final ReviewRepository reviewRepository;
	private final RagVectorStoreService ragVectorStoreService;
	private final LlmAnswerService llmAnswerService;
	private final int topK;

	public RagChatService(
			ProductRepository productRepository,
			ReviewRepository reviewRepository,
			RagVectorStoreService ragVectorStoreService,
			LlmAnswerService llmAnswerService,
			@Value("${app.rag.retrieval.top-k:5}") int topK
	) {
		this.productRepository = productRepository;
		this.reviewRepository = reviewRepository;
		this.ragVectorStoreService = ragVectorStoreService;
		this.llmAnswerService = llmAnswerService;
		this.topK = topK;
	}

	@Transactional
	public int rebuildIndex() {
		List<RagProductDocument> docs = buildDocuments();
		ragVectorStoreService.upsertDocuments(docs);
		return docs.size();
	}

	@Transactional
	public RagChatResponse ask(String question) {
		List<RagProductDocument> docs = buildDocuments();
		if (docs.isEmpty()) {
			return new RagChatResponse("He thong chua co du lieu san pham de tu van.", List.of());
		}

		ragVectorStoreService.upsertDocuments(docs);
		List<RagVectorStoreService.RagProductHit> hits = ragVectorStoreService.search(question, docs, topK);

		List<RagProductSuggestion> suggestions = hits.stream()
				.limit(3)
				.map(hit -> toSuggestion(hit.document()))
				.toList();

		String context = buildContext(hits);
		String answer = llmAnswerService.generateAnswer(question, context, suggestions);
		return new RagChatResponse(answer, suggestions);
	}

	private List<RagProductDocument> buildDocuments() {
		List<Product> products = productRepository.findAllByOrderByCreatedAtDesc();
		if (products.isEmpty()) {
			return List.of();
		}

		Map<Integer, Double> avgRatings = buildAverageRatings();
		List<RagProductDocument> docs = new ArrayList<>();
		for (Product product : products) {
			double avg = avgRatings.getOrDefault(product.getId(), 0.0d);
			docs.add(toDocument(product, avg));
		}
		return docs;
	}

	private Map<Integer, Double> buildAverageRatings() {
		Map<Integer, List<Integer>> byProduct = new HashMap<>();
		for (Review review : reviewRepository.findAll()) {
			if (review.getProduct() == null || review.getProduct().getId() == null || review.getRating() == null) {
				continue;
			}
			byProduct.computeIfAbsent(review.getProduct().getId(), key -> new ArrayList<>()).add(review.getRating());
		}

		Map<Integer, Double> result = new HashMap<>();
		for (Map.Entry<Integer, List<Integer>> entry : byProduct.entrySet()) {
			double avg = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0d);
			result.put(entry.getKey(), avg);
		}
		return result;
	}

	private RagProductDocument toDocument(Product product, double avg) {
		String name = text(product.getName());
		String category = product.getCategory() != null ? text(product.getCategory().getName()) : "Khong ro";
		String description = text(product.getDescription());
		String price = product.getPrice() != null ? product.getPrice().toPlainString() + " VND" : "Khong ro";
		List<String> sizes = extractSizes(product);

		String doc = String.format(Locale.ROOT,
				"San pham: %s. Danh muc: %s. Gia: %s. Size: %s. Rating trung binh: %.1f. Mo ta: %s",
				name,
				category,
				price,
				sizes.isEmpty() ? "Khong co" : String.join(", ", sizes),
				avg,
				description
		);

		return new RagProductDocument(product, doc, sizes, avg);
	}

	private String buildContext(List<RagVectorStoreService.RagProductHit> hits) {
		StringBuilder sb = new StringBuilder();
		int i = 1;
		for (RagVectorStoreService.RagProductHit hit : hits) {
			Product p = hit.document().product();
			sb.append(i++)
					.append(". ")
					.append(text(p.getName()))
					.append(" | Gia: ")
					.append(p.getPrice() == null ? "Khong ro" : p.getPrice().toPlainString() + " VND")
					.append(" | Danh muc: ")
					.append(p.getCategory() == null ? "Khong ro" : text(p.getCategory().getName()))
					.append(" | Size: ")
					.append(hit.document().sizes().isEmpty() ? "Khong co" : String.join(", ", hit.document().sizes()))
					.append(" | Rating: ")
					.append(String.format(Locale.ROOT, "%.1f", hit.document().averageRating()))
					.append(" | Mo ta: ")
					.append(text(p.getDescription()))
					.append("\n");
		}
		return sb.toString();
	}

	private RagProductSuggestion toSuggestion(RagProductDocument doc) {
		Product p = doc.product();
		return new RagProductSuggestion(
				p.getId(),
				p.getName(),
				p.getPrice(),
				p.getCategory() == null ? null : p.getCategory().getName(),
				p.getImage(),
				p.getDescription(),
				doc.sizes(),
				doc.averageRating()
		);
	}

	private List<String> extractSizes(Product product) {
		if (product.getProductSizes() == null) {
			return List.of();
		}

		return product.getProductSizes().stream()
				.map(ProductSize::getSize)
				.filter(Objects::nonNull)
				.map(size -> size.getSizeName())
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(name -> !name.isEmpty())
				.distinct()
				.sorted(Comparator.naturalOrder())
				.toList();
	}

	private String text(String value) {
		return value == null || value.isBlank() ? "Khong co" : value;
	}
}

