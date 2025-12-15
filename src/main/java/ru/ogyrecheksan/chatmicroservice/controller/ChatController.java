package ru.ogyrecheksan.chatmicroservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.ogyrecheksan.chatmicroservice.dto.Request.CreateChatRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Request.ParticipantActionRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Request.SendMessageRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.service.ChatService;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getUserChats(
            Authentication authentication,
            @RequestHeader("Authorization") String authToken) {

        UUID userId = extractUserIdFromAuthentication(authentication);
        List<ChatResponse> chats = chatService.getUserChats(userId, authToken);
        return ResponseEntity.ok(chats);
    }

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(
            @Valid @RequestBody CreateChatRequest request,
            Authentication authentication,
            @RequestHeader("Authorization") String authToken) {

        UUID userId = extractUserIdFromAuthentication(authentication);
        ChatResponse chat;
        if (request.getType() == ChatType.PERSONAL && request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            chat = chatService.createPersonalChat(userId, request.getParticipantIds().get(0), authToken);
        } else {
            chat = chatService.createGroupChat(request, userId, authToken);
        }
        return ResponseEntity.status(201).body(chat);
    }

    @PostMapping("/personal/{userId}")
    public ResponseEntity<ChatResponse> createPersonalChat(
            @PathVariable UUID userId,
            Authentication authentication,
            @RequestHeader("Authorization") String authToken) {

        UUID currentUserId = extractUserIdFromAuthentication(authentication);
        ChatResponse chat = chatService.createPersonalChat(currentUserId, userId, authToken);
        return ResponseEntity.ok(chat);
    }

    /**
     * Получить ID личного чата с пользователем.
     *
     * GET /api/chats/personal/{userId}/id
     * Response:
     *  200 OK  -> { "chatId": 123 }
     *  404 NOT FOUND -> если личный чат еще не создан
     */
    @GetMapping("/personal/{userId}/id")
    public ResponseEntity<Map<String, Long>> getPrivateChatId(
            @PathVariable UUID userId,
            Authentication authentication) {

        UUID currentUserId = extractUserIdFromAuthentication(authentication);
        try {
            Long chatId = chatService.getPrivateChatId(currentUserId, userId);
            return ResponseEntity.ok(Map.of("chatId", chatId));
        } catch (ru.ogyrecheksan.chatmicroservice.exception.ChatNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChat(
            @PathVariable Long chatId,
            Authentication authentication,
            @RequestHeader("Authorization") String authToken) {

        UUID userId = extractUserIdFromAuthentication(authentication);
        try {
            ChatResponse chat = chatService.getChat(chatId, userId, authToken);
            return ResponseEntity.ok(chat);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Chat not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageResponse>> getChatMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication,
            @RequestHeader("Authorization") String authToken) {

        UUID userId = extractUserIdFromAuthentication(authentication);
        Page<MessageResponse> messages = messageService.getChatMessages(chatId, userId, offset, limit, authToken);
        return ResponseEntity.ok(messages.getContent());
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long chatId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication,
            @RequestHeader("Authorization") String authToken) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        request.setChatId(chatId);
        MessageResponse response = messageService.sendMessage(request, userId, authToken);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{chatId}/participants")
    public ResponseEntity<Void> updateParticipants(
            @PathVariable Long chatId,
            @Valid @RequestBody ParticipantActionRequest request,
            Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        chatService.updateParticipants(chatId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{chatId}/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable Long chatId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        String url = chatService.uploadAvatar(chatId, userId, file);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }
        return UUID.fromString(authentication.getName());
    }
}