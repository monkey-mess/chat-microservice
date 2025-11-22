package ru.ogyrecheksan.chatmicroservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ogyrecheksan.chatmicroservice.dto.Request.CreateChatRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.service.ChatService;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;

import java.util.List;
import java.util.UUID;

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
        ChatResponse chat = chatService.createGroupChat(request, userId, authToken);
        return ResponseEntity.ok(chat);
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
    public ResponseEntity<Page<MessageResponse>> getChatMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication,
            @RequestHeader("Authorization") String authToken) {

        UUID userId = extractUserIdFromAuthentication(authentication);
        Page<MessageResponse> messages = messageService.getChatMessages(chatId, userId, page, size, authToken);
        return ResponseEntity.ok(messages);
    }

    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }
        String username = authentication.getName();
        return UUID.nameUUIDFromBytes(username.getBytes());
    }
}