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

        // Здесь нужно получить ID пользователя из principal
        Long userId = 1L; // Временная реализация

        // Используем существующий метод сервиса
        request.setChatId(chatId);
        messageService.sendMessage(request, userId, null);
    }

    @MessageMapping("/chat/{chatId}/typing")
    public void handleTyping(
            @DestinationVariable Long chatId,
            @Payload Boolean typing,
            Principal principal) {

        Long userId = 1L; // Временная реализация
        webSocketService.sendTypingIndicator(chatId, userId, typing);
    }
}