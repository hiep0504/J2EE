package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.rag.RagChatRequest;
import com.example.Backend_J2EE.dto.rag.RagChatResponse;
import com.example.Backend_J2EE.service.rag.RagChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat/rag")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class RagChatController {

	private final RagChatService ragChatService;

	public RagChatController(RagChatService ragChatService) {
		this.ragChatService = ragChatService;
	}

	@PostMapping("/ask")
	public ResponseEntity<?> ask(@Valid @RequestBody RagChatRequest request) {
		try {
			RagChatResponse response = ragChatService.ask(request.getQuestion());
			return ResponseEntity.ok(response);
		} catch (HttpClientErrorException.TooManyRequests ex) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.body(Map.of(
							"message", "Gemini dang het quota hoac vuot rate limit. Vui long doi hoac doi API key/project co quota.",
							"details", ex.getResponseBodyAsString()
					));
		} catch (HttpClientErrorException ex) {
			return ResponseEntity.status(ex.getStatusCode())
					.body(Map.of(
							"message", "Loi khi goi AI provider: " + ex.getStatusCode(),
							"details", ex.getResponseBodyAsString()
					));
		} catch (IllegalStateException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", ex.getMessage()));
		}
	}

	@PostMapping("/index/rebuild")
	public ResponseEntity<String> rebuildIndex() {
		int count = ragChatService.rebuildIndex();
		return ResponseEntity.ok("Indexed vectors: " + count);
	}
}

