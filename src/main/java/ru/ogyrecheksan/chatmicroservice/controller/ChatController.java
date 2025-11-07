package ru.ogyrecheksan.chatmicroservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.ogyrecheksan.chatmicroservice.dto.Request.CreateChatRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.service.ChatService;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;


import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getUserChats(
            @AuthenticationPrincipal String userEmail,
            @RequestHeader("Authorization") String authToken) {

        Long userId = extractUserIdFromEmail(userEmail); // Нужно реализовать
        List<ChatResponse> chats = chatService.getUserChats(userId, authToken);
        return ResponseEntity.ok(chats);
    }

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(
            @Valid @RequestBody CreateChatRequest request,
            @AuthenticationPrincipal String userEmail,
            @RequestHeader("Authorization") String authToken) {

        Long userId = extractUserIdFromEmail(userEmail);
        ChatResponse chat = chatService.createGroupChat(request, userId, authToken);
        return ResponseEntity.ok(chat);
    }

    @PostMapping("/personal/{userId}")
    public ResponseEntity<ChatResponse> createPersonalChat(
            @PathVariable Long userId,
            @AuthenticationPrincipal String userEmail,
            @RequestHeader("Authorization") String authToken) {

        Long currentUserId = extractUserIdFromEmail(userEmail);
        ChatResponse chat = chatService.createPersonalChat(currentUserId, userId, authToken);
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal String userEmail,
            @RequestHeader("Authorization") String authToken) {

        Long userId = extractUserIdFromEmail(userEmail);
        ChatResponse chat = chatService.getChat(chatId, userId, authToken);
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<MessageResponse>> getChatMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal String userEmail,
            @RequestHeader("Authorization") String authToken) {

        Long userId = extractUserIdFromEmail(userEmail);
        Page<MessageResponse> messages = messageService.getChatMessages(chatId, userId, page, size, authToken);
        return ResponseEntity.ok(messages);
    }

    // Временная реализация - в продакшене нужно получать ID из auth-сервиса
    private Long extractUserIdFromEmail(String email) {
        // Здесь должна быть логика получения ID пользователя по email
        // Пока возвращаем хардкод для тестирования
        return 1L;
    }
}
