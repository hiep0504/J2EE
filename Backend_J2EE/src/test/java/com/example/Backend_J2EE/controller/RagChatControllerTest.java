package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.rag.RagChatRequest;
import com.example.Backend_J2EE.dto.rag.RagChatResponse;
import com.example.Backend_J2EE.service.rag.RagChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagChatControllerTest {

    @Mock
    private RagChatService ragChatService;

    @InjectMocks
    private RagChatController ragChatController;

    @Test
    void askDelegatesToService() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("Hello");
        RagChatResponse response = new RagChatResponse("answer", List.of());
        when(ragChatService.ask("Hello", null, null)).thenReturn(response);

        var result = ragChatController.ask(request);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());
        verify(ragChatService).ask("Hello", null, null);
    }

    @Test
    void askForwardsHistoryAndFocusFields() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("Can giay nao tot?");
        request.setHistory(List.of());
        request.setFocusProductName("Shoe A");
        RagChatResponse response = new RagChatResponse("answer", List.of());
        when(ragChatService.ask("Can giay nao tot?", List.of(), "Shoe A")).thenReturn(response);

        var result = ragChatController.ask(request);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());
        verify(ragChatService).ask("Can giay nao tot?", List.of(), "Shoe A");
    }

    @Test
    void rebuildIndexDelegatesToService() {
        when(ragChatService.rebuildIndex()).thenReturn(3);

        var result = ragChatController.rebuildIndex();

        assertEquals(200, result.getStatusCode().value());
        assertEquals("Indexed vectors: 3", result.getBody());
        verify(ragChatService).rebuildIndex();
    }

    @Test
    void askMapsIllegalStateToBadRequest() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("Hello");
        when(ragChatService.ask("Hello", null, null)).thenThrow(new IllegalStateException("missing data"));

        var result = ragChatController.ask(request);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("missing data", ((java.util.Map<?, ?>) result.getBody()).get("message"));
    }

    @Test
    void askMapsTooManyRequestsToQuotaMessage() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("Hello");
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                "quota".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        when(ragChatService.ask("Hello", null, null)).thenThrow(exception);

        var result = ragChatController.ask(request);

        assertEquals(429, result.getStatusCode().value());
    }

    @Test
    void askMapsOtherHttpErrors() {
        RagChatRequest request = new RagChatRequest();
        request.setQuestion("Hello");
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_GATEWAY,
                "Bad Gateway",
                HttpHeaders.EMPTY,
                "bad".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        when(ragChatService.ask("Hello", null, null)).thenThrow(exception);

        var result = ragChatController.ask(request);

        assertEquals(502, result.getStatusCode().value());
    }
}
