package ru.ogyrecheksan.chatmicroservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ru.ogyrecheksan.chatmicroservice.dto.Request.SendMessageRequest;
import ru.ogyrecheksan.chatmicroservice.service.MessageService;
import ru.ogyrecheksan.chatmicroservice.service.WebSocketService;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final MessageService messageService;
    private final WebSocketService webSocketService;

    @MessageMapping("/chat/{chatId}/sendMessage")
    public void sendMessage(
            @DestinationVariable Long chatId,
            @Payload SendMessageRequest request,
            Principal principal) {

        // Получаем UUID пользователя из principal
        UUID userId = extractUserIdFromPrincipal(principal);

        // Используем существующий метод сервиса
        request.setChatId(chatId);
        messageService.sendMessage(request, userId, null); // authToken = null для WebSocket
    }

    @MessageMapping("/chat/{chatId}/typing")
    public void handleTyping(
            @DestinationVariable Long chatId,
            @Payload Boolean typing,
            Principal principal) {

        UUID userId = extractUserIdFromPrincipal(principal);
        webSocketService.sendTypingIndicator(chatId, userId, typing);
    }

    private UUID extractUserIdFromPrincipal(Principal principal) {
        // Временная реализация - в продакшене нужно получать UUID из principal
        // Используем тот же метод, что и в ChatController для консистентности
        if (principal != null && principal.getName() != null) {
            return UUID.nameUUIDFromBytes(principal.getName().getBytes());
        }
        // Fallback для тестирования
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}