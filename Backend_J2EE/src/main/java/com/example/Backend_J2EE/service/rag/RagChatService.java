package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.dto.rag.RagChatResponse;
import com.example.Backend_J2EE.dto.rag.RagChatTurn;
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
	private final int contextWindowTurns;
	private final int contextWindowMaxHistoryChars;

	public RagChatService(
			ProductRepository productRepository,
			ReviewRepository reviewRepository,
			RagVectorStoreService ragVectorStoreService,
			LlmAnswerService llmAnswerService,
			@Value("${app.rag.retrieval.top-k:5}") int topK,
			@Value("${app.rag.chat.context-window-turns:8}") int contextWindowTurns,
			@Value("${app.rag.chat.context-window-max-history-chars:6000}") int contextWindowMaxHistoryChars
	) {
		this.productRepository = productRepository;
		this.reviewRepository = reviewRepository;
		this.ragVectorStoreService = ragVectorStoreService;
		this.llmAnswerService = llmAnswerService;
		this.topK = topK;
		this.contextWindowTurns = Math.max(0, contextWindowTurns);
		this.contextWindowMaxHistoryChars = Math.max(0, contextWindowMaxHistoryChars);
	}

	@Transactional
	public int rebuildIndex() {
		List<RagProductDocument> docs = buildDocuments();
		ragVectorStoreService.upsertDocuments(docs);
		return docs.size();
	}

	@Transactional
	public RagChatResponse ask(String question, List<RagChatTurn> history, String focusProductName) {
		List<RagProductDocument> docs = buildDocuments();
		if (docs.isEmpty()) {
			return new RagChatResponse("He thong chua co du lieu san pham de tu van.", List.of());
		}

		if (!ragVectorStoreService.hasAnyVectorForDocuments(docs)) {
			return new RagChatResponse(
					"Chi muc vector chua san sang. Vui long rebuild index (POST /api/chat/rag/index/rebuild) hoac doi job nen chay.",
					List.of()
			);
		}

		String searchQuery = buildSearchQuery(question, history, focusProductName);
		List<RagVectorStoreService.RagProductHit> hits = ragVectorStoreService.search(searchQuery, docs, topK);
		if (hits.isEmpty()) {
			return new RagChatResponse(
					"Khong tim thay san pham phu hop trong chi muc vector hien tai. Ban co the rebuild index de dong bo du lieu moi.",
					List.of()
			);
		}

		List<RagProductSuggestion> suggestions = hits.stream()
				.limit(3)
				.map(hit -> toSuggestion(hit.document()))
				.toList();

		String context = buildContext(hits);
		List<RagChatTurn> normalizedHistory = normalizeHistory(history);
		String answer = llmAnswerService.generateAnswer(question, context, suggestions, normalizedHistory, normalizeFocusProductName(focusProductName));
		return new RagChatResponse(answer, suggestions);
	}

	private String buildSearchQuery(String question, List<RagChatTurn> history, String focusProductName) {
		StringBuilder query = new StringBuilder(question == null ? "" : question.trim());
		String normalizedFocus = normalizeFocusProductName(focusProductName);
		if (!normalizedFocus.isEmpty()) {
			query.append(' ').append(normalizedFocus);
		}

		if (history != null) {
			for (int i = history.size() - 1; i >= 0; i--) {
				RagChatTurn turn = history.get(i);
				if (turn == null || turn.getContent() == null) {
					continue;
				}
				String content = turn.getContent().trim();
				if (content.isEmpty()) {
					continue;
				}
				query.append(' ').append(content);
				break;
			}
		}

		return query.toString().trim();
	}

	private String normalizeFocusProductName(String focusProductName) {
		return focusProductName == null ? "" : focusProductName.trim();
	}

	private List<RagChatTurn> normalizeHistory(List<RagChatTurn> history) {
		if (history == null || history.isEmpty() || contextWindowTurns == 0) {
			return List.of();
		}

		List<RagChatTurn> cleaned = history.stream()
				.filter(item -> item != null && item.getRole() != null && item.getContent() != null)
				.map(item -> {
					RagChatTurn turn = new RagChatTurn();
					String role = item.getRole().trim().toLowerCase(Locale.ROOT);
					if (!role.equals("user") && !role.equals("assistant")) {
						return null;
					}
					String content = item.getContent().trim();
					if (content.isEmpty()) {
						return null;
					}
					turn.setRole(role);
					turn.setContent(content);
					return turn;
				})
				.filter(Objects::nonNull)
				.toList();

		if (cleaned.size() <= contextWindowTurns) {
			return trimHistoryByCharBudget(cleaned);
		}

		return trimHistoryByCharBudget(cleaned.subList(cleaned.size() - contextWindowTurns, cleaned.size()));
	}

	private List<RagChatTurn> trimHistoryByCharBudget(List<RagChatTurn> history) {
		if (history.isEmpty() || contextWindowMaxHistoryChars == 0) {
			return List.of();
		}

		int usedChars = 0;
		List<RagChatTurn> trimmed = new ArrayList<>();
		for (int i = history.size() - 1; i >= 0; i--) {
			RagChatTurn turn = history.get(i);
			int turnChars = turn.getRole().length() + turn.getContent().length() + 16;
			if (!trimmed.isEmpty() && usedChars + turnChars > contextWindowMaxHistoryChars) {
				break;
			}
			usedChars += turnChars;
			trimmed.add(0, turn);
		}
		return trimmed;
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

