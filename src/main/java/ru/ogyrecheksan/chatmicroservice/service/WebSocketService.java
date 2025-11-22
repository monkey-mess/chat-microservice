package ru.ogyrecheksan.chatmicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.ogyrecheksan.chatmicroservice.dto.Response.MessageResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.WebSocketMessage;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessageToChat(Long chatId, MessageResponse message) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("MESSAGE");
        wsMessage.setPayload(message);
        wsMessage.setChatId(chatId);

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
    }

    public void sendTypingIndicator(Long chatId, UUID userId, Boolean typing) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("TYPING");

        // Создаем объект для payload
        TypingIndicator typingIndicator = new TypingIndicator();
        typingIndicator.setUserId(userId);
        typingIndicator.setTyping(typing);

        wsMessage.setPayload(typingIndicator);
        wsMessage.setChatId(chatId);

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
    }

    // Вспомогательный класс для индикатора набора текста
    public static class TypingIndicator {
        private UUID userId;
        private Boolean typing;

        // геттеры и сеттеры
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public Boolean getTyping() { return typing; }
        public void setTyping(Boolean typing) { this.typing = typing; }
    }
}