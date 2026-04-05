package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.chat.ChatMessageResponse;
import com.example.Backend_J2EE.dto.chat.ConversationDetailsResponse;
import com.example.Backend_J2EE.dto.chat.ConversationSummaryResponse;
import com.example.Backend_J2EE.dto.chat.SendMessageRequest;
import com.example.Backend_J2EE.dto.chat.StartConversationRequest;
import com.example.Backend_J2EE.dto.chat.UserChatHistoryResponse;
import com.example.Backend_J2EE.service.AdminAuthorizationService;
import com.example.Backend_J2EE.service.AuthService;
import com.example.Backend_J2EE.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ChatController {

    private final ChatService chatService;
    private final AdminAuthorizationService adminAuthorizationService;

    public ChatController(ChatService chatService, AdminAuthorizationService adminAuthorizationService) {
        this.chatService = chatService;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @PostMapping("/conversations/start")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDetailsResponse startConversation(@RequestBody(required = false) StartConversationRequest request, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return chatService.startConversation(accountId, request != null ? request.getContent() : null);
    }

    @GetMapping("/conversations/my-active")
    public ConversationDetailsResponse getMyActiveConversation(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return chatService.getMyActiveConversation(accountId);
    }

    @GetMapping("/conversations/my-history")
    public UserChatHistoryResponse getMyConversationHistory(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return chatService.getMyConversationHistory(accountId);
    }

    @GetMapping("/conversations/{id}")
    public ConversationDetailsResponse getConversationForUser(@PathVariable Integer id, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return chatService.getConversationDetailsForUser(accountId, id);
    }

    @PostMapping("/conversations/{id}/messages")
    public ChatMessageResponse sendMessageAsUser(@PathVariable Integer id, @RequestBody SendMessageRequest request, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return chatService.sendMessageByUser(accountId, id, request != null ? request.getContent() : null);
    }

    @GetMapping("/admin/conversations/open")
    public List<ConversationSummaryResponse> getOpenConversations(HttpSession session) {
        adminAuthorizationService.requireAdmin(session);
        return chatService.getOpenConversations();
    }

    @GetMapping("/admin/conversations/assigned")
    public List<ConversationSummaryResponse> getAssignedConversations(HttpSession session) {
        adminAuthorizationService.requireAdmin(session);
        return chatService.getAssignedConversations();
    }

    @GetMapping("/admin/conversations/mine")
    public List<ConversationSummaryResponse> getMyAssignedConversations(HttpSession session) {
        Integer accountId = adminAuthorizationService.requireAdmin(session).getId();
        return chatService.getMyAssignedConversations(accountId);
    }

    @GetMapping("/admin/conversations/{id}")
    public ConversationDetailsResponse getConversationForAdmin(@PathVariable Integer id, HttpSession session) {
        Integer accountId = adminAuthorizationService.requireAdmin(session).getId();
        return chatService.getConversationDetailsForAdmin(accountId, id);
    }

    @PostMapping("/admin/conversations/{id}/pick")
    public ConversationSummaryResponse pickConversation(@PathVariable Integer id, HttpSession session) {
        Integer accountId = adminAuthorizationService.requireAdmin(session).getId();
        return chatService.pickConversation(accountId, id);
    }

    @PostMapping("/admin/conversations/{id}/takeover")
    public ConversationSummaryResponse takeoverConversation(@PathVariable Integer id, HttpSession session) {
        Integer accountId = adminAuthorizationService.requireAdmin(session).getId();
        return chatService.takeoverConversation(accountId, id);
    }

    @PostMapping("/admin/conversations/{id}/messages")
    public ChatMessageResponse sendMessageAsAdmin(@PathVariable Integer id, @RequestBody SendMessageRequest request, HttpSession session) {
        Integer accountId = adminAuthorizationService.requireAdmin(session).getId();
        return chatService.sendMessageByAdmin(accountId, id, request != null ? request.getContent() : null);
    }

    @PostMapping("/admin/conversations/{id}/close")
    public ConversationSummaryResponse closeConversation(@PathVariable Integer id, HttpSession session) {
        Integer accountId = adminAuthorizationService.requireAdmin(session).getId();
        return chatService.closeConversation(accountId, id);
    }
}
