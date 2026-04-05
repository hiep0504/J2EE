package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.chat.ChatMessageResponse;
import com.example.Backend_J2EE.dto.chat.ChatQueueEvent;
import com.example.Backend_J2EE.dto.chat.ConversationDetailsResponse;
import com.example.Backend_J2EE.dto.chat.ConversationSummaryResponse;
import com.example.Backend_J2EE.dto.chat.UserChatHistoryResponse;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.ChatMessage;
import com.example.Backend_J2EE.entity.Conversation;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.repository.ChatMessageRepository;
import com.example.Backend_J2EE.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AccountRepository accountRepository;
    private final ChatPresenceService chatPresenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final long timeoutSeconds;
    private final long offlineUnassignSeconds;
    private final Set<String> superAdminUsernames;

    public ChatService(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            AccountRepository accountRepository,
            ChatPresenceService chatPresenceService,
            SimpMessagingTemplate messagingTemplate,
            @Value("${app.chat.timeout-seconds:180}") long timeoutSeconds,
                @Value("${app.chat.offline-unassign-seconds:20}") long offlineUnassignSeconds,
            @Value("${app.chat.super-admin-usernames:}") String superAdminUsernames
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.accountRepository = accountRepository;
        this.chatPresenceService = chatPresenceService;
        this.messagingTemplate = messagingTemplate;
        this.timeoutSeconds = timeoutSeconds;
        this.offlineUnassignSeconds = offlineUnassignSeconds;
        this.superAdminUsernames = Arrays.stream(superAdminUsernames.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Transactional
    public ConversationDetailsResponse startConversation(Integer userId, String content) {
        Account user = requireAccount(userId);
        ensureUserRole(user);

        Conversation conversation = conversationRepository
                .findTopByUser_IdAndStatusInOrderByLastMessageAtDesc(
                        userId,
                        List.of(Conversation.Status.OPEN, Conversation.Status.ASSIGNED)
                )
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .user(user)
                        .status(Conversation.Status.OPEN)
                        .build()));

        if (StringUtils.hasText(content)) {
            appendMessage(conversation, user, ChatMessage.SenderType.USER, content);
        }

        broadcastQueueEvent("CONVERSATION_OPENED", conversation);
        return getConversationDetailsForUser(userId, conversation.getId());
    }

    @Transactional(readOnly = true)
    public ConversationDetailsResponse getMyActiveConversation(Integer userId) {
        requireAccount(userId);

        Conversation conversation = conversationRepository
                .findTopByUser_IdAndStatusInOrderByLastMessageAtDesc(
                        userId,
                        List.of(Conversation.Status.OPEN, Conversation.Status.ASSIGNED)
                )
            .or(() -> conversationRepository.findTopByUser_IdOrderByLastMessageAtDesc(userId))
                .orElse(null);

        if (conversation == null) {
            return new ConversationDetailsResponse(null, List.of());
        }

        List<ChatMessageResponse> messages = chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(this::toMessageResponse)
                .toList();

        return new ConversationDetailsResponse(toConversationSummary(conversation), messages);
    }

    @Transactional(readOnly = true)
    public UserChatHistoryResponse getMyConversationHistory(Integer userId) {
        requireAccount(userId);

        List<ConversationDetailsResponse> conversations = conversationRepository.findByUser_IdOrderByLastMessageAtDesc(userId)
                .stream()
                .map(conversation -> {
                    List<ChatMessageResponse> messages = chatMessageRepository
                            .findByConversation_IdOrderByCreatedAtAsc(conversation.getId())
                            .stream()
                            .map(this::toMessageResponse)
                            .toList();
                    return new ConversationDetailsResponse(toConversationSummary(conversation), messages);
                })
                .toList();

        return new UserChatHistoryResponse(conversations);
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getOpenConversations() {
        return conversationRepository.findByStatusOrderByLastMessageAtDesc(Conversation.Status.OPEN)
                .stream()
                .map(this::toConversationSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getAssignedConversations() {
        return conversationRepository.findByStatusOrderByLastMessageAtDesc(Conversation.Status.ASSIGNED)
                .stream()
                .map(this::toConversationSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getMyAssignedConversations(Integer adminId) {
        return conversationRepository.findByAdmin_IdAndStatusOrderByLastMessageAtDesc(adminId, Conversation.Status.ASSIGNED)
                .stream()
                .map(this::toConversationSummary)
                .toList();
    }

    @Transactional
    public ConversationSummaryResponse pickConversation(Integer adminId, Integer conversationId) {
        Account admin = requireAccount(adminId);
        ensureAdminRole(admin);

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay conversation"));

        if (conversation.getStatus() == Conversation.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conversation da dong");
        }

        if (conversation.getAdmin() != null && !conversation.getAdmin().getId().equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conversation da duoc admin khac pick");
        }

        conversation.setAdmin(admin);
        conversation.setStatus(Conversation.Status.ASSIGNED);
        conversation.setAssignedAt(LocalDateTime.now());
        Conversation saved = conversationRepository.save(conversation);

        broadcastQueueEvent("CONVERSATION_ASSIGNED", saved);
        return toConversationSummary(saved);
    }

    @Transactional
    public ConversationSummaryResponse takeoverConversation(Integer adminId, Integer conversationId) {
        Account admin = requireAccount(adminId);
        ensureAdminRole(admin);

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay conversation"));

        if (conversation.getStatus() == Conversation.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conversation da dong");
        }

        Account current = conversation.getAdmin();
        if (current != null && current.getId().equals(adminId)) {
            return toConversationSummary(conversation);
        }

        if (current != null) {
            boolean currentOnline = chatPresenceService.isAdminOnline(current.getId());
            if (currentOnline && !isSuperAdmin(admin)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin hien tai dang online, khong the takeover");
            }
        }

        conversation.setAdmin(admin);
        conversation.setStatus(Conversation.Status.ASSIGNED);
        conversation.setAssignedAt(LocalDateTime.now());
        Conversation saved = conversationRepository.save(conversation);

        broadcastQueueEvent("CONVERSATION_TAKEN_OVER", saved);
        return toConversationSummary(saved);
    }

    @Transactional
    public ConversationSummaryResponse closeConversation(Integer adminId, Integer conversationId) {
        Account admin = requireAccount(adminId);
        ensureAdminRole(admin);

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay conversation"));

        if (conversation.getAdmin() == null || !conversation.getAdmin().getId().equals(adminId)) {
            if (!isSuperAdmin(admin)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chi admin dang xu ly moi co quyen dong conversation");
            }
        }

        conversation.setStatus(Conversation.Status.CLOSED);
        Conversation saved = conversationRepository.save(conversation);
        broadcastQueueEvent("CONVERSATION_CLOSED", saved);
        return toConversationSummary(saved);
    }

    @Transactional
    public ChatMessageResponse sendMessageByUser(Integer userId, Integer conversationId, String content) {
        Account user = requireAccount(userId);
        ensureUserRole(user);

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay conversation"));

        if (!conversation.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen gui vao conversation nay");
        }
        if (conversation.getStatus() == Conversation.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conversation da dong");
        }

        ChatMessage savedMessage = appendMessage(conversation, user, ChatMessage.SenderType.USER, content);
        broadcastMessage(savedMessage);
        broadcastQueueEvent("MESSAGE_FROM_USER", conversation);
        return toMessageResponse(savedMessage);
    }

    @Transactional
    public ChatMessageResponse sendMessageByAdmin(Integer adminId, Integer conversationId, String content) {
        Account admin = requireAccount(adminId);
        ensureAdminRole(admin);

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay conversation"));

        if (conversation.getStatus() != Conversation.Status.ASSIGNED || conversation.getAdmin() == null || !conversation.getAdmin().getId().equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chi admin duoc assign moi co quyen tra loi");
        }

        ChatMessage savedMessage = appendMessage(conversation, admin, ChatMessage.SenderType.ADMIN, content);
        conversation.setLastAdminReplyAt(savedMessage.getCreatedAt());
        conversationRepository.save(conversation);

        broadcastMessage(savedMessage);
        broadcastQueueEvent("MESSAGE_FROM_ADMIN", conversation);
        return toMessageResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public ConversationDetailsResponse getConversationDetailsForAdmin(Integer adminId, Integer conversationId) {
        Account admin = requireAccount(adminId);
        ensureAdminRole(admin);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay conversation"));

        if (conversation.getAdmin() != null && !conversation.getAdmin().getId().equals(adminId) && !isSuperAdmin(admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem conversation nay");
        }

        List<ChatMessageResponse> messages = chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();

        return new ConversationDetailsResponse(toConversationSummary(conversation), messages);
    }

    @Transactional(readOnly = true)
    public ConversationDetailsResponse getConversationDetailsForUser(Integer userId, Integer conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay conversation"));

        if (!conversation.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem conversation nay");
        }

        List<ChatMessageResponse> messages = chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();

        return new ConversationDetailsResponse(toConversationSummary(conversation), messages);
    }

    @Transactional
    public void unassignConversationsForOfflineAdmin(Integer adminId) {
        if (chatPresenceService.isAdminOnline(adminId)) {
            return;
        }

        List<Conversation> assigned = conversationRepository.findByAdmin_IdAndStatus(adminId, Conversation.Status.ASSIGNED);
        if (assigned.isEmpty()) {
            return;
        }

        for (Conversation conversation : assigned) {
            conversation.setAdmin(null);
            conversation.setStatus(Conversation.Status.OPEN);
            conversation.setAssignedAt(null);
        }
        conversationRepository.saveAll(assigned);

        for (Conversation conversation : assigned) {
            broadcastQueueEvent("CONVERSATION_UNASSIGNED_OFFLINE", conversation);
        }
    }

    @Scheduled(fixedDelayString = "${app.chat.offline-unassign-scan-ms:5000}")
    @Transactional
    public void releaseOfflineAdminConversations() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(offlineUnassignSeconds);
        List<Integer> offlineAdminIds = chatPresenceService.findAdminsOfflineBefore(cutoff);
        if (offlineAdminIds.isEmpty()) {
            return;
        }

        for (Integer adminId : offlineAdminIds) {
            unassignConversationsForOfflineAdmin(adminId);
            chatPresenceService.clearOfflineMarker(adminId);
        }
    }

    @Scheduled(fixedDelayString = "${app.chat.timeout-scan-ms:30000}")
    @Transactional
    public void releaseTimedOutConversations() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
        List<Conversation> timedOut = conversationRepository.findTimedOutConversations(
                Conversation.Status.ASSIGNED,
                Conversation.SenderType.USER,
                cutoff
        );

        if (timedOut.isEmpty()) {
            return;
        }

        for (Conversation conversation : timedOut) {
            conversation.setAdmin(null);
            conversation.setStatus(Conversation.Status.OPEN);
            conversation.setAssignedAt(null);
        }
        conversationRepository.saveAll(timedOut);

        for (Conversation conversation : timedOut) {
            broadcastQueueEvent("CONVERSATION_UNASSIGNED_TIMEOUT", conversation);
        }
    }

    private ChatMessage appendMessage(Conversation conversation, Account sender, ChatMessage.SenderType senderType, String content) {
        String safeContent = content == null ? "" : content.trim();
        if (!StringUtils.hasText(safeContent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Noi dung tin nhan la bat buoc");
        }

        ChatMessage savedMessage = chatMessageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .senderType(senderType)
                .content(safeContent)
                .build());

        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        conversation.setLastMessageSenderType(senderType == ChatMessage.SenderType.ADMIN
                ? Conversation.SenderType.ADMIN
                : Conversation.SenderType.USER);
        conversationRepository.save(conversation);
        return savedMessage;
    }

    private void broadcastMessage(ChatMessage message) {
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getId(),
                toMessageResponse(message)
        );
    }

    private void broadcastQueueEvent(String eventType, Conversation conversation) {
        ChatQueueEvent payload = new ChatQueueEvent(eventType, toConversationSummary(conversation));
        messagingTemplate.convertAndSend("/topic/admin/conversations", payload);
    }

    private ConversationSummaryResponse toConversationSummary(Conversation conversation) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getUser() != null ? conversation.getUser().getId() : null,
                conversation.getUser() != null ? conversation.getUser().getUsername() : null,
                conversation.getAdmin() != null ? conversation.getAdmin().getId() : null,
                conversation.getAdmin() != null ? conversation.getAdmin().getUsername() : null,
                conversation.getStatus(),
                conversation.getLastMessageAt(),
                conversation.getAssignedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getConversation() != null ? message.getConversation().getId() : null,
                message.getSender() != null ? message.getSender().getId() : null,
                message.getSenderType(),
                message.getSender() != null ? message.getSender().getUsername() : null,
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private Account requireAccount(Integer accountId) {
        if (accountId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
        }
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tai khoan khong ton tai"));
    }

    private void ensureAdminRole(Account account) {
        if (account.getRole() != Account.Role.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen admin");
        }
        if (Boolean.TRUE.equals(account.getLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan admin da bi khoa");
        }
    }

    private void ensureUserRole(Account account) {
        if (Boolean.TRUE.equals(account.getLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan da bi khoa");
        }
    }

    private boolean isSuperAdmin(Account account) {
        String username = account.getUsername();
        return username != null && superAdminUsernames.contains(username.toLowerCase());
    }
}
